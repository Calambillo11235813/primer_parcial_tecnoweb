# ESTADO DEL PROYECTO: PRIMER PARCIAL TECNOLOGÍA WEB (FEDER)

**Última actualización:** 29 de octubre de 2026  
**Entorno:** Java puro (sin frameworks externos de correo) + PostgreSQL institucional  
**Meta de desarrollo:** Plan de 3 días

---

## 1. Resumen de lo Implementado y Validado

### A. Pruebas de Red y Protocolos (Telnet)
- **Servidor Institucional:** `mail.tecnoweb.org.bo`
- **Protocolo POP3 (Puerto 110):**
  - Conexión y autenticación exitosa con la cuenta `grupo16sc`.
  - Comandos comprobados: `USER`, `PASS`, `STAT`, `LIST`, `RETR`, `QUIT`.
- **Protocolo SMTP (Puerto 25):**
  - Conexión directa y comprobación de IP pública autorizada en la lista de retransmisión.
  - Comandos comprobados: `HELO tecnoweb.org.bo`, `MAIL FROM`, `RCPT TO`, `DATA`, `.` y `QUIT`.
  - Correo de prueba (`Subject: TEST_PARCIAL`, cuerpo: `HELP`) despachado por SMTP y verificado con éxito en la bandeja POP3.

### B. Base de Datos (PostgreSQL)
- **Instancia:** Servidor del docente (`db_grupo16sc` en el puerto 5432).
- **Esquema aislado:** `feder` (creado para separar las tablas del proyecto de los esquemas individuales de tareas).
- **Tablas desplegadas:**
  - *Seguridad:* `ROL`, `USUARIO`, `AUDITORIA`
  - *CMS y Comunidades:* `PAGINA_INSTITUCIONAL`, `RED_SOCIAL`, `COMUNIDAD`, `PROGRAMA_PRODUCTIVO`, `TESTIMONIO`, `NOTICIA`, `GALERIA_MULTIMEDIA`
  - *Crowdfunding y Contacto:* `CAMPANA`, `APORTE`, `CONSULTA`
- **Datos Semilla:** Roles cargados (`Director`, `Secretaria`, `Instructor`), usuario Director inicial y comunidades base de la Chiquitanía (San José, Roboré, Okinawa).

### C. Contexto y Arquitectura (.context/)
- `CASOS_DE_USO_REQUISITOS.md`: Mapeo de CU-01 al CU-08 con los 17 RF y actores.
- `ARQUITECTURA_3_CAPAS.md`: Definición de responsabilidades para `EmailApp`, capas `N...`, capas `D...` y entidades.
- `ESQUEMA_DB.sql`: Script DDL ejecutado con `currentSchema=feder`.
- `COMANDOS_SINTAXIS.md`: Diccionario y gramática formal de órdenes para el `CommandParser`.

---

## 2. Estado de Implementación por Casos de Uso

| Caso de Uso | Requisitos Funcionales Asociados | Estado | Componentes Desarrollados |
| :--- | :--- | :--- | :--- |
| **`CU-01: Gestionar Usuarios y Roles`** | **RF-14:** Autenticación de usuarios<br>**RF-15:** Roles y permisos | **IMPLEMENTADO** | • `Usuario.java`, `Rol.java`, `Auditoria.java`<br>• `SqlConnetion.java`<br>• `DUsuario.java` (CRUD transaccional + Auditoría)<br>• `NUsuario.java` (Cifrado SHA-256 + RBAC Director)<br>• Comandos: `INSUSR`, `MODUSR`, `DELUSR`, `LISUSR` |
| **`CU-02: Gestionar Información Institucional`** | **RF-03:** Información institucional<br>**RF-04:** Redes sociales | **PENDIENTE** | Tablas: `PAGINA_INSTITUCIONAL`, `RED_SOCIAL`<br>Por implementar: `DPagina`, `DRedSocial`, `NInstitucional` |
| **`CU-03: Gestionar Publicaciones y Medios`** | **RF-01:** Noticias y comunicados<br>**RF-02:** Galería multimedia<br>**RF-08:** Testimonios | **PENDIENTE** | Tablas: `NOTICIA`, `GALERIA_MULTIMEDIA`, `TESTIMONIO`<br>Por implementar: `DNoticia`, `DTestimonio`, `NPublicacion` |
| **`CU-04: Gestionar Comunidades y Programas`** | **RF-05:** Comunidades beneficiarias<br>**RF-06:** Programas de asistencia<br>**RF-07:** Búsqueda y filtrado | **PENDIENTE** | Tablas: `COMUNIDAD`, `PROGRAMA_PRODUCTIVO`<br>Por implementar: `DComunidad`, `DPrograma`, `NComunidad` |
| **`CU-05: Gestionar Campañas de Recaudación`** | **RF-09:** Campañas de financiamiento<br>**RF-10:** Seguimiento recaudación | **PENDIENTE** | Tabla: `CAMPANA`<br>Por implementar: `DCampana`, `NCampana` |
| **`CU-06: Gestionar Donaciones y Aportes`** | **RF-11:** Registro de aportes<br>**RF-12:** Confirmación de aporte<br>**RF-13:** Fichas de proyectos | **PENDIENTE** | Tabla: `APORTE`<br>Por implementar: `DAporte`, `NAporte` |
| **`CU-07: Gestionar Bandeja de Consultas`** | **RF-16:** Mensajes de contacto | **PENDIENTE** | Tabla: `CONSULTA`<br>Por implementar: `DConsulta`, `NConsulta` |
| **`CU-08: Generar Reportes y Estadísticas`** | **RF-17:** Reportes y estadísticas | **PENDIENTE** | Vistas y cálculos analíticos<br>Por implementar: `DReporte`, `NReporte` |

---

## 3. Plan de Desarrollo de 3 Días

```text
DÍA 1: Persistencia (Capa D...), Entidades y Lógica CU-01
  [x] Ejecución de DDL y Seed Data en PostgreSQL
  [x] Implementar SqlConnetion.java con schema=feder
  [x] Implementar Entidades CU-01 (Usuario, Rol, Auditoria)
  [x] Implementar Controlador de Datos DUsuario.java con auditoría transaccional
  [x] Implementar Controlador de Negocio NUsuario.java (Cifrado SHA-256 + RBAC Director)
  [ ] Implementar restantes Entidades (Campana, Aporte, Comunidad, Noticia, Consulta, etc.)
  [ ] Implementar restantes Controladores de Datos (DCampana, DAporte, DConsulta, DComunidad)

DÍA 2: Lógica de Negocio (Capa N...) y Parser de Comandos
  [x] Redactar COMANDOS_SINTAXIS.md (Diccionario y gramática de órdenes para CU-01)
  [ ] Implementar CommandParser.java y comando HELP
  [ ] Implementar Controladores de Negocio restantes (NCampana, NAporte, NConsulta, NComunidad)
  [ ] Validaciones de permisos por remitente (Director vs Secretaria vs Instructor)

DÍA 3: Capa de Presentación (Sockets POP3/SMTP) e Integración
  [ ] Implementar ClientePOP3.java (hilo MailVerificationThread con sondeo de 10-15s)
  [ ] Implementar ClienteSMTP.java (SendEmailThread para respuestas tabuladas)
  [ ] Orquestar en EmailApp.java y probar flujo extremo a extremo por correo
```
