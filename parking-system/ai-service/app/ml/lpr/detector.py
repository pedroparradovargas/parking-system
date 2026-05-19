"""Detector de placas — wrapper sobre YOLO (Ultralytics).

Devuelve una lista de `PlateCrop` con bytes recortados listos para PaddleOCR.
En `stub=True` devuelve un único crop falso para pruebas.
"""
from __future__ import annotations

from dataclasses import dataclass
from io import BytesIO

import numpy as np
from PIL import Image


@dataclass
class PlateCrop:
    image_bytes: bytes
    x: int
    y: int
    w: int
    h: int
    confidence: float


class PlateDetector:
    def __init__(self, model: object | None, stub: bool = False) -> None:
        self.model = model
        self.stub = stub

    def detect(self, image_bytes: bytes) -> list[PlateCrop]:
        if self.stub or self.model is None:
            # Devuelve la imagen completa con confidence baja.  Útil en CI sin GPU.
            return [PlateCrop(image_bytes=image_bytes, x=0, y=0, w=0, h=0, confidence=0.2)]

        img = Image.open(BytesIO(image_bytes)).convert("RGB")
        arr = np.array(img)
        results = self.model(arr, verbose=False)  # type: ignore[operator]
        crops: list[PlateCrop] = []
        for r in results:
            boxes = getattr(r, "boxes", None)
            if boxes is None:
                continue
            for box in boxes:
                xyxy = box.xyxy[0].tolist()
                conf = float(box.conf[0])
                x1, y1, x2, y2 = map(int, xyxy)
                crop_arr = arr[y1:y2, x1:x2]
                buf = BytesIO()
                Image.fromarray(crop_arr).save(buf, format="JPEG", quality=92)
                crops.append(
                    PlateCrop(
                        image_bytes=buf.getvalue(),
                        x=x1, y=y1, w=x2 - x1, h=y2 - y1,
                        confidence=conf,
                    )
                )
        return crops
