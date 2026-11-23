"""API de predicción de victoria. La consume el backend Java con PREDICCION_URL.

Endpoints:
- GET  /health    estado del servicio
- GET  /opciones  valores admitidos y rangos del entrenamiento (para el formulario)
- POST /predecir  probabilidad de que el equipo gane
"""

import json
import pathlib
from typing import Literal

import numpy as np
import tensorflow as tf
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from app.preproceso import CATEGORIAS_LOCAL, CATEGORIAS_RIVAL, vectorizar

RAIZ = pathlib.Path(__file__).resolve().parent.parent
MODELO = RAIZ / "modelo" / "modelo.keras"
PREPROCESO = RAIZ / "modelo" / "preproceso.json"

app = FastAPI(title="LBC · predicción", version="1.0.0")


class PrediccionRequest(BaseModel):
    localVisitante: Literal["Local", "Visitante"]
    rachaUlt3: float = Field(ge=0, le=3, description="Victorias en los últimos 3 partidos")
    promCarrerasFavor: float = Field(ge=0)
    promCarrerasContra: float = Field(ge=0)
    rivalFuerza: Literal["Baja", "Media", "Alta"]


class PrediccionResponse(BaseModel):
    clase: str
    gana: bool
    confianza: float
    probabilidades: dict[str, float]
    entradas: PrediccionRequest
    resumen: str


_modelo: tf.keras.Model | None = None
_prep: dict | None = None


def _cargar() -> None:
    global _modelo, _prep
    if _modelo is None and MODELO.exists() and PREPROCESO.exists():
        _modelo = tf.keras.models.load_model(MODELO)
        _prep = json.loads(PREPROCESO.read_text(encoding="utf-8"))


def _sin_modelo() -> HTTPException:
    return HTTPException(status_code=503, detail="El modelo no está cargado. Ejecuta entrenar.py.")


@app.get("/health")
def salud() -> dict:
    _cargar()
    return {"estado": "ok" if _modelo is not None else "sin_modelo"}


@app.get("/opciones")
def opciones() -> dict:
    _cargar()
    if _prep is None:
        raise _sin_modelo()
    return {
        "localVisitante": CATEGORIAS_LOCAL,
        "rivalFuerza": CATEGORIAS_RIVAL,
        "rangos": {
            nombre: {"minimo": r["minimo"], "maximo": r["maximo"], "promedio": r["promedio"]}
            for nombre, r in _prep["rangos"].items()
        },
        "instanciasEntrenamiento": _prep["instanciasEntrenamiento"],
    }


@app.post("/predecir", response_model=PrediccionResponse)
def predecir(req: PrediccionRequest) -> PrediccionResponse:
    _cargar()
    if _modelo is None or _prep is None:
        raise _sin_modelo()

    x = vectorizar(req.localVisitante, req.rivalFuerza, req.model_dump(), _prep)
    p_si = float(np.asarray(_modelo(x, training=False))[0][0])
    gana = p_si >= 0.5
    clase = "Si" if gana else "No"
    confianza = round((p_si if gana else 1 - p_si) * 100, 1)
    return PrediccionResponse(
        clase=clase,
        gana=gana,
        confianza=confianza,
        probabilidades={"Si": round(p_si * 100, 1), "No": round((1 - p_si) * 100, 1)},
        entradas=req,
        resumen=_resumen(req, gana, confianza),
    )


def _resumen(r: PrediccionRequest, gana: bool, confianza: float) -> str:
    condicion = "jugando como local" if r.localVisitante == "Local" else "jugando como visitante"
    racha = round(r.rachaUlt3)
    plural = "" if racha == 1 else "s"
    return (
        f"Con el equipo {condicion}, {racha} victoria{plural} en sus últimos 3 partidos, "
        f"un promedio de {r.promCarrerasFavor:.1f} carreras anotadas y {r.promCarrerasContra:.1f} "
        f"recibidas por partido, frente a un rival de fuerza {r.rivalFuerza.lower()}, "
        f"el modelo predice que {'GANA' if gana else 'NO GANA'} con una confianza del {confianza:.1f} %."
    )
