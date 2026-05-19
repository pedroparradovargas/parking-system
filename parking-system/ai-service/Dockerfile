# =============================================================================
# ai-service — Dockerfile multi-stage
#
# Stage 1 (builder): instala dependencias (incluyendo paddle/ultralytics que
# son grandes) en una venv.
# Stage 2 (runtime): copia la venv a una imagen slim y arranca uvicorn.
# =============================================================================

FROM python:3.11-slim AS builder
WORKDIR /opt/app
ENV PIP_DISABLE_PIP_VERSION_CHECK=1 PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential libgl1 libglib2.0-0 && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./
RUN python -m venv /opt/venv && /opt/venv/bin/pip install --upgrade pip && \
    /opt/venv/bin/pip install --no-cache-dir \
        "fastapi==0.115.5" "uvicorn[standard]==0.32.0" "pydantic==2.9.2" \
        "pydantic-settings==2.6.1" "python-multipart==0.0.17" "loguru==0.7.2" \
        "opencv-python==4.10.0.84" "numpy==2.1.3" "Pillow==11.0.0" \
        "scikit-learn==1.5.2" "pandas==2.2.3" "httpx==0.27.2"
# YOLO/Paddle pesan; los dejamos como opt-in con extra args para imagen base ligera.

FROM python:3.11-slim AS runtime
WORKDIR /opt/app
ENV PATH="/opt/venv/bin:$PATH" PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY --from=builder /opt/venv /opt/venv
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 curl && rm -rf /var/lib/apt/lists/*

COPY app ./app
EXPOSE 8000
HEALTHCHECK --interval=30s --timeout=3s CMD curl -fsS http://localhost:8000/health || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
