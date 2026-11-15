# ESTADO DEL PROYECTO: PRIMER PARCIAL TECNOLOGÍA WEB (FEDER)

**Última actualización:** 15 de noviembre de 2026  
**Entorno:** Java puro (sin frameworks externos de correo) + PostgreSQL institucional / local  
**Meta de desarrollo:** Plan de 3 días (Casos de uso codificados - pendiente pruebas manuales)

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
- `COMANDOS_SINTAXIS.md`: Diccionario completo de los 44 comandos del sistema (CU-01 al CU-08 + `HELP`).
- `ESTANDAR_CODIFICACION.md`: Convenciones de código Java del proyecto.

---

## 2. Estado de Implementación por Casos de Uso

| Caso de Uso | Requisitos Funcionales Asociados | Estado | Componentes Desarrollados |
| :--- | :--- | :--- | :--- |
| **`CU-01: Gestionar Usuarios y Roles`** | **RF-14:** Autenticación de usuarios<br>**RF-15:** Roles y permisos | **IMPLEMENTADO (End-to-End)** | • `Usuario.java`, `Rol.java`, `Auditoria.java`<br>• `DUsuario.java`, `NUsuario.java`<br>• Comandos: `INSUSR`, `MODUSR`, `DELUSR`, `LISUSR` |
| **`CU-02: Gestionar Información Institucional`** | **RF-03:** Información institucional<br>**RF-04:** Redes sociales | **IMPLEMENTADO (End-to-End)** | • `PaginaInstitucional.java`, `RedSocial.java`<br>• `DPaginaInstitucional.java`, `DRedSocial.java` (CRUD transaccional + Auditoría)<br>• `NInstitucional.java` (RBAC Director + validación URL)<br>• Comandos: `INSPAG`, `MODPAG`, `DELPAG`, `LISPAG`, `INSRED`, `MODRED`, `DELRED`, `LISRED` |
| **`CU-03: Gestionar Publicaciones y Medios`** | **RF-01:** Noticias y comunicados<br>**RF-02:** Galería multimedia<br>**RF-08:** Testimonios | **IMPLEMENTADO (End-to-End)** | • `Noticia.java`, `GaleriaMultimedia.java`, `Testimonio.java`<br>• `DNoticia.java`, `DGaleriaMultimedia.java`, `DTestimonio.java` (CRUD transaccional + Auditoría)<br>• `NPublicacion.java` (RBAC Director/Instructor + validaciones de dominio)<br>• Comandos: `INSNOT`, `MODNOT`, `DELNOT`, `LISNOT`, `INSMUL`, `MODMUL`, `DELMUL`, `LISMUL`, `INSTES`, `MODTES`, `DELTES`, `LISTES` |
| **`CU-04: Gestionar Comunidades y Programas`** | **RF-05:** Comunidades beneficiarias<br>**RF-06:** Programas de asistencia<br>**RF-07:** Búsqueda y filtrado | **IMPLEMENTADO (End-to-End)** | • `Comunidad.java`, `ProgramaProductivo.java`<br>• `DComunidad.java`, `DProgramaProductivo.java` (CRUD transaccional + Auditoría + búsqueda transversal `UNION ALL`)<br>• `NComunidad.java` (RBAC Director/Instructor + unicidad de comunidad)<br>• Comandos: `INSCOM`, `MODCOM`, `DELCOM`, `LISCOM`, `INSPRO`, `MODPRO`, `DELPRO`, `LISPRO`, `BUSCON` |
| **`CU-05: Gestionar Campañas de Recaudación`** | **RF-09:** Campañas de financiamiento<br>**RF-10:** Seguimiento recaudación | **IMPLEMENTADO (End-to-End)** | • `Campana.java`<br>• `DCampana.java` (CRUD transaccional + Auditoría + cálculo de avance y % sobre aportes confirmados)<br>• `NCampana.java` (RBAC Director + validación de metas y plazos)<br>• Comandos: `INSCMP`, `MODCMP`, `DELCMP`, `LISCMP`, `SEGCMP` |
| **`CU-06: Gestionar Donaciones y Aportes`** | **RF-11:** Registro de aportes<br>**RF-12:** Confirmación de aporte<br>**RF-13:** Fichas de proyectos | **IMPLEMENTADO (End-to-End)** | • `Aporte.java`<br>• `DAporte.java` (Registro, verificación con sincronización transaccional de `CAMPANA.monto_recaudado` y ficha técnica digital)<br>• `NAporte.java` (Comprobante digital de agradecimiento SMTP + RBAC Director/Secretaria)<br>• Comandos: `INSAPO`, `MODAPO`, `DELAPO`, `LISAPO`, `FICPRO` |
| **`CU-07: Gestionar Bandeja de Consultas`** | **RF-16:** Mensajes de contacto | **IMPLEMENTADO (End-to-End)** | • `Consulta.java`<br>• `DConsulta.java` (Bandeja interna + clasificación transaccional + Auditoría)<br>• `NConsulta.java` (Recepción ciudadana + RBAC Director/Secretaria + respuesta SMTP)<br>• Comandos: `INSCON`, `MODCON`, `RESCON`, `DELCON`, `LISCON` |
| **`CU-08: Generar Reportes y Estadísticas`** | **RF-17:** Reportes y estadísticas | **IMPLEMENTADO (End-to-End)** | • `DReporte.java` (KPIs ejecutivos, avance financiero de metas, aportes por modalidad, volumen de consultas y bitácora de auditoría)<br>• `NReporte.java` (RBAC exclusivo para Director)<br>• Comandos: `REPGEN`, `REPCMP`, `REPAPO`, `REPCON`, `REPAUD` |
