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

## 4. Códigos de Respuesta del Sistema

- **`SUCCESS:`** Operación ejecutada y auditada correctamente en PostgreSQL.
- **`ERROR [400]:`** Error de sintaxis o cantidad incorrecta de parámetros.
- **`ERROR [401]:`** Remitente no identificado en el sistema.
- **`ERROR [403]:`** Acceso denegado (el remitente no posee el rol requerido).
- **`ERROR [404]:`** Entidad o recurso no encontrado.
- **`ERROR [409]:`** Conflicto de unicidad o regla de negocio.
- **`ERROR [422]:`** Parámetro inválido (formato de correo o URL erróneo, id no numérico, etc.).
- **`ERROR [500]:`** Error interno del servidor o falla transaccional SQL.
