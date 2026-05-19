"""Endpoint LPR: detección + reconocimiento de placa."""
from __future__ import annotations

import base64
import time

from fastapi import APIRouter, HTTPException, Request
from pydantic import BaseModel

from app.ml.lpr.detector import PlateDetector
from app.ml.lpr.recognizer import PlateRecognizer

router = APIRouter()


class LprRequest(BaseModel):
    imageBase64: str


class BoundingBox(BaseModel):
    x: int
    y: int
    width: int
    height: int


class LprAlternative(BaseModel):
    plate: str
    confidence: float


class LprResponse(BaseModel):
    plate: str
    confidence: float
    processingMs: int
    boundingBox: BoundingBox | None = None
    alternatives: list[LprAlternative] = []


@router.post("", response_model=LprResponse)
async def recognize_plate(req: LprRequest, request: Request) -> LprResponse:
    """Decodifica la imagen base64, detecta la placa y devuelve texto + confianza."""
    started = time.perf_counter()
    try:
        image_bytes = base64.b64decode(req.imageBase64, validate=True)
    except Exception as e:  # noqa: BLE001
        raise HTTPException(status_code=400, detail=f"invalid_base64: {e}")

    registry = request.app.state.ml
    detector = PlateDetector(registry.detector, stub=registry.stub_mode)
    recognizer = PlateRecognizer(registry.recognizer, stub=registry.stub_mode)

    crops = detector.detect(image_bytes)
    if not crops:
        elapsed = int((time.perf_counter() - started) * 1000)
        return LprResponse(plate="", confidence=0.0, processingMs=elapsed)

    best = max(crops, key=lambda c: c.confidence)
    text, score, alternatives = recognizer.read(best.image_bytes)
    elapsed = int((time.perf_counter() - started) * 1000)

    return LprResponse(
        plate=text,
        confidence=score,
        processingMs=elapsed,
        boundingBox=BoundingBox(x=best.x, y=best.y, width=best.w, height=best.h),
        alternatives=[LprAlternative(plate=p, confidence=c) for p, c in alternatives],
    )
