# ESTADO DEL PROYECTO: PRIMER PARCIAL TECNOLOGÍA WEB (FEDER)

**Última actualización:** 14 de noviembre de 2026  
**Entorno:** Java puro (sin frameworks externos de correo) + PostgreSQL institucional / local  
**Meta de desarrollo:** Plan de 3 días

---

## 1. Resumen de lo Implementado y Validado

### A. Pruebas de Red y Protocolos (Telnet y Simulador RFC 5321)
- **Servidor Institucional:** `mail.tecnoweb.org.bo`
- **Protocolo POP3 (Puerto 110) y SMTP (Puerto 25):**
  - Soporte completo `UTF-8` y decodificación MIME RFC 2047 (Base64 y Quoted-Printable).
  - Descarga y borrado garantizado en dos fases (`QUIT` inmediato en POP3) y filtro anti-bucle para auto-respuestas `RE:`.
  - `SimuladorConsola.java`: Emulador local interactivo del puerto 25 (RFC 5321).

### B. Base de Datos (PostgreSQL)
- **Instancia Dual (`SqlConnetion.java`):** Interruptor `USAR_BD_LOCAL` para alternar entre PostgreSQL local (`127.0.0.1`) y el servidor remoto (`mail.tecnoweb.org.bo:5432`) bajo el esquema `feder`.

### C. Contexto y Arquitectura (`.context/`)
- `CASOS_DE_USO_REQUISITOS.md`: Mapeo de CU-01 al CU-08 con los 17 RF y actores.
- `ARQUITECTURA_3_CAPAS.md`: Definición de responsabilidades para `EmailApp`, capas `N...`, capas `D...` y entidades.
- `ESQUEMA_DB.sql`: Script DDL con `currentSchema=feder`.
- `COMANDOS_SINTAXIS.md`: Diccionario de órdenes para CU-01 y CU-02.
- `ESTANDAR_CODIFICACION.md`: Convenciones de código Java del proyecto.

---

## 2. Estado de Implementación por Casos de Uso

| Caso de Uso | Requisitos Funcionales Asociados | Estado | Componentes Desarrollados |
| :--- | :--- | :--- | :--- |
| **`CU-01: Gestionar Usuarios y Roles`** | **RF-14:** Autenticación de usuarios<br>**RF-15:** Roles y permisos | **IMPLEMENTADO (End-to-End)** | • `Usuario.java`, `Rol.java`, `Auditoria.java`<br>• `DUsuario.java`, `NUsuario.java`<br>• Comandos: `INSUSR`, `MODUSR`, `DELUSR`, `LISUSR` |
| **`CU-02: Gestionar Información Institucional`** | **RF-03:** Información institucional<br>**RF-04:** Redes sociales | **IMPLEMENTADO (End-to-End)** | • `PaginaInstitucional.java`, `RedSocial.java`<br>• `DPaginaInstitucional.java`, `DRedSocial.java` (CRUD transaccional + Auditoría)<br>• `NInstitucional.java` (RBAC Director + validación URL)<br>• Comandos: `INSPAG`, `MODPAG`, `DELPAG`, `LISPAG`, `INSRED`, `MODRED`, `DELRED`, `LISRED` |
| **`CU-03: Gestionar Publicaciones y Medios`** | **RF-01:** Noticias y comunicados<br>**RF-02:** Galería multimedia<br>**RF-08:** Testimonios | **PENDIENTE** | Tablas: `NOTICIA`, `GALERIA_MULTIMEDIA`, `TESTIMONIO`<br>Por implementar: `DNoticia`, `DTestimonio`, `NPublicacion` |
| **`CU-04: Gestionar Comunidades y Programas`** | **RF-05:** Comunidades beneficiarias<br>**RF-06:** Programas de asistencia<br>**RF-07:** Búsqueda y filtrado | **PENDIENTE** | Tablas: `COMUNIDAD`, `PROGRAMA_PRODUCTIVO`<br>Por implementar: `DComunidad`, `DPrograma`, `NComunidad` |
| **`CU-05: Gestionar Campañas de Recaudación`** | **RF-09:** Campañas de financiamiento<br>**RF-10:** Seguimiento recaudación | **PENDIENTE** | Tabla: `CAMPANA`<br>Por implementar: `DCampana`, `NCampana` |
| **`CU-06: Gestionar Donaciones y Aportes`** | **RF-11:** Registro de aportes<br>**RF-12:** Confirmación de aporte<br>**RF-13:** Fichas de proyectos | **PENDIENTE** | Tabla: `APORTE`<br>Por implementar: `DAporte`, `NAporte` |
| **`CU-07: Gestionar Bandeja de Consultas`** | **RF-16:** Mensajes de contacto | **PENDIENTE** | Tabla: `CONSULTA`<br>Por implementar: `DConsulta`, `NConsulta` |
| **`CU-08: Generar Reportes y Estadísticas`** | **RF-17:** Reportes y estadísticas | **PENDIENTE** | Vistas y cálculos analíticos<br>Por implementar: `DReporte`, `NReporte` |
