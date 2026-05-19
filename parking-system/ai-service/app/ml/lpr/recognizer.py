"""Reconocedor de placas — wrapper sobre PaddleOCR.

`read(image_bytes)` devuelve (texto, confianza, alternativas).
Filtra el resultado a alfanuméricos y mayúsculas; aplica la regla colombiana
"3 letras + 3 dígitos" como filtro suave (no estricto: permite otros formatos).
"""
from __future__ import annotations

import re
from io import BytesIO

import numpy as np
from PIL import Image

PLATE_REGEX = re.compile(r"^[A-Z]{2,3}[0-9]{2,4}$")


class PlateRecognizer:
    def __init__(self, model: object | None, stub: bool = False) -> None:
        self.model = model
        self.stub = stub

    def read(self, image_bytes: bytes) -> tuple[str, float, list[tuple[str, float]]]:
        if self.stub or self.model is None:
            return ("ABC123", 0.5, [])

        img = Image.open(BytesIO(image_bytes)).convert("RGB")
        arr = np.array(img)
        results = self.model.ocr(arr)  # type: ignore[attr-defined]
        candidates: list[tuple[str, float]] = []
        for block in results or []:
            for line in block or []:
                _, (text, score) = line
                text_clean = re.sub(r"[^A-Z0-9]", "", text.upper())
                if text_clean:
                    candidates.append((text_clean, float(score)))
        if not candidates:
            return ("", 0.0, [])

        # Prioriza candidatos que parecen placa válida.
        ranked = sorted(
            candidates,
            key=lambda c: (PLATE_REGEX.match(c[0]) is not None, c[1]),
            reverse=True,
        )
        best = ranked[0]
        alternatives = ranked[1:5]
        return (best[0], best[1], alternatives)
