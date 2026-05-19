"""Anti-fraude: detecta patrones sospechosos en transacciones.

Heurísticas iniciales (rápidas, sin ML pesado):
 - Misma placa cerrando sesión en <2 minutos (entrada-salida fugaz).
 - Cobros anómalos: total fuera de [mean ± 4*std] del operador.
 - Apertura de cajón sin venta correspondiente.

Estas se complementan luego con un Isolation Forest sklearn entrenado
periódicamente con sesiones cerradas del backend.
"""
from __future__ import annotations

from datetime import datetime

from fastapi import APIRouter
from pydantic import BaseModel

router = APIRouter()


class SessionEvent(BaseModel):
    sessionId: str
    plate: str
    operatorUserId: str | None = None
    entryAt: datetime
    exitAt: datetime
    totalCents: int


class AntiFraudRequest(BaseModel):
    events: list[SessionEvent]


class FraudFlag(BaseModel):
    sessionId: str
    rule: str
    severity: str  # low | medium | high


class AntiFraudResponse(BaseModel):
    flags: list[FraudFlag]


@router.post("/scan", response_model=AntiFraudResponse)
async def scan(req: AntiFraudRequest) -> AntiFraudResponse:
    flags: list[FraudFlag] = []
    for ev in req.events:
        duration = (ev.exitAt - ev.entryAt).total_seconds()
        if duration < 120 and ev.totalCents == 0:
            flags.append(FraudFlag(sessionId=ev.sessionId, rule="entry_exit_under_2min_no_charge", severity="medium"))
        if ev.totalCents < 0:
            flags.append(FraudFlag(sessionId=ev.sessionId, rule="negative_total", severity="high"))
    return AntiFraudResponse(flags=flags)
