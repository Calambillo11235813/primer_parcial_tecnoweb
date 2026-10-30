# DICCIONARIO Y SINTAXIS DE COMANDOS (PARSER)

Este documento define la gramática formal y el catálogo de comandos reconocidos por el `CommandParser` del sistema FEDER.

---

## 1. Gramática y Formato General

Los comandos se envían en el **Asunto (Subject)** o en el **Cuerpo (Body)** del correo electrónico con la siguiente estructura:

```text
COMANDO [parametro1; parametro2; ...; parametroN]
```

- **Delimitador de parámetros:** Punto y coma (`;`).
- **Comandos sin parámetros:** `COMANDO []` o simplemente `COMANDO`.
- **Sensibilidad:** Los comandos son case-insensitive en el parser, pero se estandarizan en MAYÚSCULAS.
- **Espacios:** Se aplica `trim()` automático a cada parámetro extraído.

---

## 2. Catálogo de Comandos: CU-01 (Usuarios y Roles)

| Comando | Parámetros Requeridos | Rol Autorizado | Descripción | Ejemplo de Uso |
| :--- | :--- | :--- | :--- | :--- |
| **`INSUSR`** | `[nombre; email; pass; rol_id]` | **Director** | Registra un nuevo usuario con contraseña cifrada (SHA-256) | `INSUSR [Maria Lopez; mlopez@feder.org.bo; Secr2026!; 2]` |
| **`MODUSR`** | `[id; nombre; email; estado; rol_id]` | **Director** | Modifica los datos o estado de un usuario existente | `MODUSR [2; Maria Lopez P.; mlopez@feder.org.bo; Activo; 2]` |
| **`DELUSR`** | `[id_usuario]` | **Director** | Realiza baja lógica (`Inactivo`) y audita la acción | `DELUSR [2]` |
| **`LISUSR`** | `[]` | **Director, Secretaria, Instructor** | Retorna la lista tabulada de usuarios y sus roles | `LISUSR []` |

### Parámetros de Referencia:
- **`rol_id`:**
  - `1`: Director
  - `2`: Secretaria
  - `3`: Instructor
- **`estado`:** `Activo`, `Inactivo`

---

## 3. Códigos de Respuesta del Sistema

- **`SUCCESS:`** Operación ejecutada y auditada correctamente en PostgreSQL.
- **`ERROR [400]:`** Error de sintaxis o cantidad incorrecta de parámetros.
- **`ERROR [401]:`** Remitente no identificado en el sistema.
- **`ERROR [403]:`** Acceso denegado (el remitente no posee el rol requerido).
- **`ERROR [404]:`** Entidad o recurso no encontrado (ej. usuario o rol inexistente).
- **`ERROR [409]:`** Conflicto de unicidad o regla de negocio (ej. email duplicado o auto-eliminación).
- **`ERROR [422]:`** Parámetro inválido (formato de correo erróneo, id no numérico, etc.).
- **`ERROR [500]:`** Error interno del servidor o falla transaccional SQL.

