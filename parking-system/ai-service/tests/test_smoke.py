"""Smoke tests del ai-service."""
from __future__ import annotations

import base64

from fastapi.testclient import TestClient

from app.main import app


def test_health() -> None:
    with TestClient(app) as client:
        r = client.get("/health")
        assert r.status_code == 200
        assert r.json() == {"status": "ok"}


def test_lpr_returns_response() -> None:
    """Aún en stub mode el endpoint debe responder 200 con plate string."""
    # 1x1 PNG transparente (base64) — válido para decode aunque no haya placa.
    one_pixel_png = base64.b64encode(
        bytes.fromhex(
            "89504E470D0A1A0A0000000D49484452000000010000000108060000001F15C489"
            "0000000A49444154789C6300010000000500010D0A2DB40000000049454E44AE426082"
        )
    ).decode("ascii")
    with TestClient(app) as client:
        r = client.post("/api/v1/lpr", json={"imageBase64": one_pixel_png})
        assert r.status_code == 200
        body = r.json()
        assert "plate" in body
        assert "confidence" in body
