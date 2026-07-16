export interface Perfil {
  nomeCompleto?: string;
  email?: string;
  telefone?: string;
  localizacao?: string;
  tituloProfissional?: string;
  objetivo?: string;
}

export type Payload = Record<string, string>;

export interface Fato {
  id: number;
  tipo: string;
  payload: Payload;
  status: string;
}

export interface Evidencia {
  id: number;
  documentoId: number;
  trecho?: string;
}

export interface Documento {
  id: number;
  titulo: string;
  origem: string;
  status: string;
  criadoEm: string;
}

export interface Proposta {
  id: number;
  acao: string;
  tipoFato: string;
  payloadAnterior?: Payload;
  payloadProposto: Payload;
  documentoOrigemId?: number;
  justificativa?: string;
  trechoEvidencia?: string;
  confianca?: number;
  status: string;
  modelo?: string;
}

export interface Vaga {
  id: number;
  titulo?: string;
  empresa?: string;
  descricaoBruta: string;
  analise?: Record<string, unknown>;
  status: string;
}

export interface RequisitoVaga {
  id: number;
  descricao: string;
  tipo: string;
  categoria?: string;
  compatibilidade?: string;
  fatosRelacionados?: number[];
  justificativa?: string;
}

export interface Curriculo {
  id: number;
  vagaId?: number;
  titulo: string;
  template: string;
  status: string;
  criadoEm: string;
}

export interface Secao {
  id: number;
  tipo: string;
  ordem: number;
  titulo?: string;
  conteudo?: string;
  fatosUtilizados?: number[];
  alertasValidacao?: string[];
}

export interface Versao {
  id: number;
  numero: number;
  nota?: string;
  criadoEm: string;
}

export interface ChamadaIa {
  id: number;
  operacao: string;
  modelo: string;
  tokensEntrada: number;
  tokensSaida: number;
  duracaoMs: number;
  custoEstimado: number;
  criadoEm: string;
}

export const TIPOS_FATO = [
  "EXPERIENCIA", "FORMACAO", "CURSO", "CERTIFICACAO",
  "PROJETO", "HABILIDADE", "IDIOMA", "LINK",
] as const;

export const CAMPOS_POR_TIPO: Record<string, string[]> = {
  EXPERIENCIA: ["cargo", "empresa", "inicio", "fim", "descricao", "resultados"],
  FORMACAO: ["curso", "instituicao", "inicio", "fim"],
  CURSO: ["nome", "instituicao"],
  CERTIFICACAO: ["nome", "instituicao"],
  PROJETO: ["nome", "descricao", "tecnologias", "url"],
  HABILIDADE: ["nome", "nivel"],
  IDIOMA: ["idioma", "nivel"],
  LINK: ["rotulo", "url"],
};
