"""Memória vetorial (Contexto Mestre, seção 5.3).

O RAG localiza evidências; nunca é fonte da verdade. No MVP o embedding é
determinístico e local (hashing de tokens em 256 dimensões, ADR-003) —
sem dependência externa. A troca por um provedor real de embeddings é
uma mudança contida neste módulo.
"""
import hashlib
import math
import re

import psycopg

from app import config

DIMENSOES = 256
TAMANHO_TRECHO = 800
SOBREPOSICAO = 100


def _conexao():
    return psycopg.connect(config.DATABASE_URL)


def embutir(texto: str) -> list[float]:
    vetor = [0.0] * DIMENSOES
    for token in re.findall(r"[\wÀ-ÿ+#./-]{2,}", texto.lower()):
        digesto = hashlib.sha256(token.encode()).digest()
        indice = int.from_bytes(digesto[:4], "big") % DIMENSOES
        sinal = 1.0 if digesto[4] % 2 == 0 else -1.0
        vetor[indice] += sinal
    norma = math.sqrt(sum(v * v for v in vetor)) or 1.0
    return [v / norma for v in vetor]


def _como_literal(vetor: list[float]) -> str:
    return "[" + ",".join(f"{v:.6f}" for v in vetor) + "]"


def fatiar(texto: str) -> list[str]:
    texto = texto.strip()
    if len(texto) <= TAMANHO_TRECHO:
        return [texto] if texto else []
    trechos, inicio = [], 0
    while inicio < len(texto):
        trechos.append(texto[inicio:inicio + TAMANHO_TRECHO])
        inicio += TAMANHO_TRECHO - SOBREPOSICAO
    return trechos


def upsert(origem: str, origem_id: int, conteudo: str) -> int:
    """Reindexação incremental: substitui apenas os trechos da origem afetada."""
    trechos = fatiar(conteudo)
    with _conexao() as conexao, conexao.cursor() as cursor:
        cursor.execute(
            "DELETE FROM trecho_vetorial WHERE origem = %s AND origem_id = %s",
            (origem, origem_id))
        for trecho in trechos:
            cursor.execute(
                "INSERT INTO trecho_vetorial (origem, origem_id, conteudo, embedding) "
                "VALUES (%s, %s, %s, %s::vector)",
                (origem, origem_id, trecho, _como_literal(embutir(trecho))))
    return len(trechos)


def remover(origem: str, origem_id: int) -> None:
    with _conexao() as conexao, conexao.cursor() as cursor:
        cursor.execute(
            "DELETE FROM trecho_vetorial WHERE origem = %s AND origem_id = %s",
            (origem, origem_id))


def buscar(consulta: str, top_k: int = 5, origem: str | None = None) -> list[dict]:
    vetor = _como_literal(embutir(consulta))
    sql = ("SELECT origem, origem_id, conteudo, 1 - (embedding <=> %s::vector) AS similaridade "
           "FROM trecho_vetorial ")
    parametros: list = [vetor]
    if origem:
        sql += "WHERE origem = %s "
        parametros.append(origem)
    sql += "ORDER BY embedding <=> %s::vector LIMIT %s"
    parametros.extend([vetor, top_k])
    with _conexao() as conexao, conexao.cursor() as cursor:
        cursor.execute(sql, parametros)
        return [{"origem": linha[0], "origem_id": linha[1], "conteudo": linha[2],
                 "similaridade": round(float(linha[3]), 4)}
                for linha in cursor.fetchall()]
