"""Configuración tipada a partir de variables de entorno."""
from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", case_sensitive=False)

    # Servidor
    host: str = "0.0.0.0"
    port: int = 8000
    log_level: str = "INFO"

    # CORS
    cors_allowed_origins: list[str] = [
        "http://localhost:3000",
        "https://admin.parking.example.com",
    ]

    # Modelos
    yolo_weights: str = "yolov8n.pt"  # se descarga automáticamente si no existe
    paddle_lang: str = "es"
    plate_min_confidence: float = 0.55

    # Auth simple — el ai-service confía en el backend Ktor (red privada).
    # En zero-trust se añadiría mTLS o un JWT.
    api_key: str = ""


settings = Settings()
