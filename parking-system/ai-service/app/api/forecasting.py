"""Forecast de ocupación por hora.

Implementación simple: media móvil + estacionalidad por hora-del-día sobre
una serie histórica entregada como JSON.  Para producción se cambia a un
modelo SARIMA / Prophet.
"""
from __future__ import annotations

from datetime import datetime
from typing import Any

import numpy as np
from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class HistoricalPoint(BaseModel):
    timestamp: datetime
    occupancy: int


class ForecastRequest(BaseModel):
    history: list[HistoricalPoint]
    horizonHours: int = 24


class ForecastPoint(BaseModel):
    timestamp: datetime
    expectedOccupancy: int
    lower: int
    upper: int


class ForecastResponse(BaseModel):
    horizon: int
    points: list[ForecastPoint]
    method: str = "hourly_mean"


@router.post("/occupancy", response_model=ForecastResponse)
async def forecast_occupancy(req: ForecastRequest) -> ForecastResponse:
    if not req.history:
        return ForecastResponse(horizon=req.horizonHours, points=[])

    arr = np.array([(p.timestamp.hour, p.occupancy) for p in req.history])
    by_hour: dict[int, list[int]] = {h: [] for h in range(24)}
    for hour, occ in arr:
        by_hour[int(hour)].append(int(occ))
    means = {h: float(np.mean(v)) if v else 0.0 for h, v in by_hour.items()}
    stds = {h: float(np.std(v)) if v else 0.0 for h, v in by_hour.items()}

    now = req.history[-1].timestamp
    points: list[Any] = []
    for i in range(1, req.horizonHours + 1):
        t = now.replace(minute=0, second=0, microsecond=0)
        t = t.replace(hour=(t.hour + i) % 24)
        m, s = means[t.hour], stds[t.hour]
        points.append(
            ForecastPoint(
                timestamp=t,
                expectedOccupancy=int(round(m)),
                lower=int(max(0, round(m - 1.96 * s))),
                upper=int(round(m + 1.96 * s)),
            )
        )
    return ForecastResponse(horizon=req.horizonHours, points=points)
