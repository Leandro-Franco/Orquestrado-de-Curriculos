from typing import Optional

from fastapi import APIRouter, Header, HTTPException

from app import config, harness

router = APIRouter(prefix="/v1/operations", tags=["operations"])


@router.get("")
def listar_operacoes() -> dict:
    return {nome: {"nivel": op.nivel, "schema": op.schema.__name__}
            for nome, op in harness.OPERACOES.items()}


@router.post("/{nome}")
def executar(
    nome: str,
    entrada: dict,
    x_ai_provider: Optional[str] = Header(None),
    x_ai_api_key: Optional[str] = Header(None),
    x_ai_base_url: Optional[str] = Header(None),
    x_ai_modelo_economico: Optional[str] = Header(None),
    x_ai_modelo_intermediario: Optional[str] = Header(None),
    x_ai_modelo_avancado: Optional[str] = Header(None),
) -> dict:
    """Único ponto de execução de IA. Operação fora do registro -> 404
    (impedir operações não autorizadas, seção 9). A configuração de
    provedor por requisição chega em cabeçalhos internos X-AI-* (ADR-004)."""
    cfg = None
    if x_ai_provider:
        modelos = {nivel: valor for nivel, valor in {
            "ECONOMICO": x_ai_modelo_economico,
            "INTERMEDIARIO": x_ai_modelo_intermediario,
            "AVANCADO": x_ai_modelo_avancado,
        }.items() if valor}
        cfg = config.RuntimeConfig(
            provider=x_ai_provider,
            api_key=x_ai_api_key or "",
            base_url=x_ai_base_url or "",
            modelos=modelos,
        )
    try:
        return harness.executar(nome, entrada, cfg)
    except KeyError:
        raise HTTPException(status_code=404, detail=f"Operação não registrada: {nome}")
    except RuntimeError as erro:
        raise HTTPException(status_code=502, detail=str(erro))
