from app import config
from app.providers.base import Provider
from app.providers.fake_provider import FakeProvider

_provider: Provider | None = None


def obter_provider() -> Provider:
    """Fábrica do provedor — a troca de provedor é uma configuração, não código."""
    global _provider
    if _provider is None:
        if config.AI_PROVIDER == "anthropic":
            from app.providers.anthropic_provider import AnthropicProvider
            _provider = AnthropicProvider()
        else:
            _provider = FakeProvider()
    return _provider
