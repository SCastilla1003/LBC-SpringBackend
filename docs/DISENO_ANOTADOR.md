# Diseño: anotador en vivo y estadísticas automáticas

Estado: **implementado en línea** (ver `CONTRATO_API.md` §8b); falta el modo sin conexión y las estadísticas por jugador. El esquema está en `docs/esquema/V1__esquema_inicial.sql`.
Base de datos: MySQL, empezando desde cero (sin migración desde MongoDB).

## 1. Decisiones tomadas

- Anotador en vivo con jugadas, conteo, outs, bases, cambios e inning (punto 1).
- Login con username y Google, como hoy (punto 2).
- Se agregan ciudad, color, innings, edad y lateralidad, y el estadio tiene tabla propia (punto 3).
- Se empieza desde cero (punto 4).
- La pantalla del anotador va dentro de la app Flutter, solo para el rol `ANOTADOR`.
- Lanzamientos: se registra cada bola, strike y foul.
- Sin conexión: las acciones se guardan en una cola en el dispositivo y se envían cuando haya red (wifi o datos). La pantalla muestra el partido como si estuviera en vivo.

## 2. Flujo de un partido

1. **Programado** (`SCHEDULED`): el admin crea el partido con equipos, fecha y estadio.
2. **Alineación publicada** (`LINEUP_PUBLISHED`): para cada equipo hay 9 bateadores con su orden (1 a 9) y posición defensiva, un lanzador inicial, y banca (suplentes y relevistas). Desde aquí los aficionados pueden ver la alineación.
3. **En vivo** (`LIVE`): el anotador inicia el partido. El sistema valida la alineación, pone al primer bateador del visitante y al lanzador local.
4. **Finalizado** (`FINISHED`): se cierra el partido y se recalculan `standing` y `player_stat`.

No se puede pasar a `LIVE` sin alineación completa (9 bateadores y lanzador por equipo). Publicarla antes es opcional: el partido ya es público como programado.

## 3. Cómo funciona el anotador

El anotador trabaja con una pantalla de botones. Cada acción se guarda en el momento:

| Acción | Efecto automático |
|---|---|
| Lanzamiento: bola, strike cantado, strike swinging, foul | Actualiza el conteo (`bolas`, `strikes`) y el conteo de lanzamientos del lanzador. Con 4 bolas: base por bolas. Con 3 strikes: ponche. Foul con 2 strikes no es out. |
| Resultado de la jugada (hit, out, error, etc.) | Cierra el turno al bate, mueve corredores, suma outs, carreras, hits y errores. |
| Corredores | El anotador indica a dónde va cada corredor (`game_play_movement`). Las carreras salen de quién llega a home. |
| 3 outs | Cambia la mitad de la entrada (`TOP` ↔ `BOTTOM`), limpia las bases, sube el número de entrada si corresponde y pasa el turno al siguiente bateador. |
| Error | El anotador elige el jugador que lo cometió. Suma +1 al equipo (`errores_*`) y +1 al jugador (`player_stat.errores`). |
| Sustitución | Pinch hitter, pinch runner, cambio defensivo o cambio de lanzador. Actualiza al bateador o lanzador actual. |

Antes de guardar, el sistema valida que la jugada sea posible con la situación actual (por ejemplo, no se puede sacar un out en una base vacía sin movimiento válido).

### Reglas de estadísticas

- **Turno oficial (AB):** cuenta todo menos base por bolas, golpe por lanzamiento (HBP), sacrificio de bola y sacrificio de vuelo.
- **Hit (H):** sencillo, doble, triple o jonrón.
- **Ponche (SO):** resultado `STRIKEOUT` del bateador.
- **Carrera anotada (R):** movimiento que termina en home (`base_hasta = 4`).
- **Carreras impulsadas (RBI):** carreras anotadas en la jugada, salvo las que vienen de error o de doble play. *Regla simplificada, hay que confirmarla.*
- **Lanzador:** el lanzador actual recibe los outs, hits, carreras y ponches de cada jugada.
- **Carreras limpias (ER):** carreras que no vienen de error. *Simplificado, hay que confirmarlo.*
- **Lanzamientos:** cuenta cada fila de `game_pitch` del lanzador.
- **Victoria, derrota:** se asignan al cerrar el partido. *Regla de lanzador ganador/perdedor pendiente.*

Cada jugada recalcula las estadísticas de los jugadores que participaron (bateador, lanzador, corredores y defensa). Así, si se deshace una jugada, las cifras quedan bien.

### Deshacer

Solo se puede deshacer la última jugada. El sistema:

1. Restaura el estado de `game` desde `game_play.estado_antes`.
2. Borra la jugada (sus movimientos y lanzamientos se borran en cascada).
3. Recalcula las estadísticas de los jugadores afectados.
4. Sube `game.version` y registra la acción en `audit_log`.

### Modo sin conexión (cola de envío)

- Cada acción del anotador (lanzamiento, jugada, sustitución, deshacer) intenta enviarse al servidor en el momento.
- Si no hay red, la acción se guarda en una **cola local** del dispositivo y se envía cuando haya wifi o datos. La cola sobrevive si se cierra la app.
- La pantalla se actualiza al instante con el estado calculado en el propio dispositivo. Para el anotador, el partido se ve en vivo.
- Cada comando lleva `command_id` (UUID) y `ocurrido_en` (hora en que se anotó en el campo). El servidor guarda esa hora aunque la acción llegue tarde.
- Los comandos se envían **en orden**, en lotes, a `POST /api/v1/anotador/juegos/{id}/comandos`. El servidor aplica cada uno en su propia transacción y responde por cada uno: **aplicado**, **ya existía** (no se aplica dos veces) o **rechazado**.
- Si un comando se rechaza, la app avisa al anotador y vuelve a cargar el estado del servidor. Nada se descarta en silencio.
- Si falla la red, la app reintenta con espera creciente y también al detectar que volvió la conexión.
- Los aficionados ven el marcador con retraso mientras el anotador está sin red. Es lo esperado.
- La cola muestra cuántas acciones están pendientes de enviar.

**Consecuencia técnica:** las reglas de juego tienen que existir en el dispositivo (para calcular el estado sin red) y en el servidor (la fuente de verdad). Para que no se contradigan, se escriben casos de prueba en un archivo JSON (situación inicial, jugada, resultado esperado). Esos casos deben pasar tanto en Java como en Dart.

**Límite:** un partido lo anota un solo dispositivo a la vez. Dos anotadores en el mismo partido no están soportados en esta versión.

### Evitar duplicados y conflictos

- Cada comando lleva un `command_id`. Si la app reenvía algo que el servidor ya guardó, responde "ya existía" y no lo aplica otra vez.
- `game.version` evita que dos escrituras pisen el mismo estado. Si hay un desfase, el comando se rechaza y la app se resincroniza.

## 4. Lo que ve el público

- **Alineación:** antes del partido, con orden de bateo, posiciones y banca.
- **Marcador en vivo:** marcador, entrada y mitad, outs, conteo, bateador y lanzador actuales, corredores en base.
- **Línea de anotación:** carreras, hits y errores por entrada.
- **Box score:** estadísticas de cada jugador en el partido.

El público consulta el estado cada pocos segundos (polling). Después se puede cambiar a SSE si hace falta.

## 5. API propuesta

Son rutas tentativas. Se ajustan al contrato que use Flutter.

| Método | Ruta | Quién | Qué hace |
|---|---|---|---|
| GET | `/api/v1/juegos/{id}/alineacion` | Público | Alineación publicada |
| PUT | `/api/v1/anotador/juegos/{id}/alineacion` | Admin, anotador | Guarda la alineación |
| POST | `/api/v1/anotador/juegos/{id}/iniciar` | Anotador | Pasa a `LIVE` |
| POST | `/api/v1/anotador/juegos/{id}/comandos` | Anotador | Recibe un lote ordenado de comandos: lanzamiento, jugada, sustitución, deshacer. Responde el resultado de cada uno |
| POST | `/api/v1/anotador/juegos/{id}/finalizar` | Anotador | Cierra el partido |
| GET | `/api/v1/juegos/{id}/en-vivo` | Público | Estado actual del partido |
| GET | `/api/v1/juegos/{id}/box-score` | Público | Estadísticas del partido |

Todas las escrituras del anotador son comandos con `command_id` y `ocurrido_en`. El endpoint de comandos es el único que escribe jugadas, lanzamientos y sustituciones.

## 6. Preguntas abiertas

1. **Resultados de jugada.** ¿La lista del esquema (`SINGLE`, `DOUBLE`, `GROUND_OUT`, `FIELDERS_CHOICE`, `REACHED_ON_ERROR`, `STOLEN_BASE`, `WILD_PITCH`, etc.) cubre lo que anotan en la liga?
2. **Lanzamientos.** ¿El anotador registra cada bola y strike? Sin eso, el número de lanzamientos y el promedio de lanzamientos por entrada no se pueden calcular.
3. **Pantalla del anotador.** La app Flutter no tiene pantalla de anotador. ¿La creo dentro de la app (solo para el rol `ANOTADOR`) o como página web?
4. **Actualización del público.** ¿Está bien con polling cada pocos segundos para empezar?
5. **Contrato con Flutter.** Como empezamos desde cero, algunos campos del JSON cambiarán. ¿Puedo ajustar los DTO de Flutter lo mínimo necesario?
Si no respondes algo, asumo: lista de resultados del esquema, polling para el público y ajustar los DTO de Flutter lo mínimo necesario.
