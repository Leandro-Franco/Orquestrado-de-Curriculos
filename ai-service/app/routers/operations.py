from fastapi import APIRouter, HTTPException

from app import harness

router = APIRouter(prefix="/v1/operations", tags=["operations"])


@router.get("")
def listar_operacoes() -> dict:
    return {nome: {"nivel": op.nivel, "schema": op.schema.__name__}
            for nome, op in harness.OPERACOES.items()}


@router.post("/{nome}")
def executar(nome: str, entrada: dict) -> dict:
    """Único ponto de execução de IA. Operação fora do registro -> 404
    (impedir operações não autorizadas, seção 9)."""
    try:
        return harness.executar(nome, entrada)
    except KeyError:
        raise HTTPException(status_code=404, detail=f"Operação não registrada: {nome}")
    except RuntimeError as erro:
        raise HTTPException(status_code=502, detail=str(erro))
