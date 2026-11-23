from fastapi.testclient import TestClient

from app.main import app

cliente = TestClient(app)

ENTRADA = {
    "localVisitante": "Local",
    "rachaUlt3": 2,
    "promCarrerasFavor": 5.1,
    "promCarrerasContra": 3.2,
    "rivalFuerza": "Media",
}


def test_salud():
    r = cliente.get("/health")
    assert r.status_code == 200
    assert r.json()["estado"] == "ok"


def test_opciones():
    r = cliente.get("/opciones")
    assert r.status_code == 200
    datos = r.json()
    assert datos["localVisitante"] == ["Local", "Visitante"]
    assert datos["rivalFuerza"] == ["Baja", "Alta", "Media"]
    assert datos["instanciasEntrenamiento"] == 400
    assert set(datos["rangos"]) == {"rachaUlt3", "promCarrerasFavor", "promCarrerasContra"}


def test_predecir_devuelve_la_forma_que_espera_java():
    r = cliente.post("/predecir", json=ENTRADA)
    assert r.status_code == 200
    datos = r.json()
    assert datos["clase"] in ("Si", "No")
    assert datos["gana"] == (datos["clase"] == "Si")
    assert 50 <= datos["confianza"] <= 100
    assert abs(sum(datos["probabilidades"].values()) - 100) < 0.3
    assert list(datos["probabilidades"]) == ["Si", "No"]
    assert datos["entradas"] == ENTRADA
    assert "el modelo predice que" in datos["resumen"]


def test_predecir_rechaza_racha_fuera_de_rango():
    r = cliente.post("/predecir", json={**ENTRADA, "rachaUlt3": 5})
    assert r.status_code == 422


def test_predecir_rechaza_rival_desconocido():
    r = cliente.post("/predecir", json={**ENTRADA, "rivalFuerza": "Desconocida"})
    assert r.status_code == 422
