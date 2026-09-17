# Seiton (整頓) — Especificación del Proyecto

Documento de contexto único. Describe qué es la app, sus decisiones de producto, funcionalidades, modelo de datos, reglas de comportamiento, pantallas, arquitectura y plan de implementación. Sirve como fuente de contexto completa antes de cualquier trabajo sobre el proyecto.

---

## 1. Decisión clave y alcance

| Parámetro | Decisión |
|---|---|
| Nombre | **Seiton** (整頓 — concepto 5S japonés: "organizar y poner en orden") |
| Qué es | App personal multiplataforma que combina **tareas diarias** y **control financiero** (gastos, ingresos variables, cuentas) en **una sola línea de tiempo** |
| Plataformas | Android e iOS (lógica + UI compartidas) |
| Almacenamiento | 100% local y privado (Room/SQLite embebida), sin login ni sincronización |
| Paquete | `cl.seiton.*` |
| Propósito | Uso personal; proyecto de portafolio publicable en GitHub |

---

## 2. Decisiones de producto (registro)

- **Tareas + finanzas conviven** en un timeline de día unificado (no módulos separados).
- **Setup de cuentas con saldo inicial** en el primer arranque; editable después.
- **Efectivo es una cuenta editable** igual que las bancarias (renombrar, eliminar, activar/inactivar). No hay cuenta fija e inamovible.
- **Transferencias a terceros** se registran como gasto/egreso, con categoría y beneficiario.
- **Ingresos**: registro manual de cada evento (monto + fecha + hora). **Sin generación automática** de recurrencias. Soporta patrones reales: semanales de monto variable, bonos puntuales, quincenales/mensuales fijos o variables.
- **Categorías de gasto fijas** en v1 (lista cerrada).
- **Recordatorios/notificaciones SÍ entran en v1** (opt-in).
- **Exportación/importación de datos SÍ entra en v1**: manual, a un archivo local (JSON), sin backup en la nube ni sincronización automática — coherente con el enfoque 100% local. Pensado para migrar de teléfono sin perder tareas, cuentas, movimientos ni configuración.
- **Versiones del stack**: las define el Android Studio que crea el proyecto KMP. No fijar versiones manuales; re-verificar compatibilidad de KSP/Room/Compose al momento de crear el proyecto.
- **Moneda**: CLP por defecto → detección por región del dispositivo con diálogo confirmable + selector manual en Ajustes.
- **Idioma**: bilingüe ES/EN, sigue el idioma del dispositivo al primer arranque.
- **Temas**: 6 (Light, Dark, Dracula, Liquid Glass, Nord, Solarized).
- Liquid Glass en Compose Multiplatform = glassmorphism simulado, no el efecto nativo de Apple iOS.

---

## 3. Funcionalidades — Tareas

- **Timeline de día**: jornada organizada en bloques de tiempo con hora de inicio y fin.
- **Tipos de tarea** con icono y color: CLASE, COMIDA, TRABAJO, PERSONAL, COMPRAS, TRANSPORTE, OTRO.
- **Mini-tareas**: sub-pasos dentro de cada bloque, expandibles con checkbox. Al completar todas las mini-tareas, el bloque se auto-completa.
- **Gasto vinculado (opcional)**: una tarea puede llevar asociado un gasto real (un movimiento que descuenta de una cuenta real).
- Una tarea sin gasto asociado es un bloque simple.

## 4. Funcionalidades — Finanzas

### Gastos
- Campos: monto, categoría, fecha (día/mes/año), hora, descripción.
- Categorías fijas (7): Electrónica, Transporte, Cuentas y servicios, Shopping, Entretenimiento (películas, teatro), Suscripciones (Netflix, Prime, Twitch, etc.), Otros.

### Ingresos
- Variables, no periódicos ni de monto fijo. Registro manual con monto, fecha y hora.
- Cada evento individual: ingreso semanal variable, bono puntual en fecha específica, ingreso quincenal/mensual fijo o variable.

### Cuentas
- **Efectivo** + **una o más cuentas bancarias** (multi-cuenta opcional/configurable).
- Todas editables: crear, renombrar, eliminar, activar/inactivar. Solo las cuentas activas suman al saldo total.
- Saldo inicial configurable en el primer uso (setup de onboarding) y editable después.

### Tipo de movimiento y efecto en el saldo

| Tipo | Origen | Destino | Efecto en saldo total |
|---|---|---|---|
| Gasto | cuenta propia (efectivo o banco) | externo | Baja |
| Ingreso | externo | cuenta propia | Sube |
| Transferencia interna | cuenta propia | otra cuenta propia | Neutro (redistribuye) |
| Transferencia a terceros | cuenta propia | tercero (beneficiario) | Baja (egreso) |

### Regla de integridad (crítica)
- **El saldo total en todo momento = efectivo + saldo de cada cuenta bancaria activa.**
- Movimientos internos (efectivo↔banco, banco↔banco propio) **no alteran el total**, solo redistribuyen el saldo entre orígenes.
- Gastos, ingresos y transferencias a terceros **sí alteran el total**.
- **El saldo se deriva** (saldo inicial + suma de movimientos) y **nunca se almacena** como dato suelto. Esto garantiza la integridad por diseño.

---

## 5. Modelo de datos

### Task
- id, título (obligatorio), descripción, fecha, hora inicio, hora fin, tipo (TaskType), color opcional, completado.

### MiniTask
- id, título, completado, tarea padre (FK, borrado en cascada).

### Account
- id, nombre, tipo (EFECTIVO | BANCO), saldo inicial, activa (booleano).

### Movement
- id, tipo (GASTO | INGRESO | TRANSFERENCIA_INTERNA | TRANSFERENCIA_EXTERNA), monto, categoría (solo gastos y transferencias a terceros), fecha, hora, descripción, cuenta origen, cuenta destino (solo transferencias), beneficiario (solo transferencias a terceros), tarea vinculada opcional (nullable).

---

## 6. Reglas de comportamiento

- **Vínculo tarea ↔ gasto**: crear una tarea con gasto vinculado crea un movimiento Gasto real (con cuenta origen real seleccionada) ligado a la tarea. Aparece dentro del bloque de la tarea y cuenta en el saldo y en la Billetera.
- **Borrar tarea vinculada**: pregunta si se borra también el gasto vinculado (recomendado: cascada con confirmación). Sin huérfanos.
- **Editar o borrar** una tarea/movimiento con recordatorio activo → reprograma o cancela la notificación.
- **Completar tarea** con gasto: el gasto sigue contando en la Billetera.
- **Timeline unificada**: bloques de tarea (franja horaria) y movimientos de dinero (punto en su hora) se intercalan en un solo scroll cronológico, ordenados por hora.
- **Filtros del timeline**: Todo / Tareas / Dinero.
- Bloques de tarea muestran: hora inicio–fin, icono de tipo, título, gasto asociado 💳; expandibles para ver y marcar mini-tareas.

---

## 7. Pantallas y navegación

| Pantalla | Función |
|---|---|
| **Inicio** | Saldo total + desglose por cuenta (efectivo/bancos), progreso de tareas de hoy, resumen del mes (ingresos / gastos / neto), accesos rápidos |
| **Timeline** ⭐ | Día cronológico unificado (tareas + movimientos), navegación por fecha, filtros Todo/Tareas/Dinero, total del día (ingresos, gastos, neto) |
| **Editor de tarea** | Título, descripción, fecha, hora inicio/fin, tipo, mini-tareas dinámicas, toggle "vincular gasto" (cuenta origen + categoría + monto) |
| **Editor de movimiento** | Selector de tipo (gasto / ingreso / transferencia interna / transferencia a terceros) → formulario según el tipo |
| **Cuentas** | Lista con saldo vivo, crear/renombrar/eliminar/activar, configurar saldo inicial |
| **Billetera (Wallet)** | Resúmenes por día/semana/mes/año, totales, desglose por categoría (gráfico de barras con Canvas), historial de movimientos filtrable |
| **Ajustes** | Grid de 6 temas con preview en vivo, idioma ES/EN, moneda (auto-región + selector manual), recordatorios (toggle + minutos antes), exportar/importar datos, About/GitHub |

**Navegación inferior**: Inicio · Timeline · Billetera · Cuentas · Ajustes. Los editores (tarea y movimiento) se abren como pantallas apiladas.

---

## 8. Onboarding (primer arranque)

1. Diálogo de moneda: "Detectamos tu región (🇨🇱 CL). ¿Usar $CLP?" → Sí / Elegir otra.
2. Setup de cuentas: crear efectivo y cuentas bancarias con su saldo inicial. Editable después.

---

## 9. Recordatorios (v1)

- **Notificación local, opt-in** (el usuario lo activa en Ajustes).
- **Un solo mecanismo**: aviso X minutos antes del inicio de una tarea. No existe lógica de recurrencia automática (ver sección 2, "Sin generación automática" de ingresos). Si el usuario quiere que le avisen de un pago o ingreso esperado (ej. una suscripción próxima a cobrarse), crea una tarea para ese evento y activa su recordatorio — es el mismo sistema de recordatorios de tareas, no una segunda vía donde la app detecta recurrencias financieras por sí sola.
- Android: permiso POST_NOTIFICATIONS (API 33+) + AlarmManager + BroadcastReceiver.
- iOS: UNUserNotificationCenter.

---

## 9.1 Backup y exportación de datos (v1)

- **Exportar**: genera un único archivo (JSON) con todo el estado local — cuentas (con su saldo inicial e historial de movimientos), tareas y mini-tareas, y configuración (tema, idioma, moneda). El usuario lo guarda donde quiera (compartir vía share sheet de Android/iOS, guardarlo en Drive/iCloud manualmente, etc.); la app no sube nada por sí sola.
- **Importar**: lee ese archivo y restaura el estado completo. Caso de uso principal: cambio de teléfono — exportar en el dispositivo viejo, instalar Seiton en el nuevo, importar el archivo, quedar exactamente como estaba.
- **Formato**: JSON versionado (con campo de versión de esquema), para poder detectar incompatibilidades si el modelo de datos cambia entre versiones futuras de la app.
- **Importar es reemplazo completo, no merge**, en v1: si ya hay datos en el dispositivo, se pide confirmación explícita antes de sobrescribirlos. Evita estados inconsistentes por mezclar dos historiales de movimientos.
- Es exportación/importación manual bajo demanda del usuario — no hay backup automático ni en la nube, coherente con el almacenamiento 100% local y privado (sección 1).

## 10. Stack y arquitectura

- **Clean Architecture + MVVM + UDF** (flujo de datos unidireccional con StateFlow).
- Capas: UI (composeApp) → Presentación (ViewModels + StateFlow) → Dominio (modelos + interfaces de repositorio) → Datos (repositorios ↔ Room DAOs ↔ SQLite) → DI (Koin).
- **Room KMP** + sqlite-bundled (SQLite embebido multiplataforma).
- kotlinx-datetime / kotlinx-serialization / kotlinx-coroutines.
- Navegación tipada compartida (Navigation Compose multiplatform).
- Versiones: las define el Android Studio que crea el proyecto (ver 2. Decisiones).

## 11. Estructura del proyecto

```
Seiton/
├── AGENT.md                   # Rol y constraints del agente (lectura obligatoria)
├── README.md                  # Este documento
├── plan_legacy/               # Planes y docs desactualizados (solo referencia)
├── shared/                    # Lógica compartida (dominio, datos, presentación, DI)
│   └── src/
│       ├── commonMain/        # Modelos, Room, repos, ViewModels, expect/actual
│       ├── androidMain/       # Builders Android, AlarmManager
│       └── iosMain/           # Builders iOS, KoinHelper
├── composeApp/                # UI Compose Multiplatform
│   └── src/
│       ├── commonMain/        # App(), temas, pantallas, navegación
│       └── iosMain/           # MainViewController
├── androidApp/                # Entry point Android (MainActivity)
├── iosApp/                    # Entry point iOS (SwiftUI)
└── gradle/                    # Version catalog + wrapper
```

## 12. Restricciones de desarrollo

- **Sin macOS**: el entorno local no puede compilar binarios iOS ni usar simuladores iOS.
- **Dev local**: emulador Android.
- **CI desde F0**: GitHub Actions con runners macOS para compilar iOS, distribuyendo vía TestFlight o Firebase App Distribution.
- DB builder por plataforma: Android usa Context; iOS usa NSHomeDirectory.
- Bootstrap DI: Android en Application.onCreate; iOS vía KoinHelper.

## 13. Fases de implementación

| Fase | Contenido | Criterio de salida |
|---|---|---|
| **F0 Build** | Proyecto KMP (Android Studio), estructura de módulos, CI GitHub Actions desde el día 1 (Android + iOS), paquete `com.seiton` | App vacía compila en Android + CI verde |
| **F1 Datos** | Modelos de dominio (tareas + finanzas), entidades, DAOs, convertidores, repositorios, regla de saldo derivado, DI Koin | Tests de integridad de saldo verdes (transferencia interna no altera total) |
| **F2 Shell** | Motor de 6 temas, diccionario ES/EN, App() + NavHost + bottom bar | App corre vacía y navega en ambas plataformas |
| **F3 Core** | Timeline unificado + editores de tarea y de movimiento (gasto vinculado, mini-tareas, 4 tipos) | Flujo crear/editar/completar OK y saldo correcto |
| **F4 Secundarias** | Inicio, Billetera (gráfico Canvas), Cuentas, Ajustes completos, exportar/importar datos (JSON) | Pantallas navegables y persistentes; exportar → importar en limpio reproduce el estado exacto |
| **F5 Plataforma** | Onboarding (moneda + cuentas), formateo regional, recordatorios opt-in | Notificación dispara antes de la tarea |
| **F6 Cierre** | Pulido Liquid Glass, animaciones, tests finales, verificación completa | `./gradlew build` completo verde |

## 14. Verificación / QA

- `./gradlew build` verde en cada fase.
- **QA manual clave**:
  1. Setup de cuentas con saldo inicial → el total se calcula bien.
  2. Registrar un gasto → el saldo de la cuenta y el total bajan.
  3. Ingreso → sube.
  4. Transferencia interna efectivo→banco → redistribuye pero **el total no cambia**.
  5. Transferencia a terceros → egreso con categoría y beneficiario.
  6. Crear tarea con gasto vinculado → aparece en el bloque y cuenta en Billetera.
  7. Borrar tarea vinculada → pregunta si borrar el gasto.
  8. Reiniciar la app → datos persisten.
  9. Cambiar tema/idioma/moneda en caliente.
  10. Activar recordatorios y verificar que la notificación dispara.
  11. Exportar datos → instalar/reiniciar en limpio → importar el archivo → cuentas, saldos, movimientos, tareas y configuración (tema/idioma/moneda) quedan idénticos al estado original.

---
