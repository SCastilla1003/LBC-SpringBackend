# Servicio de predicción para Railway (raíz del servicio: carpeta servicio-prediccion).
# El modelo ya entrenado va en modelo/; el entrenamiento no se ejecuta aquí.
FROM python:3.11-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    TF_CPP_MIN_LOG_LEVEL=2

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY modelo ./modelo

# Railway inyecta PORT
CMD ["sh", "-c", "exec uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8080}"]
