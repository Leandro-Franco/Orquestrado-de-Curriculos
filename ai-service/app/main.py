"""Serviço de IA do Gerador Inteligente de Currículos.

Este serviço não é proprietário dos dados: não escreve na base canônica.
Sua superfície é o harness (operações fechadas) e o índice vetorial.
"""
from fastapi import FastAPI

from app import config
from app.routers import index, operations

app = FastAPI(
    title="Serviço de IA — Gerador Inteligente de Currículos",
    version="0.1.0",
)
app.include_router(operations.router)
app.include_router(index.router)


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "provider": config.AI_PROVIDER,
        "modelos": config.MODELOS,
    }
