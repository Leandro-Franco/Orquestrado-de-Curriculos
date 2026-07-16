from abc import ABC, abstractmethod
from dataclasses import dataclass

from pydantic import BaseModel


@dataclass
class Uso:
    model: str
    input_tokens: int = 0
    output_tokens: int = 0
    cached_tokens: int = 0


class Provider(ABC):
    """Contrato mínimo de um provedor de LLM: recebe prompts + schema,
    devolve uma instância validada do schema e o uso de tokens."""

    @abstractmethod
    def gerar(self, operacao: str, entrada: dict, system: str, user: str,
              schema: type[BaseModel], modelo: str, max_tokens: int) -> tuple[BaseModel, Uso]:
        ...
