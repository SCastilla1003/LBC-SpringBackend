#!/usr/bin/env bash
# Pruebas de humo de la API (local o Railway).
# Uso: bash scripts/probar-api.sh http://localhost:8080 [usuario-admin] [contraseña-admin]
# Crea y luego BORRA una temporada de prueba "Temporada de prueba API" (año 2030).
B="${1:-http://localhost:8080}"
ADMIN_USER="${2:-adminlbc}"
ADMIN_PASS="${3:-adminlbc2025}"
A="$B/api/v1"
j() { python -I -c "import sys,json; d=json.load(sys.stdin); print(eval(sys.argv[1]))" "$1"; }
st() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

echo "== health: $(curl -s $B/actuator/health)"
T=$(curl -s $A/temporadas)
echo "== temporadas: $(echo "$T" | j "[(t['nombreTemporada'],t['anio'],t['totalEquipos'],t['partidosFinalizados']) for t in d]")"
ID=$(echo "$T" | j "([t['id'] for t in d if t['partidosFinalizados']>0] or [t['id'] for t in d])[0]")
echo "== actual: $(curl -s $A/temporadas/actual | j "(d['anio'], len(d['equipos']), d['totalAgentesLibres'])")"
echo "== posiciones: $(curl -s $A/temporadas/$ID/posiciones | j "[(p['posicion'],p['nombre'],p['ganados'],p['perdidos'],p['porcentajeVictorias'],p['juegosDetras'],p['racha']) for p in d]")"
EQ=$(curl -s $A/temporadas/$ID/equipos | j "d[0]['id']")
echo "== estadisticas equipo: $(curl -s $A/temporadas/$ID/equipos/$EQ/estadisticas | j "([(p['rival']['nombre'],p['resultado'],p['diferencial']) for p in d['partidos']], [(c['metrica'],c['valorEquipo'],c['promedioLiga']) for c in d['comparativaLiga']][:4])")"
echo "== lideres HR: $(curl -s "$A/temporadas/$ID/lideres?tipo=bateo&estadistica=homeRuns&limite=3" | j "[(l['posicion'],l['nombre'],l['valor']) for l in d['lideres']]")"
echo "== lideres ERA: $(curl -s "$A/temporadas/$ID/lideres?tipo=pitcheo&estadistica=era" | j "(d['menorEsMejor'], [(l['nombre'],l['valor']) for l in d['lideres']])")"
echo "== lider invalido: $(curl -s "$A/temporadas/$ID/lideres?estadistica=avg" | j "(d['codigo'], d['mensaje'])")"
BAT=$(curl -s $A/temporadas/$ID/bateadores | j "d[0]['id']")
echo "== perfil bateador: $(curl -s $A/temporadas/$ID/bateadores/$BAT | j "(d['jugador']['nombre'], d['jugador']['hits'], d['jugador']['totalBases'], d['referenciaLiga']['homeRuns'])")"
LAN=$(curl -s $A/temporadas/$ID/lanzadores | j "d[0]['id']")
echo "== perfil lanzador: $(curl -s $A/temporadas/$ID/lanzadores/$LAN | j "(d['jugador']['era'], d['jugador']['whip'], d['jugador']['k9'], d['jugador']['kbb'])")"
echo "== agentes libres: $(curl -s $A/temporadas/$ID/agentes-libres | j "[b['nombre'] for b in d['bateadores']]")"
echo "== partidos: $(curl -s $A/temporadas/$ID/partidos | j "[(p['equipoLocal']['nombre'],p['carrerasEquipoLocal'],p['equipoVisitante']['nombre'],p['carrerasEquipoVisitante'],p['estado'],p['fecha'],p['hora'],p['estadio']['nombre']) for p in d]")"
echo "== estadisticas temporada: $(curl -s $A/temporadas/$ID/estadisticas | j "(d['kpis'], d['distribucionHits'])")"
echo "== prediccion opciones: $(curl -s $A/prediccion/opciones | j "(d['rivalFuerza'], d['rangos'])")"
echo "== prediccion: $(curl -s -X POST $A/prediccion -H 'Content-Type: application/json' -d '{"localVisitante":"Local","rachaUlt3":2,"promCarrerasFavor":5.1,"promCarrerasContra":3.2,"rivalFuerza":"Media"}' | j "(d['clase'], d['confianza'], d['probabilidades'], d['resumen'])")"
echo "== prediccion invalida: $(curl -s -X POST $A/prediccion -H 'Content-Type: application/json' -d '{"localVisitante":"Casa","rachaUlt3":5}' | j "(d['codigo'], d['detalles'])")"
echo "== 404 temporada: $(curl -s $A/temporadas/noexiste | j "(d['codigo'], d['detalles'])")"
echo "== 404 ruta: $(curl -s $A/noexiste)"

echo "--- seguridad ---"
echo "POST sin token: $(curl -s -X POST $A/temporadas -H 'Content-Type: application/json' -d '{"nombreTemporada":"X","anio":2027}')"
echo "token invalido: $(st -X POST $A/temporadas -H 'Authorization: Bearer abc' -H 'Content-Type: application/json' -d '{}')"
echo "login malo: $(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' -d '{"username":"adminlbc","password":"mala"}')"
ADMIN=$(curl -s -X POST $A/auth/login -H 'Content-Type: application/json' -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASS\"}" | j "d['token']")
echo "yo admin: $(curl -s $A/auth/yo -H "Authorization: Bearer $ADMIN")"
U="usuario$RANDOM"
USR=$(curl -s -X POST $A/auth/registro -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"secreta123\"}" | j "d['token']")
echo "registro duplicado: $(curl -s -X POST $A/auth/registro -H 'Content-Type: application/json' -d "{\"username\":\"$U\",\"password\":\"secreta123\"}" | j "d['codigo']")"
echo "USER escribe (403): $(curl -s -X POST $A/temporadas -H "Authorization: Bearer $USR" -H 'Content-Type: application/json' -d '{"nombreTemporada":"X","anio":2027}')"
echo "google sin configurar: $(curl -s -X POST $A/auth/google -H 'Content-Type: application/json' -d '{"idToken":"x"}' | j "(d['codigo'])")"

echo "--- CRUD como ADMIN ---"
H=(-H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json')
NT=$(curl -s -X POST $A/temporadas "${H[@]}" -d '{"nombreTemporada":"Temporada de prueba API","anio":2030}' | j "d['id']")
E1=$(curl -s -X POST $A/temporadas/$NT/equipos "${H[@]}" -d '{"nombre":"Tigres","ciudad":"Cartagena"}' | j "d['id']")
E2=$(curl -s -X POST $A/temporadas/$NT/equipos "${H[@]}" -d '{"nombre":"Leones","ciudad":"Turbaco"}' | j "d['id']")
echo "equipo duplicado: $(curl -s -X POST $A/temporadas/$NT/equipos "${H[@]}" -d '{"nombre":"tigres"}' | j "d['codigo']")"
echo "nombre reservado: $(curl -s -X POST $A/temporadas/$NT/equipos "${H[@]}" -d '{"nombre":"Agentes Libres"}' | j "d['mensaje']")"
S1=$(curl -s -X POST $A/temporadas/$NT/estadios "${H[@]}" -d '{"nombre":"Once de Noviembre","ubicacion":"Cartagena"}' | j "d['id']")
P1=$(curl -s -X POST $A/temporadas/$NT/partidos "${H[@]}" -d "{\"fecha\":\"2030-03-01\",\"hora\":\"19:00\",\"estado\":\"FINALIZADO\",\"idEstadio\":\"$S1\",\"idEquipoLocal\":\"$E1\",\"idEquipoVisitante\":\"$E2\",\"carrerasEquipoLocal\":3,\"carrerasEquipoVisitante\":5,\"hitTotalesPartido\":12,\"erroresTotalesPartido\":1}" | j "d['id']")
echo "partido mismo equipo: $(curl -s -X POST $A/temporadas/$NT/partidos "${H[@]}" -d "{\"fecha\":\"2030-03-02\",\"idEquipoLocal\":\"$E1\",\"idEquipoVisitante\":\"$E1\"}" | j "d['mensaje']")"
echo "validacion partido: $(curl -s -X POST $A/temporadas/$NT/partidos "${H[@]}" -d '{}' | j "d['detalles']")"
B1=$(curl -s -X POST $A/temporadas/$NT/equipos/$E1/bateadores "${H[@]}" -d '{"nombre":"Juan","edad":20,"posicion":"SS","derechoZurdo":"Derecho","homeRuns":3,"sencillos":10,"dobles":2,"ponches":4,"basePorBola":6}' | j "d['id']")
echo "editar bateador: $(curl -s -X PUT $A/temporadas/$NT/bateadores/$B1 "${H[@]}" -d '{"nombre":"Juan Pérez","edad":21,"homeRuns":4}' | j "(d['nombre'],d['homeRuns'],d['hits'])")"
echo "traspaso: $(curl -s -X POST $A/temporadas/$NT/jugadores/$B1/traspaso "${H[@]}" -d "{\"idEquipoDestino\":\"$E2\"}" | j "(d['tipo'], d['bateador']['nombreEquipo'])")"
echo "renombrar equipo: $(curl -s -X PUT $A/temporadas/$NT/equipos/$E2 "${H[@]}" -d '{"nombre":"Leones de Turbaco","ciudad":"Turbaco"}' | j "d['nombre']") -> partido: $(curl -s $A/temporadas/$NT/partidos/$P1 | j "(d['equipoVisitante']['nombre'], d['idEquipoGanador']=='$E2')")"
echo "posiciones nueva: $(curl -s $A/temporadas/$NT/posiciones | j "[(p['nombre'],p['ganados'],p['perdidos'],p['diferencial']) for p in d]")"
echo "borrar equipo con partidos: $(curl -s -X DELETE $A/temporadas/$NT/equipos/$E1 -H "Authorization: Bearer $ADMIN" | j "d['codigo']")"
printf '\x89PNG\r\n\x1a\n0000000000' > foto-prueba.png
F=foto-prueba.png
FOTO=$(curl -s -X PUT $A/temporadas/$NT/equipos/$E1/foto -H "Authorization: Bearer $ADMIN" -F "archivo=@$F" | j "d['urlFoto']")
echo "foto subida: $FOTO -> GET $(curl -s -o /dev/null -w '%{http_code} %{content_type}' $B$FOTO) ; equipo urlFoto=$(curl -s $A/temporadas/$NT/equipos/$E1 | j "d['urlFoto']")"
echo "304 con ETag: $(st $B$FOTO -H "If-None-Match: \"${FOTO##*/}\"")"
printf 'hola' > texto.txt
echo "foto no imagen: $(curl -s -X PUT $A/temporadas/$NT/equipos/$E1/foto -H "Authorization: Bearer $ADMIN" -F 'archivo=@texto.txt' | j "d['codigo']")"
rm -f texto.txt foto-prueba.png
echo "campeon: $(curl -s -X PUT $A/temporadas/$NT "${H[@]}" -d "{\"nombreTemporada\":\"Temporada de prueba API\",\"anio\":2030,\"idEquipoCampeon\":\"$E2\"}" | j "d['campeon']['nombre']")"
echo "borrar temporada: $(st -X DELETE $A/temporadas/$NT -H "Authorization: Bearer $ADMIN") ; foto tras borrar: $(st $B$FOTO)"

echo "--- CORS ---"
echo "preflight permitido:"; curl -s -i -X OPTIONS $A/temporadas -H 'Origin: http://localhost:53211' -H 'Access-Control-Request-Method: POST' -H 'Access-Control-Request-Headers: authorization,content-type' | grep -iE "^HTTP|^access-control"
echo "preflight origen no permitido: $(st -X OPTIONS $A/temporadas -H 'Origin: https://malicioso.example' -H 'Access-Control-Request-Method: GET')"
echo "GET con Origin: $(curl -s -i $A/temporadas -H 'Origin: http://localhost:53211' | grep -i '^access-control-allow-origin')"
