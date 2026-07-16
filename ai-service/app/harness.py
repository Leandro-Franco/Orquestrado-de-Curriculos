"""Harness da LLM (Contexto Mestre, seção 9).

Aceita apenas operações previamente registradas, seleciona o modelo por
nível, exige saída estruturada, aplica política de repetição e devolve
métricas de uso em todo envelope. Não existe operação genérica.
"""
import time
from dataclasses import dataclass
from typing import Callable, Optional

from pydantic import BaseModel

from app import config, prompts, schemas
from app.providers import obter_provider


@dataclass
class Operacao:
    nivel: str  # ECONOMICO | INTERMEDIARIO | AVANCADO
    prompt: Callable[[dict], tuple[str, str]]
    schema: type[BaseModel]
    max_tokens: int = 2048
    transformar: Optional[Callable[[BaseModel, str], dict]] = None


def _transformar_extracao(resultado: BaseModel, modelo: str) -> dict:
    """Achata o payload (removendo campos None) e carimba modelo/versão do
    prompt em cada proposta — rastreabilidade exigida pela seção 5.4."""
    saida = {"propostas": []}
    for p in resultado.propostas:  # type: ignore[attr-defined]
        item = p.model_dump(exclude={"payload"})
        item["payload"] = p.payload.model_dump(exclude_none=True)
        item["modelo"] = modelo
        item["versao_prompt"] = config.VERSAO_PROMPT
        saida["propostas"].append(item)
    return saida


OPERACOES: dict[str, Operacao] = {
    "extrair-conhecimento": Operacao("INTERMEDIARIO", prompts.extrair_conhecimento,
                                     schemas.ResultadoExtracao, 4096, _transformar_extracao),
    "analisar-vaga": Operacao("INTERMEDIARIO", prompts.analisar_vaga,
                              schemas.ResultadoAnaliseVaga, 4096),
    "relacionar-requisitos": Operacao("ECONOMICO", prompts.relacionar_requisitos,
                                      schemas.ResultadoRelacionamento, 4096),
    "gerar-estrategia": Operacao("AVANCADO", prompts.gerar_estrategia,
                                 schemas.ResultadoEstrategia, 2048),
    "gerar-secao": Operacao("AVANCADO", prompts.gerar_secao,
                            schemas.ResultadoSecao, 2048),
    "validar-afirmacoes": Operacao("INTERMEDIARIO", prompts.validar_afirmacoes,
                                   schemas.ResultadoValidacao, 4096),
    "resumir-documento": Operacao("ECONOMICO", prompts.resumir_documento,
                                  schemas.ResultadoResumo, 1024),
}


def _custo_estimado(modelo: str, tokens_entrada: int, tokens_saida: int) -> float:
    nome = modelo.removeprefix("fake:")
    for prefixo, (preco_in, preco_out) in config.PRECOS_POR_MTOK.items():
        if nome.startswith(prefixo):
            if modelo.startswith("fake:"):
                return 0.0
            return (tokens_entrada * preco_in + tokens_saida * preco_out) / 1_000_000
    return 0.0


def executar(nome: str, entrada: dict) -> dict:
    op = OPERACOES.get(nome)
    if op is None:
        raise KeyError(nome)

    system, user = op.prompt(entrada)
    modelo = config.MODELOS[op.nivel]
    provider = obter_provider()

    inicio = time.monotonic()
    ultimo_erro: Exception | None = None
    for tentativa in range(1, config.MAX_TENTATIVAS + 1):
        try:
            resultado, uso = provider.gerar(nome, entrada, system, user,
                                            op.schema, modelo, op.max_tokens)
            duracao_ms = int((time.monotonic() - inicio) * 1000)
            corpo = (op.transformar(resultado, uso.model) if op.transformar
                     else resultado.model_dump())
            return {
                "result": corpo,
                "usage": {
                    "model": uso.model,
                    "input_tokens": uso.input_tokens,
                    "output_tokens": uso.output_tokens,
                    "cached_tokens": uso.cached_tokens,
                    "retrieved_chunks": 0,
                    "duration_ms": duracao_ms,
                    "estimated_cost": _custo_estimado(uso.model, uso.input_tokens,
                                                      uso.output_tokens),
                    "attempts": tentativa,
                    "valid": True,
                },
            }
        except Exception as erro:  # resposta inválida ou falha do provedor
            ultimo_erro = erro
    raise RuntimeError(f"Operação {nome} falhou após "
                       f"{config.MAX_TENTATIVAS} tentativas: {ultimo_erro}")
