# CONTEXTO DEL SISTEMA: PORTAL WEB - FEDER

## 1. Descripción Institucional y Alcance del Portal
La **Fundación Empresarial para el Desarrollo Rural de la Pequeña Industria y Artesanía (FEDER)** es una organización que apoya el desarrollo socioeconómico de comunidades rurales y artesanas de la Chiquitanía (San José de Chiquitos, Roboré, Okinawa, entre otras), impulsando iniciativas como la extracción de aceites, jabonería artesanal (pesoé y totaí) y elaboración de derivados.

El sistema comprende un **Portal Web Institucional y CMS a medida** diseñado para:
1. Brindar autonomía operativa y administrativa al personal de FEDER eliminando la dependencia de terceros para actualizar contenidos.
2. Difundir el trabajo de campo, actividades productivas y testimonios de artesanos y microempresarios beneficiarios.
3. Facilitar un canal interactivo de captación de recursos (*crowdfunding* comunitario) con metas económicas, plazos definidos y seguimiento en tiempo real.
4. Centralizar la atención de consultas ciudadanas recibidas desde el portal.

Para este primer parcial, la interacción administrativa y operativa se ejecuta a través de una **arquitectura asíncrona de 3 capas basada en comandos de correo electrónico (protocolos POP3/SMTP)** y persistencia en **PostgreSQL**.

Alcance de Implementación para el Primer Parcial:
La interacción administrativa y operativa se ejecuta mediante una arquitectura distribuida y asíncrona de 3 capas sin interfaz web tradicional, desacoplada mediante un cliente de red en Java basado en sockets TCP directos (RFC de los protocolos POP3 por el puerto 110 y SMTP por el puerto 25). El sistema implementa un hilo de sondeo continuo (MailVerificationThread) que inspecciona el buzón institucional, un analizador sintáctico determinista (CommandParser / TokenListener) para decodificar las órdenes y parámetros enviados en el correo, la delegación a controladores de negocio (N...) que auditan roles y permisos, y la persistencia transaccional contra un motor relacional PostgreSQL mediante una capa de datos desacoplada (D...) que genera acuses y tablas de respuesta automática despachadas por un hilo de salida (SendEmailThread).
---

## 2. Actores del Sistema
- **Director:** Representa el nivel jerárquico directivo y de máxima autoridad administrativa. Cuenta con privilegios completos: administración de usuarios/roles, contenidos corporativos, aprobación/cierre de campañas de recaudación y visualización de reportes ejecutivos.
- **Secretaria:** Personal administrativo a cargo de la atención ciudadana. Monitorea y clasifica la bandeja de consultas, emite respuestas institucionales y verifica compromisos preliminares de aporte.
- **Instructor:** Personal técnico-operativo de campo en la Chiquitanía. Registra y actualiza noticias, comunicados, galerías multimedia y testimonios de los artesanos.
- **Donante / Visitante:** Usuario externo (público general, cooperantes o empresas aliadas) que navega el catálogo de proyectos, registra compromisos de aporte o envía mensajes de contacto.

---

## 3. Matriz de Casos de Uso y Requisitos Funcionales

### CU-01: Gestionar Usuarios y Roles
- **RF-14: Autenticación de usuarios:** El sistema debe validar el ingreso del personal autorizado mediante credenciales seguras y contraseñas cifradas con hash.
- **RF-15: Gestión de roles y permisos:** El sistema debe permitir asignar roles jerárquicos (Director, Secretaria, Instructor) restringiendo el acceso a las funciones del sistema.

### CU-02: Gestionar Información Institucional
- **RF-03: Gestión de información institucional:** El sistema debe permitir actualizar autónomamente la reseña, misión, visión, objetivos, ubicación y canales de contacto de FEDER.
- **RF-04: Gestión de redes sociales:** El sistema debe permitir registrar y modificar los enlaces oficiales de difusión (Facebook, Instagram, TikTok, WhatsApp).

### CU-03: Gestionar Publicaciones y Medios
- **RF-01: Gestión de noticias y comunicados:** El sistema debe permitir registrar, editar, publicar y archivar notas de prensa, comunicados y artículos informativos.
- **RF-02: Gestión de galería multimedia:** El sistema debe gestionar fotografías y enlaces audiovisuales vinculados a las actividades de campo.
- **RF-08: Gestión de testimonios:** El sistema debe registrar y publicar historias de éxito y testimonios de los microempresarios y comunidades.

### CU-04: Gestionar Comunidades y Programas
- **RF-05: Gestión de comunidades beneficiarias:** El sistema debe catalogar los municipios y comunidades de intervención rural de la Chiquitanía.
- **RF-06: Gestión de programas de asistencia:** El sistema debe administrar los programas productivos y capacitaciones técnicas (aceites, jabones, harinas).
- **RF-07: Búsqueda y filtrado de contenidos:** El sistema debe permitir a los usuarios consultar y filtrar proyectos, actividades y notas por comunidad o rubro.

### CU-05: Gestionar Campañas de Recaudación
- **RF-09: Gestión de campañas de financiamiento:** El sistema debe permitir crear, editar y cerrar campañas con metas económicas y fechas límite delimitadas.
- **RF-10: Seguimiento de recaudación:** El sistema debe calcular y mostrar el avance y porcentaje de recaudación tomando como base exclusivamente los aportes confirmados.

### CU-06: Gestionar Donaciones y Aportes
- **RF-11: Registro de compromisos de aporte:** El sistema debe permitir registrar la intención de donación con montos y modalidades de pago.
- **RF-12: Confirmación del registro de aporte:** El sistema debe despachar un comprobante digital y mensaje de agradecimiento institucional vía correo electrónico (SMTP).
- **RF-13: Descarga de información de proyectos:** El sistema debe facilitar la descarga de resúmenes o fichas técnicas de los planes comunitarios en formato digital.

### CU-07: Gestionar Bandeja de Consultas
- **RF-16: Gestión de mensajes de contacto:** El sistema debe recibir y almacenar las consultas ciudadanas en una bandeja interna, clasificar su estado (Pendiente, En Proceso, Atendido) y permitir responderlas vía correo electrónico.

### CU-08: Generar Reportes y Estadísticas
- **RF-17: Generación de Reportes y Estadísticas:** El sistema debe generar y exportar reportes estadísticos e indicadores consolidados sobre montos recaudados, avance financiero de metas, registro de aportes y volumen de consultas atendidas.