"""Provedor para APIs compatíveis com o protocolo da OpenAI.

Um único cliente cobre OpenAI, Grok (x.ai), Ollama/Llama local e qualquer
serviço "OpenAI-compatible" (Groq, DeepSeek, vLLM…): muda apenas a base_url.
A saída estruturada é pedida via response_format quando o servidor aceita,
com fallback para extração de JSON do texto + validação Pydantic (o harness
repete uma vez em caso de resposta inválida).
"""
import json
import re

from pydantic import BaseModel

from app.providers.base import Provider, Uso

# base_url padrão por provedor; "openai" usa o endpoint oficial do SDK.
BASES_PADRAO = {
    "openai": None,
    "grok": "https://api.x.ai/v1",
    "ollama": "http://localhost:11434/v1",
    "personalizado": None,
}


class OpenAICompatProvider(Provider):

    def __init__(self, nome: str, api_key: str, base_url: str) -> None:
        from openai import OpenAI
        base = base_url or BASES_PADRAO.get(nome)
        if nome == "personalizado" and not base:
            raise RuntimeError("Provedor 'personalizado' exige base_url")
        # Ollama local não valida chave, mas o SDK exige um valor não vazio.
        self._client = OpenAI(api_key=api_key or "sem-chave", base_url=base)

    def gerar(self, operacao: str, entrada: dict, system: str, user: str,
              schema: type[BaseModel], modelo: str, max_tokens: int) -> tuple[BaseModel, Uso]:
        instrucao = (
            f"{system}\n\nResponda SOMENTE com um objeto JSON válido, sem comentários "
            f"nem markdown, conforme este JSON Schema:\n{json.dumps(schema.model_json_schema())}"
        )
        mensagens = [{"role": "system", "content": instrucao},
                     {"role": "user", "content": user}]
        try:
            resposta = self._client.chat.completions.create(
                model=modelo, max_tokens=max_tokens, messages=mensagens,
                response_format={"type": "json_object"})
        except Exception:
            # Alguns servidores compatíveis não aceitam response_format.
            resposta = self._client.chat.completions.create(
                model=modelo, max_tokens=max_tokens, messages=mensagens)

        texto = resposta.choices[0].message.content or ""
        resultado = schema.model_validate(self._extrair_json(texto))
        uso = Uso(
            model=modelo,
            input_tokens=getattr(resposta.usage, "prompt_tokens", 0) or 0,
            output_tokens=getattr(resposta.usage, "completion_tokens", 0) or 0,
        )
        return resultado, uso

    @staticmethod
    def _extrair_json(texto: str) -> dict:
        texto = re.sub(r"^```(?:json)?\s*|\s*```$", "", texto.strip())
        inicio, fim = texto.find("{"), texto.rfind("}")
        if inicio < 0 or fim <= inicio:
            raise ValueError("Resposta sem JSON")
        return json.loads(texto[inicio:fim + 1])
