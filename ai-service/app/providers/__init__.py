from app import config
from app.providers.base import Provider
from app.providers.fake_provider import FakeProvider

# Cache de provedores por configuração (evita recriar cliente a cada chamada).
_cache: dict[tuple, Provider] = {}


def obter_provider(cfg: config.RuntimeConfig | None = None) -> Provider:
    """Fábrica do provedor — a troca é configuração (ambiente ou requisição),
    nunca código. Sem configuração explícita, vale o padrão de ambiente."""
    nome = (cfg.provider if cfg and cfg.provider else config.AI_PROVIDER)
    api_key = (cfg.api_key if cfg and cfg.api_key else config.ANTHROPIC_API_KEY)
    base_url = cfg.base_url if cfg else ""

    chave_cache = (nome, api_key, base_url)
    if chave_cache in _cache:
        return _cache[chave_cache]

    if nome == "anthropic":
        from app.providers.anthropic_provider import AnthropicProvider
        provider: Provider = AnthropicProvider(api_key=api_key)
    elif nome in ("openai", "grok", "ollama", "personalizado"):
        from app.providers.openai_provider import OpenAICompatProvider
        provider = OpenAICompatProvider(nome, api_key=api_key, base_url=base_url)
    else:
        provider = FakeProvider()

    _cache[chave_cache] = provider
    return provider
