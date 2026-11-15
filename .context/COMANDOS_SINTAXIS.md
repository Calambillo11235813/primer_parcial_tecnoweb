# DICCIONARIO Y SINTAXIS DE COMANDOS (PARSER)

Este documento define la gramática formal y el catálogo de comandos reconocidos por el `CommandParser` del sistema FEDER.

---

## 1. Gramática y Formato General

Los comandos se envían en el **Asunto (Subject)**, en el **Cuerpo (Body)** o combinados (comando en Asunto y `[...]` en Cuerpo):

```text
COMANDO [parametro1; parametro2; ...; parametroN]
```

- **Delimitador de parámetros:** Punto y coma (`;`).
- **Comandos sin parámetros:** `COMANDO []` o simplemente `COMANDO`.
- **Sensibilidad:** Los comandos son case-insensitive en el parser, pero se estandarizan en MAYÚSCULAS.

---

## 2. Catálogo de Comandos: CU-01 (Usuarios y Roles)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSUSR`** | `[nombre; email; pass; rol_id]` | **Director** | Registra un nuevo usuario con contraseña cifrada (SHA-256) | `INSUSR [Maria Lopez; mlopez@feder.org.bo; Secr2026!; 2]` |
| **`MODUSR`** | `[id; nombre; email; estado; rol_id]` | **Director** | Modifica los datos o estado de un usuario existente | `MODUSR [2; Maria Lopez P.; mlopez@feder.org.bo; Activo; 2]` |
| **`DELUSR`** | `[id_usuario]` | **Director** | Realiza baja lógica (`Inactivo`) y audita la acción | `DELUSR [2]` |
| **`LISUSR`** | `[]` | **Director, Secretaria, Instructor** | Retorna la lista tabulada de usuarios y sus roles | `LISUSR []` |

---

## 3. Catálogo de Comandos: CU-02 (Información Institucional y Redes Sociales)

### A. Páginas Institucionales (RF-03)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSPAG`** | `[titulo; contenido]` | **Director** | Registra una sección institucional (Reseña, Objetivos, Contacto) | `INSPAG [Objetivos; Impulsar la produccion artesanal en la Chiquitania.]` |
| **`MODPAG`** | `[id_pagina; titulo; contenido]` | **Director** | Actualiza el título o contenido de una página institucional | `MODPAG [1; Mision; Ofrecer servicios financieros y asistencia tecnica rural.]` |
| **`DELPAG`** | `[id_pagina]` | **Director** | Elimina una sección institucional y audita la acción | `DELPAG [3]` |
| **`LISPAG`** | `[]` | **Público / Todos** | Lista todas las secciones institucionales con fecha y autor | `LISPAG []` |

### B. Redes Sociales (RF-04)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSRED`** | `[plataforma; url_enlace]` | **Director** | Registra un canal oficial de difusión (`http://` o `https://`) | `INSRED [Facebook; https://facebook.com/feder.scz]` |
| **`MODRED`** | `[id_red_social; plataforma; url_enlace; estado]` | **Director** | Modifica los datos o estado (`Activo`/`Inactivo`) de una red social | `MODRED [1; Facebook Oficial; https://facebook.com/feder.bo; Activo]` |
| **`DELRED`** | `[id_red_social]` | **Director** | Realiza baja lógica (`Inactivo`) de una red social | `DELRED [1]` |
| **`LISRED`** | `[]` | **Público / Todos** | Lista todos los enlaces oficiales de redes sociales | `LISRED []` |

---

## 4. Catálogo de Comandos: CU-03 (Publicaciones y Medios)

### A. Noticias y Comunicados (RF-01)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSNOT`** | `[titulo; contenido; imagen_principal; etiquetas]` | **Director, Instructor** | Registra y publica una noticia o comunicado institucional | `INSNOT [Feria Artesanal 2026; Se inauguro la feria productiva en San Ignacio.; https://feder.org.bo/img/feria.jpg; Feria,Chiquitania]` |
| **`MODNOT`** | `[id_noticia; titulo; contenido; imagen_principal; etiquetas; estado]` | **Director, Instructor** | Edita una noticia o cambia su estado (`Publicado`, `Borrador`, `Archivado`) | `MODNOT [1; Feria Artesanal 2026; Gran participacion de 15 comunidades.; https://feder.org.bo/img/feria2.jpg; Feria,Exito; Publicado]` |
| **`DELNOT`** | `[id_noticia]` | **Director, Instructor** | Archiva una noticia cambiando su estado a `Archivado` | `DELNOT [1]` |
| **`LISNOT`** | `[]` | **Público / Todos** | Lista todas las noticias con fecha, etiquetas, estado y autor | `LISNOT []` |

### B. Galería Multimedia (RF-02)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSMUL`** | `[titulo; tipo; url_archivo; descripcion; noticia_id]` | **Director, Instructor** | Registra una fotografía (`Imagen`) o enlace audiovisual (`Video`) asociado a una noticia | `INSMUL [Taller de Tejido; Imagen; https://feder.org.bo/media/taller1.jpg; Capacitacion en telares; 1]` |
| **`MODMUL`** | `[id_multimedia; titulo; tipo; url_archivo; descripcion; noticia_id]` | **Director, Instructor** | Modifica los datos de un recurso de la galería multimedia | `MODMUL [1; Video Inauguracion; Video; https://youtube.com/watch?v=abc123; Resumen de la jornada; 1]` |
| **`DELMUL`** | `[id_multimedia]` | **Director, Instructor** | Elimina un recurso multimedia y audita la acción | `DELMUL [1]` |
| **`LISMUL`** | `[]` | **Público / Todos** | Lista todos los recursos multimedia junto a su noticia vinculada | `LISMUL []` |

### C. Testimonios e Historias de Éxito (RF-08)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSTES`** | `[titulo; texto; fotografia; enlace_audiovisual; comunidad_id]` | **Director, Instructor** | Registra y publica un testimonio de microempresario o comunidad | `INSTES [Tejedoras de Chochis; Gracias al taller duplicamos nuestras ventas mensuales.; https://feder.org.bo/img/tes1.jpg; https://youtube.com/watch?v=tes1; 1]` |
| **`MODTES`** | `[id_testimonio; titulo; texto; fotografia; enlace_audiovisual; estado; comunidad_id]` | **Director, Instructor** | Modifica un testimonio o su estado (`Publicado`, `Archivado`) | `MODTES [1; Asociacion de Chochis; Hoy exportamos tejidos a nivel nacional.; https://feder.org.bo/img/tes1.jpg; https://youtube.com/watch?v=tes1; Publicado; 1]` |
| **`DELTES`** | `[id_testimonio]` | **Director, Instructor** | Archiva un testimonio cambiando su estado a `Archivado` | `DELTES [1]` |
| **`LISTES`** | `[]` | **Público / Todos** | Lista todos los testimonios junto con su comunidad beneficiaria | `LISTES []` |

---

## 5. Catálogo de Comandos: CU-04 (Comunidades y Programas)

### A. Comunidades Beneficiarias (RF-05)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSCOM`** | `[nombre; descripcion; ubicacion]` | **Director, Instructor** | Registra una nueva comunidad o municipio beneficiario | `INSCOM [Santiago de Chiquitos; Comunidad ecoturistica y artesanal; Robore, Santa Cruz]` |
| **`MODCOM`** | `[id_comunidad; nombre; descripcion; ubicacion; estado]` | **Director, Instructor** | Modifica los datos o estado (`Activa`, `Inactiva`) de una comunidad | `MODCOM [1; San Jose de Chiquitos; Municipio historico y artesanal; Chiquitania, Santa Cruz; Activa]` |
| **`DELCOM`** | `[id_comunidad]` | **Director, Instructor** | Realiza baja lógica (`Inactiva`) de una comunidad | `DELCOM [4]` |
| **`LISCOM`** | `[]` | **Público / Todos** | Lista todas las comunidades beneficiarias registradas | `LISCOM []` |

### B. Programas Productivos y Asistencia Técnica (RF-06)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSPRO`** | `[nombre; descripcion; area_productiva; poblacion_beneficiaria; comunidad_id]` | **Director, Instructor** | Registra un programa productivo o capacitación técnica | `INSPRO [Extraccion de Aceite de Totai; Capacitacion en prensado en frio; Aceites Naturales; 45 mujeres artesanas; 1]` |
| **`MODPRO`** | `[id_programa; nombre; descripcion; area_productiva; poblacion_beneficiaria; comunidad_id]` | **Director, Instructor** | Modifica los datos de un programa productivo existente | `MODPRO [1; Jaboneria de Pesoe y Totai; Produccion de jabones medicinales; Jaboneria Artesanal; 60 familias rurales; 2]` |
| **`DELPRO`** | `[id_programa]` | **Director, Instructor** | Elimina un programa productivo y audita la acción | `DELPRO [1]` |
| **`LISPRO`** | `[]` | **Público / Todos** | Lista todos los programas productivos con su comunidad y rubro | `LISPRO []` |

### C. Búsqueda y Filtrado de Contenidos (RF-07)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`BUSCON`** | `[criterio_busqueda]` | **Público / Todos** | Filtra programas productivos, noticias y testimonios por comunidad, rubro o palabra clave | `BUSCON [Robore]` o `BUSCON [Aceites]` |

---

## 6. Catálogo de Comandos: CU-07 (Bandeja de Consultas Ciudadanas - RF-16)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSCON`** | `[nombre; asunto; mensaje]` o `[nombre; correo; asunto; mensaje]` | **Público / Visitante / Todos** | Registra una consulta ciudadana en la bandeja interna con estado `Pendiente` | `INSCON [Carlos Roca; Talleres en Robore; Deseo informacion sobre inscripciones.]` |
| **`MODCON`** | `[id_consulta; estado]` | **Director, Secretaria** | Clasifica el estado de una consulta (`Pendiente`, `En Proceso`, `Atendido`) | `MODCON [1; En Proceso]` |
| **`RESCON`** | `[id_consulta; respuesta_institucional]` | **Director, Secretaria** | Emite respuesta oficial vía correo electrónico y marca la consulta como `Atendido` | `RESCON [1; Estimado Carlos, las inscripciones abren este lunes en nuestra oficina de Robore.]` |
| **`DELCON`** | `[id_consulta]` | **Director, Secretaria** | Elimina una consulta de la bandeja y audita la acción | `DELCON [1]` |
| **`LISCON`** | `[]` o `[estado]` | **Director, Secretaria** | Lista todas las consultas de la bandeja o filtra por estado (`Pendiente`, `En Proceso`, `Atendido`) | `LISCON []` o `LISCON [Pendiente]` |

---

## 7. Códigos de Respuesta del Sistema

- **`SUCCESS:`** Operación ejecutada y auditada correctamente en PostgreSQL.
- **`ERROR [400]:`** Error de sintaxis o cantidad incorrecta de parámetros.
- **`ERROR [401]:`** Remitente no identificado en el sistema.
- **`ERROR [403]:`** Acceso denegado (el remitente no posee el rol requerido).
- **`ERROR [404]:`** Entidad o recurso no encontrado.
- **`ERROR [409]:`** Conflicto de unicidad o regla de negocio.
- **`ERROR [422]:`** Parámetro inválido (formato de correo o URL erróneo, id no numérico, etc.).
- **`ERROR [500]:`** Error interno del servidor o falla transaccional SQL.
