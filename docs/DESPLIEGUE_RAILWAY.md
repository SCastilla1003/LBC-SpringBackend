# Despliegue de la API LBC en Railway

Arquitectura final: **un servicio MongoDB** (datos + fotos en GridFS) y **un servicio con la API**
(imagen Docker construida desde este repositorio). La app Flutter solo habla con la URL pública de la API.

> El volcado `img_lbc.sql` ya no se usa: no contenía fotos y la API no usa MySQL.

---

## 1. Crear el proyecto y la base de datos

1. Entra a <https://railway.com>, **New Project → Database → Add MongoDB**.
2. Abre el servicio **MongoDB → Variables**. Railway crea, entre otras:
   - `MONGO_URL`: conexión **interna** (la usará la API, no sale a internet).
   - `MONGO_PUBLIC_URL`: conexión **pública** (para conectarte desde tu PC con Compass).

## 2. Importar los datos existentes (MongoDB Compass)

1. Copia `MONGO_PUBLIC_URL` y en Compass: **New connection → pega la URL → Connect**.
2. Crea la base `db-ligabeisbolcartagena` con la colección `temporadas`, y añade también la colección `usuarios`.
3. En cada colección: **Add data → Import JSON or CSV file** y elige el archivo de
   `BASES DE DATOS/MONGODB/db-ligabeisbolcartagena/`:
   - `temporadas` ← `db-ligabeisbolcartagena.temporadas.json`
   - `usuarios` ← `db-ligabeisbolcartagena.usuarios.json`

Al arrancar, la API migra sola estos datos (ids, enlaces de partidos, récords). Es idempotente.

> **Usuarios:** el volcado trae cuentas de prueba (`Camilo`, `Santiago`, `a`, `prueba`) con sus contraseñas
> de desarrollo. Si la API va a ser pública y no las necesitas, importa solo `temporadas`: el administrador
> se crea solo con `ADMIN_PASSWORD`. Si importas `adminlbc`, su contraseña se reemplaza por `ADMIN_PASSWORD`.

Alternativa por consola (requiere [MongoDB Database Tools](https://www.mongodb.com/try/download/database-tools)):

```bash
mongoimport --uri "<MONGO_PUBLIC_URL>" --db db-ligabeisbolcartagena --collection temporadas --jsonArray --file "db-ligabeisbolcartagena.temporadas.json"
```

## 3. Subir el código de la API

El repositorio git es la carpeta `proaula-lbc-main` (ya tiene `Dockerfile` y `railway.json`).

**Opción A: desde GitHub (despliega solo en cada push).** Crea un repositorio **privado** vacío en GitHub y:

```bash
git remote add origin https://github.com/<tu-usuario>/lbc-api.git
```

```bash
git push -u origin main
```

En Railway: **New → GitHub Repo →** elige `lbc-api`.

**Opción B: con la CLI de Railway (sin GitHub).** Desde la carpeta `proaula-lbc-main`:

```bash
npm i -g @railway/cli
```

```bash
railway login
```

```bash
railway link
```

```bash
railway up
```

Railway detecta `railway.json`: construye con el `Dockerfile` y usa `/actuator/health` como healthcheck.

## 4. Variables de entorno del servicio de la API

En el servicio de la API → **Variables** (usa *Raw Editor* para pegarlas juntas):

| Variable | Valor | Obligatoria |
|---|---|---|
| `MONGO_URL` | `${{MongoDB.MONGO_URL}}` (referencia al servicio MongoDB; ajusta el nombre si lo renombraste) | Sí |
| `MONGO_DATABASE` | `db-ligabeisbolcartagena` | No (es el valor por defecto) |
| `JWT_SECRET` | cadena aleatoria de 32+ caracteres (ver abajo) | Sí |
| `ADMIN_PASSWORD` | contraseña fuerte para `adminlbc` | Sí |
| `CORS_ALLOWED_ORIGINS` | orígenes de Flutter Web separados por comas, p. ej. `http://localhost:*` mientras desarrollas y luego el dominio donde publiques la web | Sí para Flutter Web |
| `PORT` | `8080` | Recomendado (fija el puerto del dominio público) |
| `GOOGLE_CLIENT_IDS` | client IDs de Google (web y Android) separados por comas | Solo para login con Google |
| `JWT_EXPIRACION` | duración ISO-8601, p. ej. `PT24H` | No |
| `ADMIN_USERNAME` | `adminlbc` | No |
| `LBC_ZONA_HORARIA` | `America/Bogota` | No |

Generar un `JWT_SECRET`:

```bash
python -c "import secrets; print(secrets.token_urlsafe(48))"
```

**No** pongas estos valores en el código ni en git. `GOOGLE_CLIENT_SECRET` ya no hace falta.

## 5. Dominio público

Servicio de la API → **Settings → Networking → Generate Domain** (puerto `8080`).
Obtendrás `https://<app>.up.railway.app`: esa es la `API_BASE_URL` de Flutter.

## 6. Comprobar el despliegue

En **Deployments → View logs** deben aparecer:

```
Modelo Weka J48 cargado (400 instancias de entrenamiento).
CORS: orígenes permitidos [...]
Started MainApplication in ... seconds
Migración de datos terminada: N temporada(s) actualizada(s).
```

y el healthcheck en verde.

## 7. Cargar la temporada Demo (datos ficticios)

Desde la carpeta `proaula-lbc-main` (pide la contraseña del admin; no queda guardada):

```bash
python scripts/cargar_temporada_demo.py --api https://<app>.up.railway.app
```

Crea "Temporada Demo (datos ficticios)" con 6 equipos, 60 jugadores, 3 estadios y 30 partidos
(24 jugados, 6 programados). `--reemplazar` la borra y la vuelve a crear; `--semilla N` genera otros datos.

## 8. Verificación con curl

Sustituye `URL` por tu dominio. En Git Bash:

```bash
URL=https://<app>.up.railway.app
```

Salud y endpoints públicos:

```bash
curl -s $URL/actuator/health
```

```bash
curl -s $URL/api/v1/temporadas
```

```bash
curl -s $URL/api/v1/temporadas/actual
```

Escritura sin token → debe responder **401** `NO_AUTENTICADO`:

```bash
curl -s -X POST $URL/api/v1/temporadas -H "Content-Type: application/json" -d '{"nombreTemporada":"X","anio":2030}'
```

Login (escribe tu contraseña en lugar de `<ADMIN_PASSWORD>`) y endpoint protegido con token → **201**, y luego bórrala:

```bash
TOKEN=$(curl -s -X POST $URL/api/v1/auth/login -H "Content-Type: application/json" -d '{"username":"adminlbc","password":"<ADMIN_PASSWORD>"}' | python -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

```bash
ID=$(curl -s -X POST $URL/api/v1/temporadas -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"nombreTemporada":"Prueba curl","anio":2030}' | python -c "import sys,json; print(json.load(sys.stdin)['id'])")
```

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X DELETE $URL/api/v1/temporadas/$ID -H "Authorization: Bearer $TOKEN"
```

CORS preflight desde un origen permitido → **200** con `Access-Control-Allow-Origin`:

```bash
curl -s -i -X OPTIONS $URL/api/v1/temporadas -H "Origin: http://localhost:5000" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: authorization,content-type"
```

Desde un origen no permitido → **403**:

```bash
curl -s -o /dev/null -w "%{http_code}\n" -X OPTIONS $URL/api/v1/temporadas -H "Origin: https://otro-sitio.example" -H "Access-Control-Request-Method: GET"
```

Prueba completa (todos los recursos, errores, fotos y CORS; crea y borra una temporada de prueba):

```bash
bash scripts/probar-api.sh $URL adminlbc '<ADMIN_PASSWORD>'
```

## Problemas frecuentes

| Síntoma | Causa probable |
|---|---|
| El despliegue falla con `Could not resolve placeholder 'MONGO_URL'` o `'JWT_SECRET'` | Falta la variable en el servicio de la API |
| `JWT_SECRET debe tener al menos 32 caracteres` | Secreto demasiado corto |
| Healthcheck en rojo y `Timed out ... mongodb` en los logs | `MONGO_URL` no apunta al servicio MongoDB del mismo proyecto |
| Flutter Web: error de CORS / preflight 403 | El origen exacto de la web no está en `CORS_ALLOWED_ORIGINS` |
| 401 en un `GET` público | La app envía un token caducado: debe borrarlo y reintentar |
| Login con Google responde 503 | Falta `GOOGLE_CLIENT_IDS` |
