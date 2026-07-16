"""Configuração do serviço de IA — tudo por variável de ambiente.

O provedor da LLM é substituível (Contexto Mestre, seção 17) e os
segredos vivem apenas no backend; nada de chaves no frontend.
"""
import os

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
