# Contrato de la API v1 (MySQL)

Estado: **propuesta para revisión**. Todavía no está implementado. Sigue al esquema de `docs/esquema/V1__esquema_inicial.sql`.
Las reglas del anotador en vivo están en `docs/DISENO_ANOTADOR.md` y su API se define aparte.

## 1. Convenciones

- Base: `/api/v1`. JSON en camelCase.
- **IDs numéricos** (`123`), no texto. La app debe dejar de tratarlos como `String`.
- Fechas: `fecha` = `2026-02-07`; `fechaHora` = `2026-02-07T15:00` (hora local de Bogotá, sin zona).
- Enumeraciones: los mismos valores que la base de datos.
  - Estado de temporada: `DRAFT`, `ACTIVE`, `FINISHED`.
  - Estado de partido: `SCHEDULED`, `LINEUP_PUBLISHED`, `LIVE`, `FINISHED`, `SUSPENDED`.
  - Mitad de entrada: `TOP` (batea el visitante), `BOTTOM` (batea el local).
  - Lado de bateo: `D`, `Z`, `A`. Lado de lanzamiento: `D`, `Z`.
- Errores con el formato actual (sin cambios):
  ```json
  { "codigo": "VALIDACION", "mensaje": "El nombre es obligatorio", "detalles": { "nombre": "obligatorio" } }
  ```
  Códigos: `VALIDACION`, `SOLICITUD_INVALIDA`, `NO_AUTENTICADO`, `CREDENCIALES_INVALIDAS`, `ACCESO_DENEGADO`, `NO_ENCONTRADO`, `METODO_NO_PERMITIDO`, `CONFLICTO`, `ARCHIVO_DEMASIADO_GRANDE`, `TIPO_NO_SOPORTADO`, `SERVICIO_NO_DISPONIBLE`, `ERROR_INTERNO`.
- Quién puede hacer qué:
  - **Público** (sin token): todos los GET de temporadas, equipos, jugadores, partidos, estadios, posiciones y alineaciones publicadas.
  - **ADMIN**: crear, editar y borrar.
  - **ANOTADOR** o **ADMIN**: alineaciones y el anotador en vivo.
- Crear devuelve `201` con el recurso. Editar devuelve `200` con el recurso. Borrar devuelve `204`.

## 2. Autenticación (sin cambio de rutas)

| Método | Ruta | Cambio |
|---|---|---|
| POST | `/auth/login` | Igual |
| POST | `/auth/registro` | El rol nuevo es `FAN` (antes `USER`) |
| POST | `/auth/google` | Igual |
| GET | `/auth/yo` | Igual |

El token trae `roles` con uno de estos valores: `ADMIN`, `ANOTADOR`, `FAN`.

## 3. Temporadas

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| GET | `/temporadas` | Público | Lista de temporadas |
| GET | `/temporadas/actual` | Público | La temporada `ACTIVE` más reciente (`404` si no hay) |
| GET | `/temporadas/{id}` | Público | Temporada |
| POST | `/temporadas` | ADMIN | Crea |
| PUT | `/temporadas/{id}` | ADMIN | Edita |
| DELETE | `/temporadas/{id}` | ADMIN | `409` si tiene partidos o plantilla |

Temporada:
```json
{ "id": 1, "nombre": "Temporada 2026", "anio": 2026, "estado": "ACTIVE",
  "fechaInicio": "2026-01-10", "fechaFin": "2026-06-30" }
```

Creación (`POST`):
- `nombre` obligatorio. `anio` obligatorio (1900–2100). `estado` opcional, por defecto `DRAFT`.
- `fechaFin` no puede ser anterior a `fechaInicio`.
- Conflicto si ya existe otra temporada con el mismo `nombre` y `anio`.

## 4. Equipos

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| GET | `/equipos` | Público | Lista |
| GET | `/equipos/{id}` | Público | Equipo |
| POST | `/equipos` | ADMIN | Crea |
| PUT | `/equipos/{id}` | ADMIN | Edita |
| DELETE | `/equipos/{id}` | ADMIN | `409` si participa en partidos o plantillas |
| GET | `/temporadas/{id}/equipos` | Público | Equipos inscritos en la temporada o con jugadores en su plantilla |
| POST | `/temporadas/{id}/equipos` | ADMIN | Inscribe un equipo (`{ "equipoId": 4 }`), aunque no tenga jugadores. `409` si ya está |
| DELETE | `/temporadas/{id}/equipos/{equipoId}` | ADMIN | Quita la inscripción. `409` si tiene jugadores o partidos en la temporada |

Equipo:
```json
{ "id": 4, "nombre": "Pelícanos de Bocagrande", "abreviatura": "PEL",
  "ciudad": "Cartagena", "color": "azul", "escudoUrl": null }
```

Creación: `nombre` y `abreviatura` obligatorios (`abreviatura` 2–5 letras, se guarda en mayúsculas). Conflicto si `nombre` o `abreviatura` ya existen.

El escudo se sube al bucket de Railway: ver la sección 6b.

## 5. Jugadores y plantilla

Un jugador existe una sola vez. Su pertenencia a un equipo es por temporada (plantilla).

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| GET | `/jugadores/{id}` | Público | Jugador |
| POST | `/jugadores` | ADMIN | Crea |
| PUT | `/jugadores/{id}` | ADMIN | Edita |
| DELETE | `/jugadores/{id}` | ADMIN | `409` si tiene jugadas, plantillas o alineaciones |
| GET | `/temporadas/{id}/equipos/{equipoId}/plantilla` | Público | Plantilla del equipo |
| GET | `/temporadas/{id}/agentes-libres` | Público | Jugadores sin equipo en la temporada |
| POST | `/temporadas/{id}/plantilla` | ADMIN | Inscribe un jugador |
| PUT | `/temporadas/{id}/plantilla/{entradaId}` | ADMIN | Cambia equipo, número o posición |
| DELETE | `/temporadas/{id}/plantilla/{entradaId}` | ADMIN | Quita de la temporada |

Jugador:
```json
{ "id": 17, "nombres": "Luis", "apellidos": "Calvo", "fechaNacimiento": "2001-05-14",
  "edad": 25, "batea": "D", "lanza": null, "fotoUrl": null }
```

Entrada de plantilla:
```json
{ "id": 40, "jugador": { "id": 17, "nombres": "Luis", "apellidos": "Calvo", "batea": "D", "lanza": null, "fotoUrl": null },
  "equipo": { "id": 4, "nombre": "Pelícanos de Bocagrande", "abreviatura": "PEL" },
  "numero": 7, "posicion": "SS" }
```

Reglas:
- Un jugador solo puede estar una vez por temporada. Conflicto si ya está inscrito; para cambiarlo de equipo se usa `PUT`.
- `equipo` en `null` = agente libre.
- `posicion` admitida: `P`, `C`, `1B`, `2B`, `3B`, `SS`, `LF`, `CF`, `RF`, `DH`.

## 6. Estadios

| Método | Ruta | Quién |
|---|---|---|
| GET | `/estadios`, `/estadios/{id}` | Público |
| POST, PUT, DELETE | `/estadios`, `/estadios/{id}` | ADMIN |

Estadio: `{ "id": 2, "nombre": "Parque de Pelota El Laguito", "ubicacion": "Cartagena" }`.
`nombre` es único.

## 6b. Fotos y escudos (bucket de Railway)

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| POST | `/jugadores/{id}/foto` | ADMIN | Multipart, campo `archivo` (JPG, PNG o WEBP, máx. 5 MB). Devuelve el jugador |
| DELETE | `/jugadores/{id}/foto` | ADMIN | Quita la foto. Devuelve el jugador |
| GET | `/jugadores/{id}/foto` | Público | La imagen |
| POST | `/equipos/{id}/escudo` | ADMIN | Igual que la foto. Devuelve el equipo |
| DELETE | `/equipos/{id}/escudo` | ADMIN | Quita el escudo. Devuelve el equipo |
| GET | `/equipos/{id}/escudo` | Público | La imagen |

- El bucket de Railway es privado: la API guarda la imagen en `jugadores/{id}` o `equipos/{id}` y la sirve ella misma.
- `fotoUrl` y `escudoUrl` quedan como ruta de la API con versión, p. ej. `/api/v1/jugadores/17/foto?v=1732400000000`; cada subida cambia `v` para que las apps no muestren la imagen anterior desde caché.
- El tipo se comprueba por el contenido del archivo, no por la extensión (`TIPO_NO_SOPORTADO` si no es imagen; `ARCHIVO_DEMASIADO_GRANDE` si pasa de 5 MB).
- Si el bucket no está configurado, subir o ver responde `SERVICIO_NO_DISPONIBLE`.

## 7. Partidos

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| GET | `/temporadas/{id}/partidos` | Público | Lista. Filtros: `estado`, `equipoId`, `desde`, `hasta` (fechas) |
| GET | `/partidos/{id}` | Público | Detalle |
| POST | `/temporadas/{id}/partidos` | ADMIN | Crea (`SCHEDULED`) |
| PUT | `/partidos/{id}` | ADMIN | Edita fecha, estadio o entradas |
| DELETE | `/partidos/{id}` | ADMIN | Solo si está `SCHEDULED` |
| GET | `/temporadas/{id}/posiciones` | Público | Tabla de posiciones |

Partido (lista, resumen):
```json
{ "id": 9, "temporadaId": 1, "fechaHora": "2026-02-07T15:00", "estado": "FINISHED",
  "estadio": { "id": 2, "nombre": "Parque de Pelota El Laguito", "ubicacion": "Cartagena" },
  "equipoLocal": { "id": 4, "nombre": "Pelícanos de Bocagrande", "abreviatura": "PEL", "color": "azul", "escudoUrl": null },
  "equipoVisita": { "id": 5, "nombre": "Corsarios de Turbaco", "abreviatura": "COR", "color": "verde", "escudoUrl": null },
  "marcador": { "local": 8, "visita": 6 }, "ganadorId": 4 }
```

Partido (detalle) agrega:
```json
{ "entradasProgramadas": 9, "entradaActual": 9, "mitad": "BOTTOM",
  "lineScore": { "entradasProgramadas": 9, "entradasExtra": false,
                 "entradas": [ { "numero": 1, "local": 0, "visita": 2 }, { "numero": 2, "local": null, "visita": null } ] },
  "situacion": { "outs": 2, "bolas": 1, "strikes": 2,
                 "bateador": { "id": 17, "nombre": "Luis Calvo" },
                 "lanzador": { "id": 22, "nombre": "Pedro Ruiz" },
                 "corredores": { "primera": null, "segunda": 31, "tercera": null } },
  "hits": { "local": 12, "visita": 9 }, "errores": { "local": 1, "visita": 0 },
  "version": 14 }
```

Creación (`POST`): `equipoLocalId`, `equipoVisitaId`, `fechaHora` obligatorios. `estadioId` y `entradasProgramadas` (1–15, por defecto 9) opcionales. Validaciones: los dos equipos deben ser distintos, y deben existir.

Ciclo de vida:
`SCHEDULED` → `LINEUP_PUBLISHED` (al publicar la alineación) → `LIVE` (el anotador inicia) → `FINISHED` (el anotador finaliza). `SUSPENDED` lo pone el admin.

Marcador por entrada (`lineScore`), como en MLB:
- Partido programado: tantas casillas vacías como entradas programadas.
- En vivo: las entradas programadas (o más en extra innings); las entradas sin jugar llevan `null`.
- Finalizado: solo las entradas jugadas. Un partido de 7 entradas muestra 7.
- Si pasa de las entradas programadas, `entradasExtra` es `true` para que la app lo avise.
- La columna `visita` corresponde a la mitad `TOP` y la `local` a la `BOTTOM`.

Posiciones:
```json
[{ "equipo": { "id": 4, "nombre": "Pelícanos de Bocagrande", "abreviatura": "PEL" },
   "juegos": 10, "ganados": 7, "perdidos": 3, "pct": 0.700,
   "carrerasFavor": 61, "carrerasContra": 48 }]
```
Orden: `pct` descendente, y después `carrerasFavor - carrerasContra`.

## 8. Alineaciones

| Método | Ruta | Quién | Respuesta |
|---|---|---|---|
| GET | `/partidos/{id}/alineacion` | Público si está publicada; si no, ADMIN o ANOTADOR | Alineación completa: titulares y banca |
| GET | `/partidos/{id}/alineacion/pitcheo` | Igual que la anterior | Lanzador inicial y relevistas de cada equipo (botón de pitcheo) |
| PUT | `/partidos/{id}/alineacion` | ADMIN o ANOTADOR | Guarda; con `publicar: true` pasa a `LINEUP_PUBLISHED` |

Regla: el lanzador inicial normalmente **no va en el orden de bateo** (la liga usa DH). Si batea, va en el orden de bateo con posición `P`; esa fila cuenta como su lanzador inicial. La posición `P` solo puede ser del lanzador inicial. Los relevistas son los jugadores de la banca con lado de lanzamiento registrado.

Cuerpo de `PUT`:
```json
{ "publicar": true,
  "local": {
    "lanzadorInicial": 22,
    "bateo": [ { "orden": 1, "jugadorId": 17, "posicion": "CF" }, "... 9 en total ..." ],
    "banca": [ 31, 33 ] },
  "visita": { "lanzadorInicial": 25, "bateo": [ "..." ], "banca": [ 40 ] } }
```

Validaciones (al publicar, todas; al guardar sin publicar, solo formato):
- `bateo` tiene exactamente 9 elementos, con `orden` 1..9 sin repetir.
- Cada jugador pertenece a la plantilla de ese equipo en esa temporada. Un jugador no puede aparecer dos veces.
- `lanzadorInicial` pertenece a la plantilla y tiene `lanza` no nulo.
- Solo se puede editar si el partido está `SCHEDULED` o `LINEUP_PUBLISHED`. Si no, `CONFLICTO`.

Respuesta de `GET`:
```json
{ "publicada": true,
  "local": { "equipo": { "id": 4, "nombre": "Pelícanos de Bocagrande" },
    "lanzadorInicial": { "id": 22, "nombre": "Pedro Ruiz" },
    "bateo": [ { "orden": 1, "posicion": "CF", "jugador": { "id": 17, "nombres": "Luis", "apellidos": "Calvo", "batea": "D" } } ],
    "banca": [ { "id": 31, "nombres": "Ana", "apellidos": "Mora", "batea": "Z", "lanza": null, "fotoUrl": null } ] },
  "visita": { "...": "igual" } }
```

## 9. Predicción

`POST /prediccion` y `GET /prediccion/opciones` mantienen las mismas entradas y la misma respuesta. La API no calcula nada: reenvía la petición al servicio Python desplegado en Railway, cuya URL está en la variable `PREDICCION_URL`. Si la variable falta o el servicio no responde, la API devuelve `SERVICIO_NO_DISPONIBLE`.

Contrato del servicio Python: `POST /predecir` y `GET /opciones`, con las mismas formas JSON.

## 10. Pendiente en otras fases

- **Anotador en vivo** (`/anotador/partidos/{id}/...`): se define en `docs/DISENO_ANOTADOR.md`.
- **Estadísticas** (`/temporadas/{id}/estadisticas/bateo`, `/lanzadores`, `/jugadores/{id}/estadisticas`, `/partidos/{id}/box-score`): dependen de las jugadas.

## 11. Cambios que la app Flutter debe absorber

- IDs: de `String` (UUID) a `int`.
- Partidos: `fecha` + `hora` pasan a `fechaHora`; `equipoLocal` y `equipoVisitante` pasan a `equipoLocal` y `equipoVisita`; los marcadores pasan a `marcador.local` y `marcador.visita`.
- Bateadores y lanzadores: se unifican en `jugador`; la pertenencia a un equipo viene de la plantilla de la temporada.
- Roles: `USER` pasa a `FAN`.
- Rutas: `/temporadas/{id}/bateadores` y `/lanzadores` desaparecen; se usan la plantilla, las alineaciones y los agentes libres.

## 12. Decisiones tomadas

- IDs numéricos y enumeraciones en inglés (`ACTIVE`, `LIVE`…). Flutter las traduce para mostrarlas.
- La alineación pública incluye titulares **y** banca, más un endpoint aparte de pitcheo.
- El marcador por entrada sigue el formato de MLB: 9 entradas en vivo; solo las jugadas al terminar; aviso si hay extra innings.
- `POST /prediccion` se conecta al servicio Python con `PREDICCION_URL`.
- La tabla de posiciones se calcula con los partidos `FINISHED` (no se guarda aparte).
