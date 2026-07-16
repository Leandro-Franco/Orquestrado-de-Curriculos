"""Provedor real via SDK oficial da Anthropic.

Usa saídas estruturadas (client.messages.parse + output_format) para
garantir que a resposta valida contra o schema Pydantic da operação.
"""
from anthropic import Anthropic
from pydantic import BaseModel

from app import config
from app.providers.base import Provider, Uso


class AnthropicProvider(Provider):

    def __init__(self) -> None:
        if not config.ANTHROPIC_API_KEY:
            raise RuntimeError("AI_PROVIDER=anthropic exige ANTHROPIC_API_KEY")
        self._client = Anthropic(api_key=config.ANTHROPIC_API_KEY)

    def gerar(self, operacao: str, entrada: dict, system: str, user: str,
              schema: type[BaseModel], modelo: str, max_tokens: int) -> tuple[BaseModel, Uso]:
        response = self._client.messages.parse(
            model=modelo,
            max_tokens=max_tokens,
            system=system,
            messages=[{"role": "user", "content": user}],
            output_format=schema,
        )
        resultado = response.parsed_output
        if resultado is None:
            raise ValueError(f"Resposta não estruturada do modelo para {operacao}")
        uso = Uso(
            model=modelo,
            input_tokens=response.usage.input_tokens,
            output_tokens=response.usage.output_tokens,
            cached_tokens=getattr(response.usage, "cache_read_input_tokens", 0) or 0,
        )
        return resultado, uso
