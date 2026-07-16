"""Contratos estruturados de cada operação do harness.

Toda resposta da LLM precisa validar contra um destes schemas antes de
voltar ao Backend Core (Contexto Mestre, seção 9: "exigir respostas
estruturadas; validar saídas").
"""
from typing import Literal, Optional

from pydantic import BaseModel, Field

TipoFato = Literal[
    "EXPERIENCIA", "FORMACAO", "CURSO", "CERTIFICACAO",
    "PROJETO", "HABILIDADE", "IDIOMA", "LINK",
]

Compatibilidade = Literal["ALTA", "MEDIA", "PARCIAL", "AUSENTE", "INCONCLUSIVA"]


class PayloadFato(BaseModel):
    """Campos possíveis de um fato. Campos não usados ficam None e são
    descartados — mantém o JSON Schema fechado (additionalProperties: false),
    exigido pelas saídas estruturadas da API."""
    cargo: Optional[str] = None
    empresa: Optional[str] = None
    inicio: Optional[str] = None
    fim: Optional[str] = None
    descricao: Optional[str] = None
    resultados: Optional[str] = None
    nome: Optional[str] = None
    instituicao: Optional[str] = None
    curso: Optional[str] = None
    nivel: Optional[str] = None
    idioma: Optional[str] = None
    rotulo: Optional[str] = None
    url: Optional[str] = None
    tecnologias: Optional[str] = None


class PropostaExtraida(BaseModel):
    acao: Literal["CRIAR", "ATUALIZAR"] = "CRIAR"
    tipo_fato: TipoFato
    payload: PayloadFato
    fato_alvo_id: Optional[int] = None
    justificativa: str = ""
    trecho_evidencia: str = ""
    confianca: float = Field(default=0.5, ge=0, le=1)


class ResultadoExtracao(BaseModel):
    propostas: list[PropostaExtraida] = []


class RequisitoExtraido(BaseModel):
    descricao: str
    categoria: str = ""


class ResultadoAnaliseVaga(BaseModel):
    titulo: str = ""
    empresa: str = ""
    senioridade: str = ""
    modalidade: str = ""
    localizacao: str = ""
    responsabilidades: list[str] = []
    requisitos_obrigatorios: list[RequisitoExtraido] = []
    requisitos_desejaveis: list[RequisitoExtraido] = []
    tecnologias: list[str] = []
    idiomas: list[str] = []
    palavras_chave: list[str] = []
    competencias_comportamentais: list[str] = []
    criterios_eliminatorios: list[str] = []


class RelacaoRequisito(BaseModel):
    requisito_id: int
    compatibilidade: Compatibilidade
    fatos: list[int] = []
    justificativa: str = ""


class ResultadoRelacionamento(BaseModel):
    relacoes: list[RelacaoRequisito] = []


class ResultadoEstrategia(BaseModel):
    posicionamento: str = ""
    competencias_prioritarias: list[str] = []
    experiencias_relevantes: list[int] = []
    projetos_destaque: list[int] = []
    informacoes_secundarias: list[str] = []
    lacunas: list[str] = []
    tom: str = "profissional e direto"


class ResultadoSecao(BaseModel):
    titulo: str = ""
    conteudo: str = ""
    fatos_utilizados: list[int] = []


class Afirmacao(BaseModel):
    texto: str
    sustentada: bool
    fatos: list[int] = []
    nota: str = ""


class ResultadoValidacao(BaseModel):
    afirmacoes: list[Afirmacao] = []


class ResultadoResumo(BaseModel):
    resumo: str = ""
