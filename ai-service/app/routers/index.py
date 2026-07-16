from fastapi import APIRouter
from pydantic import BaseModel

from app import rag

router = APIRouter(prefix="/v1/index", tags=["index"])


class EntradaUpsert(BaseModel):
    origem: str  # DOCUMENTO | FATO
    origem_id: int
    conteudo: str


class EntradaBusca(BaseModel):
    consulta: str
    top_k: int = 5
    origem: str | None = None


@router.post("/upsert")
def upsert(entrada: EntradaUpsert) -> dict:
    trechos = rag.upsert(entrada.origem, entrada.origem_id, entrada.conteudo)
    return {"trechos_indexados": trechos}


@router.post("/search")
def buscar(entrada: EntradaBusca) -> dict:
    return {"trechos": rag.buscar(entrada.consulta, entrada.top_k, entrada.origem)}


@router.delete("/{origem}/{origem_id}")
def remover(origem: str, origem_id: int) -> dict:
    rag.remover(origem, origem_id)
    return {"removido": True}
