"""Codificación de las entradas. La usan el entrenamiento y el servicio, así que deben coincidir."""

import numpy as np

CATEGORIAS_LOCAL = ["Local", "Visitante"]
CATEGORIAS_RIVAL = ["Baja", "Alta", "Media"]
NUMERICAS = ["rachaUlt3", "promCarrerasFavor", "promCarrerasContra"]


def vectorizar(local_visitante: str, rival_fuerza: str, numericas: dict, prep: dict) -> np.ndarray:
    """Convierte una observación en el vector que recibe la red.

    Las dos categorías van en one-hot; las numéricas se estandarizan con la media y
    la desviación del conjunto de entrenamiento (guardadas en preproceso.json).
    """
    x = [1.0 if local_visitante == c else 0.0 for c in prep["categorias"]["localVisitante"]]
    x += [1.0 if rival_fuerza == c else 0.0 for c in prep["categorias"]["rivalFuerza"]]
    for nombre in NUMERICAS:
        valor = float(numericas[nombre])
        x.append((valor - prep["media"][nombre]) / prep["desviacion"][nombre])
    return np.array([x], dtype=np.float32)
