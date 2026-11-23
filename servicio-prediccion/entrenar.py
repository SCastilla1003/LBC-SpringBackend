"""Entrena la red neuronal que predice si un equipo gana, y guarda el modelo en modelo/.

Uso (desde esta carpeta):
    python entrenar.py

Arquitectura: Dense(16, relu) -> Dense(8, relu) -> Dense(1, sigmoid), Adam, entropía cruzada binaria.
Requiere requirements-entrenamiento.txt además de requirements.txt.
"""

import json
import pathlib
import random

import numpy as np
import pandas as pd
import tensorflow as tf
from scipy.io import arff
from sklearn.model_selection import StratifiedKFold, train_test_split

from app.preproceso import CATEGORIAS_LOCAL, CATEGORIAS_RIVAL, NUMERICAS, vectorizar

SEMILLA = 42
RAIZ = pathlib.Path(__file__).parent
ARFF = RAIZ / "datos" / "lbc-dataset.arff"
SALIDA = RAIZ / "modelo"
EPOCAS = 1000

COLUMNAS = ["localVisitante", "rachaUlt3", "promCarrerasFavor", "promCarrerasContra", "rivalFuerza", "gana"]


def cargar() -> pd.DataFrame:
    datos, _ = arff.loadarff(ARFF)
    df = pd.DataFrame(datos)
    for columna in df.select_dtypes(include="object").columns:
        df[columna] = df[columna].str.decode("utf-8")
    df.columns = COLUMNAS
    return df


def construir_modelo(entradas: int) -> tf.keras.Model:
    tf.keras.utils.set_random_seed(SEMILLA)
    modelo = tf.keras.Sequential([
        tf.keras.Input(shape=(entradas,)),
        tf.keras.layers.Dense(16, activation="relu"),
        tf.keras.layers.Dense(8, activation="relu"),
        tf.keras.layers.Dense(1, activation="sigmoid"),
    ])
    modelo.compile(optimizer=tf.keras.optimizers.Adam(0.01), loss="binary_crossentropy", metrics=["accuracy"])
    return modelo


def preparar_prep(df_entrenamiento: pd.DataFrame) -> dict:
    """Media y desviación de las numéricas calculadas solo con el conjunto de entrenamiento."""
    return {
        "categorias": {"localVisitante": CATEGORIAS_LOCAL, "rivalFuerza": CATEGORIAS_RIVAL},
        "media": {n: float(df_entrenamiento[n].mean()) for n in NUMERICAS},
        "desviacion": {n: float(df_entrenamiento[n].std(ddof=0)) for n in NUMERICAS},
    }


def matriz(df: pd.DataFrame, prep: dict) -> tuple[np.ndarray, np.ndarray]:
    filas = [
        vectorizar(r.localVisitante, r.rivalFuerza,
                   {n: getattr(r, n) for n in NUMERICAS}, prep)[0]
        for r in df.itertuples(index=False)
    ]
    x = np.array(filas, dtype=np.float32)
    y = (df["gana"] == "Si").astype(np.float32).to_numpy()
    return x, y


def entrenar(x: np.ndarray, y: np.ndarray) -> tf.keras.Model:
    modelo = construir_modelo(x.shape[1])
    parada = tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=100, restore_best_weights=True)
    modelo.fit(x, y, epochs=EPOCAS, validation_split=0.2, callbacks=[parada], verbose=0)
    return modelo


def precision(modelo: tf.keras.Model, x: np.ndarray, y: np.ndarray) -> float:
    prob = modelo.predict(x, verbose=0).flatten()
    return float(np.mean((prob >= 0.5) == (y == 1)))


def main() -> None:
    random.seed(SEMILLA)
    np.random.seed(SEMILLA)
    df = cargar()
    entrenamiento, prueba = train_test_split(df, test_size=0.2, stratify=df["gana"], random_state=SEMILLA)

    prep = preparar_prep(entrenamiento)
    x_tr, y_tr = matriz(entrenamiento, prep)
    x_te, y_te = matriz(prueba, prep)

    modelo = entrenar(x_tr, y_tr)
    acc_prueba = precision(modelo, x_te, y_te)

    # Validación cruzada: estima mejor el error porque la prueba tiene solo 80 filas.
    accs = []
    for tr, te in StratifiedKFold(n_splits=5, shuffle=True, random_state=SEMILLA).split(df, df["gana"]):
        dtr, dte = df.iloc[tr], df.iloc[te]
        p = preparar_prep(dtr)
        xa, ya = matriz(dtr, p)
        xb, yb = matriz(dte, p)
        accs.append(precision(entrenar(xa, ya), xb, yb))
    acc_cv = float(np.mean(accs))

    rangos = {n: {"minimo": float(df[n].min()), "maximo": float(df[n].max()),
                  "promedio": round(float(df[n].mean()), 2)} for n in NUMERICAS}
    prep.update({"rangos": rangos, "instanciasEntrenamiento": len(df),
                 "metricas": {"precisionPrueba": round(acc_prueba, 4), "precisionValidacionCruzada": round(acc_cv, 4)}})

    SALIDA.mkdir(exist_ok=True)
    modelo.save(SALIDA / "modelo.keras")
    (SALIDA / "preproceso.json").write_text(json.dumps(prep, indent=2, ensure_ascii=False), encoding="utf-8")

    print(f"Precisión en prueba: {acc_prueba:.3f}")
    print(f"Precisión validación cruzada (5 pliegues): {acc_cv:.3f}")
    print(f"Modelo guardado en {SALIDA}")


if __name__ == "__main__":
    main()
