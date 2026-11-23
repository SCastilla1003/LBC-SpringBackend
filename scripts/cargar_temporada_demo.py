#!/usr/bin/env python3
"""
Carga una temporada de DEMOSTRACIÓN con datos FICTICIOS en la API LBC (local o Railway).

Los equipos, jugadores, estadios y resultados son inventados; sirven para que las
gráficas de la app tengan datos con los que verse. La temporada se llama
"Temporada Demo (datos ficticios)" para que nunca se confunda con datos reales.

Todo pasa por la API (no toca la base de datos directamente), así que también
prueba los endpoints de escritura. Solo usa la librería estándar de Python.

Uso:
    python scripts/cargar_temporada_demo.py --api https://<app>.up.railway.app
    python scripts/cargar_temporada_demo.py --api http://localhost:8080 --reemplazar

La contraseña del administrador se toma de la variable LBC_ADMIN_PASSWORD o se pide
por teclado (no se escribe en el código ni en la línea de comandos).
"""
import argparse
import getpass
import json
import math
import os
import random
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

NOMBRE_TEMPORADA = "Temporada Demo (datos ficticios)"

EQUIPOS = [
    ("Pelícanos de Bocagrande", "Cartagena"),
    ("Iguanas de Getsemaní", "Cartagena"),
    ("Alcatraces de Manga", "Cartagena"),
    ("Faroleros del Cabrero", "Cartagena"),
    ("Brisas de La Boquilla", "Cartagena"),
    ("Corsarios de Turbaco", "Turbaco"),
]
ESTADIOS = [
    ("Parque de Pelota El Laguito", "Cartagena"),
    ("Diamante Las Murallas", "Cartagena"),
    ("Campo Los Corales", "Turbaco"),
]
NOMBRES = ["Andrés", "Carlos", "Daniel", "Esteban", "Felipe", "Gabriel", "Héctor", "Iván", "Jorge", "Kevin",
           "Luis", "Mateo", "Nicolás", "Óscar", "Pedro", "Rafael", "Samuel", "Tomás", "Víctor", "Yeison",
           "Juan", "Miguel", "Sebastián", "Julián", "Camilo", "David", "Brayan", "Elkin", "Wilmer", "Jhon"]
APELLIDOS = ["Arrieta", "Barrios", "Castillo", "Díaz", "Escorcia", "Fuentes", "Gómez", "Herrera", "Julio",
             "Lambis", "Marimón", "Navarro", "Orozco", "Pájaro", "Quintana", "Rodríguez", "Simancas", "Torres",
             "Urueta", "Villalba", "Zúñiga", "Cassiani", "Teherán", "Puello", "Caraballo", "Montes", "Arroyo",
             "Blanco", "Cabarcas", "Salgado"]
POSICIONES_BATEO = ["C", "1B", "2B", "3B", "SS", "LF", "CF", "RF", "DH", "UT"]
HORAS = ["15:00", "18:00", "19:30"]


# ---------------------------------------------------------------- cliente HTTP

class ErrorApi(Exception):
    pass


class Api:
    def __init__(self, base):
        self.base = base.rstrip("/") + "/api/v1"
        self.token = None

    def pedir(self, metodo, ruta, cuerpo=None):
        datos = json.dumps(cuerpo).encode("utf-8") if cuerpo is not None else None
        req = urllib.request.Request(self.base + ruta, data=datos, method=metodo)
        req.add_header("Accept", "application/json")
        if datos is not None:
            req.add_header("Content-Type", "application/json; charset=utf-8")
        if self.token:
            req.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                texto = r.read().decode("utf-8")
                return json.loads(texto) if texto else None
        except urllib.error.HTTPError as e:
            try:
                err = json.loads(e.read().decode("utf-8"))
                detalle = f"{err.get('codigo')}: {err.get('mensaje')} {err.get('detalles') or ''}"
            except Exception:
                detalle = str(e)
            raise ErrorApi(f"{metodo} {ruta} -> HTTP {e.code} {detalle}") from None
        except urllib.error.URLError as e:
            raise ErrorApi(f"No se pudo conectar con {self.base}: {e.reason}") from None


# ---------------------------------------------------------------- generación de datos

def poisson(rng, media):
    """Muestra de una distribución de Poisson (algoritmo de Knuth)."""
    limite, k, p = math.exp(-media), 0, 1.0
    while True:
        p *= rng.random()
        if p <= limite:
            return k
        k += 1


def repartir(rng, total, pesos):
    """Reparte 'total' unidades enteras de forma aleatoria según 'pesos'."""
    resultado = [0] * len(pesos)
    for _ in range(max(0, int(round(total)))):
        resultado[rng.choices(range(len(pesos)), weights=pesos)[0]] += 1
    return resultado


def calendario(n):
    """Todos contra todos a dos vueltas (método del círculo): lista de jornadas con pares (local, visitante)."""
    equipos = list(range(n))
    jornadas = []
    for r in range(n - 1):
        jornada = []
        for i in range(n // 2):
            a, b = equipos[i], equipos[n - 1 - i]
            jornada.append((a, b) if r % 2 == 0 else (b, a))
        jornadas.append(jornada)
        equipos = [equipos[0]] + [equipos[-1]] + equipos[1:-1]
    vuelta = [[(v, l) for (l, v) in j] for j in jornadas]
    return jornadas + vuelta


def generar(rng, anio, jornadas_pendientes):
    nombres_usados = set()

    def nombre_jugador():
        while True:
            n = f"{rng.choice(NOMBRES)} {rng.choice(APELLIDOS)}"
            if n not in nombres_usados:
                nombres_usados.add(n)
                return n

    fuerza = [rng.uniform(0.8, 1.25) for _ in EQUIPOS]
    jornadas = calendario(len(EQUIPOS))
    inicio = date(anio, 2, 7)
    partidos = []
    stats = [dict(jugados=0, carreras=0, contra=0, hits=0, hits_contra=0, errores=0, innings=0) for _ in EQUIPOS]

    for nj, jornada in enumerate(jornadas):
        fecha = inicio + timedelta(days=4 * nj)
        pendiente = nj >= len(jornadas) - jornadas_pendientes
        for k, (l, v) in enumerate(jornada):
            p = dict(fecha=fecha.isoformat(), hora=HORAS[k % len(HORAS)], local=l, visitante=v,
                     estadio=(nj + k) % len(ESTADIOS))
            if pendiente:
                p.update(estado="PROGRAMADO", cl=0, cv=0, innings=9, actual=0, hits=0, errores=0)
            else:
                cl = poisson(rng, 4.4 * fuerza[l] / math.sqrt(fuerza[v]) * 1.05)
                cv = poisson(rng, 4.4 * fuerza[v] / math.sqrt(fuerza[l]))
                innings = 9
                while cl == cv:  # en béisbol no hay empates: entradas extra
                    innings += 1
                    if rng.random() < 0.5:
                        cl += 1
                    else:
                        cv += 1
                hl = max(cl, poisson(rng, cl + 3.5))
                hv = max(cv, poisson(rng, cv + 3.5))
                el, ev = poisson(rng, 0.8), poisson(rng, 0.8)
                p.update(estado="FINALIZADO", cl=cl, cv=cv, innings=innings, actual=innings,
                         hits=hl + hv, errores=el + ev)
                for eq, a_favor, en_contra, h, h_contra, err in ((l, cl, cv, hl, hv, el), (v, cv, cl, hv, hl, ev)):
                    s = stats[eq]
                    s["jugados"] += 1
                    s["carreras"] += a_favor
                    s["contra"] += en_contra
                    s["hits"] += h
                    s["hits_contra"] += h_contra
                    s["errores"] += err
                    s["innings"] += innings
            partidos.append(p)

    equipos = []
    for i, (nombre, ciudad) in enumerate(EQUIPOS):
        s = stats[i]
        equipos.append(dict(nombre=nombre, ciudad=ciudad,
                            bateadores=generar_bateadores(rng, s, nombre_jugador),
                            lanzadores=generar_lanzadores(rng, s, nombre_jugador)))

    libres_b = [bateador_libre(rng, nombre_jugador()) for _ in range(2)]
    libres_l = [lanzador_libre(rng, nombre_jugador())]
    return equipos, partidos, libres_b, libres_l


def generar_bateadores(rng, s, nombre_jugador):
    n = len(POSICIONES_BATEO)
    turnos = [rng.uniform(0.9, 1.3) if i < 9 else rng.uniform(0.3, 0.6) for i in range(n)]
    poder = [rng.uniform(0.4, 2.2) for _ in range(n)]
    velocidad = [rng.uniform(0.2, 2.0) for _ in range(n)]
    hits = repartir(rng, s["hits"], turnos)
    impulsadas = repartir(rng, s["carreras"] * 0.92, [t * p for t, p in zip(turnos, poder)])
    bb = repartir(rng, s["jugados"] * 3.2, turnos)
    ponches = repartir(rng, s["jugados"] * 7.0, [t * (0.6 + p / 2) for t, p in zip(turnos, poder)])
    robos = repartir(rng, s["jugados"] * 0.9, [t * v for t, v in zip(turnos, velocidad)])
    golpeados = repartir(rng, s["jugados"] * 0.35, turnos)
    errores = repartir(rng, s["errores"], [1.6 if pos in ("SS", "3B", "2B") else 1.0 for pos in POSICIONES_BATEO])
    lista = []
    for i in range(n):
        tipos = repartir(rng, hits[i], [0.70, 0.19, 0.03 * velocidad[i], 0.08 * poder[i]])
        lista.append(dict(
            nombre=nombre_jugador(), edad=rng.randint(18, 36), posicion=POSICIONES_BATEO[i],
            derechoZurdo=rng.choices(["Derecho", "Zurdo", "Ambidiestro"], weights=[70, 25, 5])[0],
            sencillos=tipos[0], dobles=tipos[1], triples=tipos[2], homeRuns=tipos[3],
            basePorBola=bb[i], basePorGolpe=golpeados[i], ponches=ponches[i], roboDeBase=robos[i],
            carrerasImpulsadas=impulsadas[i], errores=errores[i]))
    return lista


def generar_lanzadores(rng, s, nombre_jugador):
    roles = ["RHP", "LHP", "RHP", "RHP", "LHP"]
    pesos_innings = [0.27, 0.25, 0.22, 0.14, 0.12]
    innings = repartir(rng, s["innings"], pesos_innings)
    calidad = [rng.uniform(0.7, 1.4) for _ in roles]
    carreras = repartir(rng, s["contra"], [ip * c for ip, c in zip(innings, calidad)])
    hits = repartir(rng, s["hits_contra"], [ip * c for ip, c in zip(innings, calidad)])
    lista = []
    for i, rol in enumerate(roles):
        ip = innings[i]
        k = int(round(ip * rng.uniform(0.65, 1.25) / calidad[i]))
        bb = int(round(ip * rng.uniform(0.25, 0.5) * calidad[i]))
        lista.append(dict(
            nombre=nombre_jugador(), edad=rng.randint(19, 37), posicion=rol,
            derechoZurdo="Zurdo" if rol == "LHP" else "Derecho",
            inningsLanzados=ip, carrerasPermitidas=carreras[i], hitsPermitidos=hits[i],
            ponchados=k, basePorBola=bb, numeroLanzamientos=int(round(ip * rng.uniform(14, 17.5))),
            aparicionesPlato=ip * 3 + hits[i] + bb))
    return lista


def bateador_libre(rng, nombre):
    s = rng.randint(2, 9)
    return dict(nombre=nombre, edad=rng.randint(20, 34), posicion=rng.choice(["LF", "1B", "C"]),
                derechoZurdo="Derecho", sencillos=s, dobles=rng.randint(0, 3), triples=0,
                homeRuns=rng.randint(0, 2), basePorBola=rng.randint(1, 6), basePorGolpe=0,
                ponches=rng.randint(3, 10), roboDeBase=rng.randint(0, 2), carrerasImpulsadas=rng.randint(1, 8),
                errores=rng.randint(0, 2))


def lanzador_libre(rng, nombre):
    ip = rng.randint(5, 14)
    return dict(nombre=nombre, edad=rng.randint(22, 35), posicion="RHP", derechoZurdo="Derecho",
                inningsLanzados=ip, carrerasPermitidas=rng.randint(2, 9), hitsPermitidos=rng.randint(ip - 2, ip + 6),
                ponchados=rng.randint(ip // 2, ip + 3), basePorBola=rng.randint(2, 8),
                numeroLanzamientos=ip * 16, aparicionesPlato=ip * 4 + 3)


# ---------------------------------------------------------------- carga

def main():
    ap = argparse.ArgumentParser(description="Carga la temporada Demo (datos ficticios) en la API LBC.")
    ap.add_argument("--api", default=os.environ.get("LBC_API_URL"), help="URL base, p. ej. https://<app>.up.railway.app")
    ap.add_argument("--usuario", default=os.environ.get("LBC_ADMIN_USER", "adminlbc"))
    ap.add_argument("--anio", type=int, default=2026)
    ap.add_argument("--semilla", type=int, default=2026, help="misma semilla = mismos datos")
    ap.add_argument("--jornadas-pendientes", type=int, default=2, help="jornadas finales sin jugar (PROGRAMADO)")
    ap.add_argument("--reemplazar", action="store_true", help="borra la temporada Demo existente y la crea de nuevo")
    args = ap.parse_args()
    if not args.api:
        ap.error("indica --api o la variable LBC_API_URL")

    password = os.environ.get("LBC_ADMIN_PASSWORD") or getpass.getpass(f"Contraseña de {args.usuario}: ")
    api = Api(args.api)
    try:
        api.token = api.pedir("POST", "/auth/login", {"username": args.usuario, "password": password})["token"]

        existentes = [t for t in api.pedir("GET", "/temporadas") if t["nombreTemporada"] == NOMBRE_TEMPORADA]
        if existentes and not args.reemplazar:
            print(f"Ya existe '{NOMBRE_TEMPORADA}'. Usa --reemplazar para borrarla y crearla de nuevo.")
            return 1
        for t in existentes:
            api.pedir("DELETE", f"/temporadas/{t['id']}")
            print(f"Borrada la temporada Demo anterior ({t['id']}).")

        rng = random.Random(args.semilla)
        equipos, partidos, libres_b, libres_l = generar(rng, args.anio, args.jornadas_pendientes)

        t = api.pedir("POST", "/temporadas", {"nombreTemporada": NOMBRE_TEMPORADA, "anio": args.anio})["id"]
        print(f"Temporada creada: {t}")
        ids_estadios = [api.pedir("POST", f"/temporadas/{t}/estadios", {"nombre": n, "ubicacion": u})["id"]
                        for n, u in ESTADIOS]
        ids_equipos = []
        for e in equipos:
            ide = api.pedir("POST", f"/temporadas/{t}/equipos", {"nombre": e["nombre"], "ciudad": e["ciudad"]})["id"]
            ids_equipos.append(ide)
            for b in e["bateadores"]:
                api.pedir("POST", f"/temporadas/{t}/equipos/{ide}/bateadores", b)
            for lz in e["lanzadores"]:
                api.pedir("POST", f"/temporadas/{t}/equipos/{ide}/lanzadores", lz)
            print(f"  {e['nombre']}: {len(e['bateadores'])} bateadores, {len(e['lanzadores'])} lanzadores")
        for b in libres_b:
            api.pedir("POST", f"/temporadas/{t}/agentes-libres/bateadores", b)
        for lz in libres_l:
            api.pedir("POST", f"/temporadas/{t}/agentes-libres/lanzadores", lz)
        for p in partidos:
            api.pedir("POST", f"/temporadas/{t}/partidos", {
                "fecha": p["fecha"], "hora": p["hora"], "estado": p["estado"],
                "idEstadio": ids_estadios[p["estadio"]],
                "idEquipoLocal": ids_equipos[p["local"]], "idEquipoVisitante": ids_equipos[p["visitante"]],
                "carrerasEquipoLocal": p["cl"], "carrerasEquipoVisitante": p["cv"],
                "innings": p["innings"], "inningActual": p["actual"],
                "hitTotalesPartido": p["hits"], "erroresTotalesPartido": p["errores"]})
        jugados = sum(1 for p in partidos if p["estado"] == "FINALIZADO")
        print(f"  {len(partidos)} partidos ({jugados} finalizados, {len(partidos) - jugados} programados)")

        print("\nTabla de posiciones:")
        for f in api.pedir("GET", f"/temporadas/{t}/posiciones"):
            pct = "—" if f["porcentajeVictorias"] is None else f"{f['porcentajeVictorias']:.3f}"
            print(f"  {f['posicion']}. {f['nombre']:<26} {f['ganados']:>2}-{f['perdidos']:<2} {pct}  dif {f['diferencial']:+d}")
        print(f"\nListo. Temporada Demo: {t}")
        return 0
    except ErrorApi as e:
        print(f"Error: {e}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
