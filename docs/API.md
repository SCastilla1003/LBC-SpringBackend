# API REST LBC (`/api/v1`)

Contrato que consume la app Flutter. Todas las respuestas son JSON (excepto las imágenes).
Fechas `"2025-05-28"`, horas `"09:30:00"` (se aceptan `"09:30"`), instantes ISO-8601 UTC.

## Autenticación

| Acceso | Rutas |
|---|---|
| Público | todos los `GET /api/v1/**`, `POST /auth/login`, `POST /auth/registro`, `POST /auth/google`, `POST /prediccion` |
| Con token | `GET /auth/yo` |
| Rol `ADMIN` | cualquier otro `POST`/`PUT`/`DELETE` |

Cabecera: `Authorization: Bearer <token>`. El token caduca según `JWT_EXPIRACION` (24 h por defecto);
con un token caducado o inválido **cualquier** ruta responde 401 (incluso las públicas): la app debe
borrar el token y reintentar sin él.

| Método y ruta | Cuerpo | Respuesta |
|---|---|---|
| `POST /auth/login` | `{username, password}` | `AuthResponse` |
| `POST /auth/registro` | `{username (3-50), password (6-100)}` | `201 AuthResponse` (rol `USER`) |
| `POST /auth/google` | `{idToken}` (de `google_sign_in`) | `AuthResponse` |
| `GET /auth/yo` | — | `{username, roles, proveedor}` |

`AuthResponse = {token, tipo:"Bearer", expiraEn, usuario:{username, roles:["ADMIN"|"USER"], proveedor:"local"|"google"}}`

## Errores

Siempre `{ "codigo": "...", "mensaje": "texto para el usuario", "detalles": { ... } }`.

| Status | codigo | Cuándo |
|---|---|---|
| 400 | `VALIDACION` | campos no válidos; `detalles` = `{campo: mensaje}` |
| 400 | `SOLICITUD_INVALIDA` | JSON mal formado, parámetro inválido, regla de negocio |
| 401 | `NO_AUTENTICADO` / `CREDENCIALES_INVALIDAS` | sin token, token inválido, login fallido |
| 403 | `ACCESO_DENEGADO` | el usuario no es ADMIN |
| 404 | `NO_ENCONTRADO` | `detalles = {recurso, id}` o `{ruta}` |
| 409 | `CONFLICTO` | nombre repetido, borrar algo en uso, edición concurrente |
| 413 | `ARCHIVO_DEMASIADO_GRANDE` | foto > 10 MB |
| 415 | `TIPO_NO_SOPORTADO` | foto que no es PNG/JPEG/WebP |
| 503 | `SERVICIO_NO_DISPONIBLE` | login con Google sin configurar |
| 500 | `ERROR_INTERNO` | error inesperado |

## Recursos (`{t}` = id de temporada)

Los `PUT` reemplazan el recurso completo (enviar todos los campos del formulario).

### Temporadas
- `GET /temporadas` → `[TemporadaResumen]` (de la más reciente a la más antigua)
- `GET /temporadas/actual` → `TemporadaDetalle` (mayor año)
- `GET /temporadas/{t}` → `TemporadaDetalle` (equipos con récord, estadios)
- `POST /temporadas`, `PUT /temporadas/{t}` → `{nombreTemporada, anio, idEquipoCampeon?}`
- `DELETE /temporadas/{t}` → 204 (borra también sus fotos)

### Equipos (sin "Agentes Libres")
- `GET /temporadas/{t}/equipos` → `[Equipo]` con `registro` = `{partidosJugados, ganados, perdidos, empates, porcentajeVictorias, carrerasFavor, carrerasContra, diferencial}`
- `GET /temporadas/{t}/equipos/{e}` → `EquipoDetalle` (con bateadores y lanzadores)
- `GET /temporadas/{t}/equipos/{e}/estadisticas` → `{equipo, partidos:[{idPartido, fecha, hora, local, rival, carrerasFavor, carrerasContra, diferencial, resultado:"G"|"P"|"E"}], comparativaLiga:[{metrica, etiqueta, valorEquipo, promedioLiga, mayorEsMejor}], bateo, pitcheo}`
- `GET /temporadas/{t}/posiciones` → `[{posicion, idEquipo, nombre, ..., porcentajeVictorias, juegosDetras, ultimosResultados, racha}]`
- `POST`/`PUT /temporadas/{t}/equipos[/{e}]` → `{nombre, ciudad?, color?}` · `DELETE` → 204 (409 si tiene partidos)
- `PUT /temporadas/{t}/equipos/{e}/foto` (multipart, campo `archivo`) → `{fotoId, urlFoto}` · `DELETE .../foto` → 204

**Color de equipo** (`color` en Equipo, EquipoDetalle, Posicion y en las referencias `equipoLocal`/`equipoVisitante`/`rival`): clave de la paleta `azul, naranja, aguamarina, amarillo, magenta, verde, violeta, rojo`. Se asigna sola al crear el equipo (primera libre); si en un `PUT` se omite, se conserva. Dos equipos de la misma temporada no pueden compartir color (409). Con más de 8 equipos, los sobrantes quedan con `null`.

El récord se **calcula** con los partidos `FINALIZADO`; no se edita a mano.

### Jugadores
- `GET|POST /temporadas/{t}/equipos/{e}/bateadores` · `GET|POST /temporadas/{t}/equipos/{e}/lanzadores`
- `GET /temporadas/{t}/bateadores` · `GET /temporadas/{t}/lanzadores` (todos, incluye agentes libres con `agenteLibre:true`)
- `GET /temporadas/{t}/bateadores/{j}` → `{jugador: Bateador, referenciaLiga: {metrica: {promedio, minimo, maximo, muestras}}}`
- `GET /temporadas/{t}/lanzadores/{j}` → `{jugador: Lanzador, referenciaLiga}`
- `PUT|DELETE /temporadas/{t}/bateadores/{j}` y `/lanzadores/{j}` · `PUT|DELETE .../{j}/foto`
- `GET /temporadas/{t}/agentes-libres` → `{idEquipo, bateadores, lanzadores}`
- `POST /temporadas/{t}/agentes-libres/bateadores` y `/lanzadores`
- `POST /temporadas/{t}/jugadores/{j}/traspaso` → `{idEquipoDestino}` (un equipo o el id de Agentes Libres)
- `GET /temporadas/{t}/lideres?tipo=bateo|pitcheo&estadistica=homeRuns&limite=10` → `{tipo, estadistica, menorEsMejor, lideres:[{posicion, idJugador, nombre, idEquipo, nombreEquipo, urlFoto, valor}], estadisticasDisponibles}`

Bateador: campos del modelo + `hits, extrabases, totalBases, vecesEmbasado, relacionBbK`.
Lanzador: campos del modelo + `era, whip, k9, bb9, kbb, lanzamientosPorInning` (null con 0 innings).
No existen turnos al bate: no hay AVG/OBP/SLG. Referencias y líderes excluyen a los agentes libres.

Estadísticas de líderes — bateo: `homeRuns, carrerasImpulsadas, hits, extrabases, totalBases, sencillos, dobles, triples, roboDeBase, basePorBola, basePorGolpe, vecesEmbasado, ponches, errores, relacionBbK`; pitcheo: `ponchados, inningsLanzados, numeroLanzamientos, aparicionesPlato, basePorBola, carrerasPermitidas, hitsPermitidos, era, whip, k9, bb9, kbb, lanzamientosPorInning`.

### Partidos
- `GET /temporadas/{t}/partidos?estado=PROGRAMADO|EN_JUEGO|FINALIZADO&idEquipo=` (orden cronológico)
- `GET|PUT|DELETE /temporadas/{t}/partidos/{p}` · `POST /temporadas/{t}/partidos`
- Cuerpo: `{fecha, hora?, estado?, idEstadio?, idEquipoLocal, idEquipoVisitante, carrerasEquipoLocal, carrerasEquipoVisitante, innings?, inningActual, hitTotalesPartido, erroresTotalesPartido}`
- Respuesta: `{id, fecha, hora, estado, estadio, equipoLocal{id,nombre,urlFoto}, equipoVisitante, ..., idEquipoGanador}`

### Estadios
- `GET|POST /temporadas/{t}/estadios` · `GET|PUT|DELETE /temporadas/{t}/estadios/{s}` → `{id, nombre, ubicacion, partidosJugados, partidosProgramados}`

### Estadísticas de temporada
- `GET /temporadas/{t}/estadisticas` → `{kpis:{partidosJugados, partidosPendientes, carrerasTotales, promedioCarrerasPorPartido, totalEquipos, totalJugadores, liderJonrones, mejorRecord}, carrerasPorPartido:[...], victoriasPorEquipo:[...], distribucionHits:{sencillos, dobles, triples, homeRuns, total}, partidosPorEstadio:[...], referenciaBateo, referenciaPitcheo}`

### Predicción (Weka J48)
- `GET /prediccion/opciones` → `{localVisitante, rivalFuerza, rangos:{rachaUlt3, promCarrerasFavor, promCarrerasContra: {minimo, maximo, promedio}}, instanciasEntrenamiento}`
- `POST /prediccion` → cuerpo `{localVisitante:"Local"|"Visitante", rachaUlt3:0-3, promCarrerasFavor, promCarrerasContra, rivalFuerza:"Baja"|"Media"|"Alta"}` → `{clase:"Si"|"No", gana, confianza (0-100), probabilidades:{Si, No}, entradas, resumen}`

### Imágenes
- `GET /imagenes/{fotoId}` → bytes con su `Content-Type`, `ETag` y caché de 1 año.
  `urlFoto` en los DTO es **relativa**: la app la concatena a `API_BASE_URL`. `urlFoto: null` = sin foto.

## Salud
- `GET /actuator/health` → `{"status":"UP"}` (público, para el healthcheck de Railway).

## Verificación rápida
```bash
bash scripts/probar-api.sh http://localhost:8080
```
Crea y luego borra una temporada de prueba; muestra cada respuesta para revisarla.
