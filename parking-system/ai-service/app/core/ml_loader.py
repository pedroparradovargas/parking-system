"""Registro de modelos ML.  Se carga al arranque y se reutiliza por petición.

El loader es perezoso de tipo: si Ultralytics o PaddleOCR no están instalados
(imagen base mínima), se cae al modo "stub" para que el resto del servicio
siga funcionando — útil en CI y entornos de desarrollo sin GPU.
"""
from __future__ import annotations

import asyncio
from typing import Any

from loguru import logger

from app.core.config import settings


class MLRegistry:
    def __init__(self) -> None:
        self.detector: Any | None = None
        self.recognizer: Any | None = None
        self.stub_mode: bool = False

    async def warmup(self) -> None:
        """Carga modelos en hilos para no bloquear el event loop."""
        try:
            from ultralytics import YOLO  # type: ignore

            def _load_detector() -> Any:
                return YOLO(settings.yolo_weights)

            self.detector = await asyncio.get_running_loop().run_in_executor(None, _load_detector)
            logger.info(f"Detector YOLO cargado: {settings.yolo_weights}")
        except Exception as e:  # noqa: BLE001
            logger.warning(f"YOLO no disponible — modo stub: {e}")
            self.detector = None
            self.stub_mode = True

        try:
            from paddleocr import PaddleOCR  # type: ignore

            def _load_recognizer() -> Any:
                return PaddleOCR(lang=settings.paddle_lang, use_angle_cls=True, show_log=False)

            self.recognizer = await asyncio.get_running_loop().run_in_executor(None, _load_recognizer)
            logger.info("Reconocedor PaddleOCR cargado")
        except Exception as e:  # noqa: BLE001
            logger.warning(f"PaddleOCR no disponible — modo stub: {e}")
            self.recognizer = None
            self.stub_mode = True

    async def shutdown(self) -> None:
        self.detector = None
        self.recognizer = None
