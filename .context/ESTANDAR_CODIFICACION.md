# ESTÁNDAR DE CODIFICACIÓN JAVA - PROYECTO FEDER

Este documento define las convenciones oficiales de estilo, nomenclatura, comentarios y buenas prácticas para el desarrollo en **Java puro (Arquitectura en 3 Capas)** del sistema FEDER, basadas en las *Oracle Java Code Conventions* y *Google Java Style Guide* adaptadas al proyecto.

---

## 1. Estructura de Paquetes y Archivos

- **Codificación de archivos:** Todos los archivos `.java` deben guardarse en **UTF-8** sin BOM y con saltos de línea consistentes.
- **Un archivo por clase pública:** El nombre del archivo debe coincidir exactamente (respetando mayúsculas y minúsculas) con el nombre de la clase pública que contiene.
- **Organización en 3 Capas + Entidades:**
  - `uagrm.feder.Entity` (o en minúsculas según estándar estricto: `entity`): Clases POJO del dominio.
  - `uagrm.feder.Datos`: Clases de acceso a datos (`D...`) y conexión JDBC.
  - `uagrm.feder.Negocio`: Controladores de lógica de negocio (`N...`) y validaciones.
  - `uagrm.feder.Presentacion`: Sockets TCP (`ClientePOP3`, `ClienteSMTP`), `CommandParser` y `EmailApp`.
- **Importaciones (`import`):**
  - Prohibido usar comodines (`import java.sql.*;`). Importar siempre las clases explícitamente.
  - Eliminar imports no utilizados.
  - Orden sugerido: 1) paquetes del proyecto (`uagrm.feder.*`), 2) librerías de terceros, 3) paquetes estándar (`java.*`, `javax.*`).

---

## 2. Convenciones de Nombres (Naming Conventions)

| Elemento | Convención | Regla y Ejemplos |
| :--- | :--- | :--- |
| **Paquetes** | `lowercase` | Todo en minúsculas, sin guiones bajos (ej. `bo.uagrm.feder.datos`). *Nota: En este proyecto se respeta la carpeta existente `uagrm.feder.{Capa}`.* |
| **Clases e Interfaces** | `PascalCase` | Sustantivos claros y sin errores ortográficos (ej. `Usuario`, `SqlConnection`, `CommandParser`).<br>Prefijos de capa: `D` para Datos (`DUsuario`), `N` para Negocio (`NUsuario`). |
| **Métodos** | `camelCase` | Verbos en infinitivo o acción clara (ej. `registrarUsuario()`, `getByEmail()`, `enviarCorreo()`). |
| **Variables y Parámetros** | `camelCase` | Descriptivos, evitando abreviaturas crípticas (ej. `nombreCompleto`, `rolId`, `correoElectronico`). **Evitar `snake_case`** en atributos Java aunque la BD lo use. |
| **Constantes** | `UPPER_SNAKE_CASE` | `static final` en mayúsculas separadas por guion bajo (ej. `HEADERS`, `MAIL_HOST`, `INTERVALO_SONDEO_MS`). |

---

## 3. Formato, Indentación y Longitud de Líneas

### A. Indentación y Llaves (Estilo K&R)
- **Indentación:** **4 espacios** por nivel (no usar tabuladores duros `\t`).
- **Llaves `{ }`:**
  - La llave de apertura `{` va al final de la misma línea de la declaración, precedida de un espacio.
  - La llave de cierre `}` va en su propia línea, alineada con el inicio del bloque.
  - Usar siempre llaves en `if`, `else`, `for` y `while`, incluso si tienen una sola instrucción.

```java
// Correcto
if (usuario != null) {
    procesar(usuario);
} else {
    registrarError();
}

// Incorrecto
if (usuario != null) procesar(usuario);
```

### B. Longitud Máxima de Línea
- **Límite recomendado:** **100 a 120 caracteres** por línea como máximo.
- Si una firma de método, consulta SQL o concatenación supera los 120 caracteres, debe dividirse con indentación de continuación (8 espacios o alineada con los parámetros):

```java
// Correcto
public Usuario(int idUsuario, String nombreCompleto, String correoElectronico,
        String hashContrasena, String estado, Timestamp fechaRegistro, int rolId) {
    ...
}
```

### C. Orden de Miembros dentro de una Clase
1. Constantes (`public static final`, luego `private static final`).
2. Atributos de instancia (`private`).
3. Bloques estáticos (`static { ... }`).
4. Constructores (del más simple al más completo).
5. Métodos públicos (`public`).
6. Métodos privados auxiliares (`private`).
7. `toString()`, `equals()`, `hashCode()`.

---

## 4. Estándar de Comentarios y Documentación (Javadoc)

### A. Comentarios Javadoc (`/** ... */`)
Obligatorios en **toda clase pública** y en **todo método público** (excepto getters/setters triviales). Deben incluir:
- Descripción breve del propósito (qué hace y por qué, no cómo línea por línea).
- Etiquetas estándar en orden:
  - `@param nombre descripción` (para cada parámetro)
  - `@return descripción` (si no es `void`)
  - `@throws TipoExcepcion condición` (si lanza excepciones)

```java
/**
 * Registra un nuevo usuario en el sistema validando rol y unicidad de correo.
 *
 * @param params      Lista con [nombre, email, contrasena, rolId].
 * @param senderEmail Correo del usuario que solicita la operación.
 * @return Mensaje de éxito o error con código de estado.
 */
public String registrar(List<String> params, String senderEmail) {
    ...
}
```

### B. Comentarios en Línea (`//`)
- Usar `//` dentro de los métodos solo para explicar **el porqué** de una decisión no evidente, pasos críticos de un protocolo (POP3/SMTP) o reglas de negocio importantes.
- Dejar un espacio después de `//` (ej. `// Validar que el remitente no se auto-elimine`).
- Prohibido dejar código muerto comentado en el repositorio.

---

## 5. Buenas Prácticas de Código Limpio, JDBC y Redes

1. **Encapsulamiento estricto:** Todos los atributos de entidades deben ser `private` y accederse mediante getters/setters.
2. **Seguridad SQL (Capa Datos):**
   - Usar exclusivamente `PreparedStatement` con marcadores `?`. Nunca concatenar entradas de usuario en SQL.
   - Especificar el esquema (`feder.TABLA`) o configurar `currentSchema=feder`.
   - Operaciones que modifiquen más de una tabla (ej. operación + `AUDITORIA`) deben ejecutarse en una **transacción** (`setAutoCommit(false)`, `commit()`, `rollback()`).
3. **Gestión de Recursos (`AutoCloseable`):**
   - Preferir **`try-with-resources`** para `Connection`, `PreparedStatement`, `ResultSet`, `Socket`, `BufferedReader` y `DataOutputStream` para garantizar el cierre automático sin fugas de memoria.
4. **Manejo de Excepciones:**
   - Nunca dejar un bloque `catch` vacío (salvo cierre controlado y documentado).
   - No tragar excepciones silenciosamente; registrar contexto útil del error.
5. **Credenciales y Consola:**
   - Evitar caracteres especiales/emojis Unicode (`✔`, `✘`, `📥`) en `System.out.println` si la aplicación corre en terminales Windows con codificación CP850/CP1252; usar etiquetas ASCII claras (`[OK]`, `[ERROR]`, `[INFO]`).
   - Centralizar credenciales o leerlas desde variables de entorno / archivo `.properties` ignorado en `.gitignore`.
6. **Sin lógica de negocio en Presentación ni Datos:**
   - `Presentacion` solo parsea y transporta mensajes.
   - `Negocio` valida reglas, permisos (RBAC) y hashes.
   - `Datos` solo ejecuta SQL y mapea resultados.

