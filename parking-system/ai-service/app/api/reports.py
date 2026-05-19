"""Reportes asistidos por IA: insights, anomalías, segmentación de clientes."""
from __future__ import annotations

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class InsightRequest(BaseModel):
    parkingId: str
    metric: str  # e.g. "revenue_daily", "occupancy_hourly"
    series: list[float]


class Insight(BaseModel):
    kind: str
    description: str
    severity: str


class InsightResponse(BaseModel):
    insights: list[Insight]


@router.post("/insights", response_model=InsightResponse)
async def insights(req: InsightRequest) -> InsightResponse:
    """Análisis simple sobre la serie temporal entregada."""
    if not req.series:
        return InsightResponse(insights=[])

    n = len(req.series)
    mean = sum(req.series) / n
    last = req.series[-1]
    out: list[Insight] = []
    if last > mean * 1.25:
        out.append(Insight(kind="spike", description=f"El último punto supera 25% el promedio ({last:.0f} vs {mean:.0f})", severity="medium"))
    if last < mean * 0.5 and n > 5:
        out.append(Insight(kind="drop", description=f"El último punto cae 50% bajo el promedio ({last:.0f} vs {mean:.0f})", severity="high"))
    return InsightResponse(insights=out)
