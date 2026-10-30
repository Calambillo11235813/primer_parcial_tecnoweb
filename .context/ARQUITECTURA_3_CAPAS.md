# ARQUITECTURA DE SOFTWARE EN 3 CAPAS - PRIMER PARCIAL (JAVA PURO)

## 1. Visión General del Patrón Arquitectónico
El sistema para el primer parcial implementa una arquitectura desacoplada de 3 niveles basada en sockets TCP y procesamiento asíncrono de correos electrónicos, sin interfaz gráfica ni web estándar:

```text
[Cliente / Usuario Remoto]
          │
          │ Envia comando por correo (Subject / Body)
          ▼
   [Servidor de Correo] (mail.tecnoweb.org.bo)
     ├─ Puerto 110 (POP3)
     └─ Puerto 25 (SMTP)
          ▲           │
          │           │ Socket TCP
          │           ▼
┌────────────────────────────────────────────────────────┐
│ CAPA DE PRESENTACIÓN / PROTOCOLOS                      │
│ - MailVerificationThread (sondeo POP3 cada 10-15s)    │
│ - SendEmailThread (despacho SMTP)                      │
│ - EmailApp (<<interface>> / Frontera)                  │
└──────────────────────────┬─────────────────────────────┘
                           │ delega texto procesado
                           ▼
┌────────────────────────────────────────────────────────┐
│ CAPA DE NEGOCIO                                        │
│ - CommandParser & TokenListener (Analizador sintáctico)│
│ - Controladores N... (NUsuario, NCampana, NAporte...)  │
│ - Reglas de validación, roles (Director, Secretaria...)│
└──────────────────────────┬─────────────────────────────┘
                           │ invoca transacciones SQL
                           ▼
┌────────────────────────────────────────────────────────┐
│ CAPA DE DATOS & DOMINIO                                │
│ - SqlConnection (Pool / Conexión JDBC PostgreSQL)      │
│ - Controladores D... (DUsuario, DCampana, DAporte...)  │
│ - Entidades del Dominio (Usuario, Campana, Aporte...)  │
└──────────────────────────┬─────────────────────────────┘
                           │ TCP/IP (Puerto 5432)
                           ▼
              [Base de Datos PostgreSQL]


## 2. Especificación Técnica por Capa

### A. Capa de Presentación / Protocolos (`bo.uagrm.feder.Presentacion`)

#### ClientePOP3

- Abre un socket TCP (`java.net.Socket`) contra el host `mail.tecnoweb.org.bo` en el puerto `110`.
- Autenticación con `USER` y `PASS`.
- Consulta periódica con `STAT` y descarga multilínea de mensajes mediante `RETR` hasta el terminador `\r\n.\r\n`.
- Extrae el remitente (`From`), asunto (`Subject`) y cuerpo del mensaje.

#### ClienteSMTP

- Abre un socket TCP contra el puerto `25`.
- Ejecuta la secuencia `HELO -> MAIL FROM -> RCPT TO -> DATA -> . -> QUIT`.
- Termina cada línea estrictamente con `\r\n`.
- Formatea la respuesta en texto plano estructurado o tablas HTML legibles.

#### EmailApp (Frontera)

- Sirve de punto de entrada unificado para orquestar la recepción de correos y la notificación de resultados o mensajes de error (`tableNotify()`, `handleError()`).

### B. Capa de Negocio (`bo.uagrm.feder.Negocio`)

#### CommandParser y TokenListener

- Funciona como un reconocedor determinista de comandos (no chatbot heurístico).
- Valida mayúsculas/minúsculas, número de parámetros y separadores (delimitador `;` o estructura `COMANDO [p1; p2]`).
- Dispone del comando universal `HELP` que retorna por correo el manual de comandos válidos.

#### Controladores de Negocio (`N...`)

- `NUsuario`, `NCampana`, `NAporte`, `NComunidad`, `NConsulta`, `NPublicacion`, `NInstitucional`, `NReporte`.
- Validan permisos por remitente (ej. solo el correo del Director puede crear usuarios o cerrar campañas).
- Realizan cálculos de lógica de negocio (porcentaje de recaudación acumulada, estados de campaña).

### C. Capa de Datos y Dominio (`bo.uagrm.feder.Datos` y `bo.uagrm.feder.Entity`)

#### SqlConnection

- Gestiona la conexión JDBC a la base de datos PostgreSQL mediante el driver oficial (`postgresql-42.x.x.jar`).

#### Controladores de Datos (`D...`)

- Contienen la constante de encabezados `HEADERS: String[]` para formatear respuestas tabuladas.
- Construyen sentencias `PreparedStatement` para `INSERT`, `UPDATE`, `DELETE` y `SELECT`.
- Mapean las filas de `ResultSet` a objetos de las entidades (`Entity`).

#### Auditoría

- Cada operación sensible dispara un registro en la tabla `AUDITORIA` asociando la fecha, la tabla modificada y el usuario responsable.