"""Configuração do serviço de IA — padrões por variável de ambiente,
com sobreposição por requisição via cabeçalhos X-AI-* (ADR-004).

O provedor da LLM é substituível (Contexto Mestre, seção 17) e os
segredos vivem apenas no backend; nada de chaves no frontend.
"""
import os
from dataclasses import dataclass, field

DATABASE_URL = os.environ.get(
    "DATABASE_URL", "postgresql://curriculos:curriculos@localhost:5433/curriculos"
)

# "fake" (determinístico, sem custo) ou "anthropic" (SDK oficial).
AI_PROVIDER = os.environ.get("AI_PROVIDER", "fake")
ANTHROPIC_API_KEY = os.environ.get("ANTHROPIC_API_KEY", "")

# Níveis de modelo (seção 10): tarefas simples não pagam modelo caro.
MODELOS = {
    "ECONOMICO": os.environ.get("AI_MODEL_ECONOMICO", "claude-haiku-4-5"),
    "INTERMEDIARIO": os.environ.get("AI_MODEL_INTERMEDIARIO", "claude-sonnet-5"),
    "AVANCADO": os.environ.get("AI_MODEL_AVANCADO", "claude-opus-4-8"),
}

# Preço (USD por milhão de tokens de entrada/saída) para estimativa de custo.
PRECOS_POR_MTOK = {
    "claude-haiku-4-5": (1.00, 5.00),
    "claude-sonnet-5": (3.00, 15.00),
    "claude-sonnet-4-6": (3.00, 15.00),
    "claude-opus-4-8": (5.00, 25.00),
    "claude-opus-4-7": (5.00, 25.00),
}

VERSAO_PROMPT = "v1"
MAX_TENTATIVAS = 2


@dataclass(frozen=True)
class RuntimeConfig:
    """Configuração vinda do Backend Core em cada requisição (cabeçalhos X-AI-*).
    Campos vazios caem nos padrões de ambiente."""
    provider: str = ""
    api_key: str = ""
    base_url: str = ""
    modelos: dict = field(default_factory=dict)  # nivel -> modelo

    def modelo_para(self, nivel: str) -> str:
        return self.modelos.get(nivel) or MODELOS[nivel]
