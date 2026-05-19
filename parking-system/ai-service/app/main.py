"""FastAPI application factory + lifespan management."""
from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from app.api import anti_fraud, forecasting, health, lpr, reports
from app.core.config import settings
from app.core.ml_loader import MLRegistry


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Pre-carga modelos al arranque para evitar latencia en la primera petición."""
    logger.info("ai-service arrancando — pre-cargando modelos")
    registry = MLRegistry()
    await registry.warmup()
    app.state.ml = registry
    try:
        yield
    finally:
        logger.info("ai-service apagándose — liberando modelos")
        await registry.shutdown()


def create_app() -> FastAPI:
    app = FastAPI(
        title="Parking AI Service",
        version="1.0.0",
        description="LPR, forecast de ocupación, antifraude y reportes asistidos por IA.",
        lifespan=lifespan,
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_allowed_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )
    app.include_router(health.router)
    app.include_router(lpr.router, prefix="/api/v1/lpr", tags=["lpr"])
    app.include_router(forecasting.router, prefix="/api/v1/forecast", tags=["forecast"])
    app.include_router(anti_fraud.router, prefix="/api/v1/anti-fraud", tags=["anti-fraud"])
    app.include_router(reports.router, prefix="/api/v1/reports", tags=["reports"])
    return app


app = create_app()
