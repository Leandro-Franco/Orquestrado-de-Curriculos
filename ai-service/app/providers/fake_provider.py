"""Provedor determinístico para desenvolvimento local sem chave de API.

Produz saídas plausíveis (e sempre válidas contra os schemas) usando
heurísticas simples. Não é inteligente de propósito: serve para exercitar
o fluxo completo — extração → proposta → aprovação → vaga → currículo →
PDF — sem custo e sem rede.
"""
import re

from pydantic import BaseModel

from app import schemas
from app.providers.base import Provider, Uso

TECNOLOGIAS_CONHECIDAS = [
    "Java", "Python", "TypeScript", "JavaScript", "React", "Angular", "Vue",
    "Spring", "Spring Boot", "FastAPI", "Django", "Node", "PostgreSQL", "MySQL",
    "MongoDB", "Redis", "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Git",
    "Linux", "SQL", "Kafka", "REST", "GraphQL", "CI/CD", "Terraform",
]

_PALAVRAS_IRRELEVANTES = {
    "de", "da", "do", "das", "dos", "em", "para", "com", "e", "ou", "a", "o",
    "as", "os", "um", "uma", "que", "na", "no", "por", "ser", "ter",
}


def _tokens(texto: str) -> set[str]:
    return {t for t in re.findall(r"[\wÀ-ÿ+#./-]{3,}", (texto or "").lower())
            if t not in _PALAVRAS_IRRELEVANTES}


def _texto_do_fato(fato: dict) -> str:
    return " ".join(str(v) for v in (fato.get("payload") or {}).values() if v)


class FakeProvider(Provider):

    def gerar(self, operacao: str, entrada: dict, system: str, user: str,
              schema: type[BaseModel], modelo: str, max_tokens: int) -> tuple[BaseModel, Uso]:
        despacho = {
            "extrair-conhecimento": self._extrair,
            "analisar-vaga": self._analisar_vaga,
            "relacionar-requisitos": self._relacionar,
            "gerar-estrategia": self._estrategia,
            "gerar-secao": self._secao,
            "validar-afirmacoes": self._validar,
            "resumir-documento": self._resumir,
        }
        resultado = despacho[operacao](entrada)
        tamanho = len(system) + len(user)
        uso = Uso(model=f"fake:{modelo}", input_tokens=tamanho // 4,
                  output_tokens=max(1, len(str(resultado)) // 4))
        return resultado, uso

    # ------------------------------------------------------------------

    def _extrair(self, entrada: dict) -> schemas.ResultadoExtracao:
        texto = entrada.get("texto", "")
        existentes = entrada.get("fatos_existentes", [])
        nomes_existentes = {
            str((f.get("payload") or {}).get("nome", "")).lower()
            for f in existentes if f.get("tipo") == "HABILIDADE"
        }
        propostas = []
        for tec in TECNOLOGIAS_CONHECIDAS:
            padrao = r"(?<![\w])" + re.escape(tec.lower()) + r"(?![\w])"
            if re.search(padrao, texto.lower()) and tec.lower() not in nomes_existentes:
                propostas.append(schemas.PropostaExtraida(
                    tipo_fato="HABILIDADE",
                    payload=schemas.PayloadFato(nome=tec, nivel="não informado"),
                    justificativa=f"Tecnologia '{tec}' mencionada no documento (provedor fake).",
                    trecho_evidencia=self._trecho_com(texto, tec),
                    confianca=0.6,
                ))
        anos = re.findall(r"\b(?:19|20)\d{2}\b", texto)
        if anos and ("experi" in texto.lower() or "trabalh" in texto.lower()):
            propostas.append(schemas.PropostaExtraida(
                tipo_fato="EXPERIENCIA",
                payload=schemas.PayloadFato(
                    cargo="(revisar) Cargo identificado no documento",
                    empresa="(revisar) Empresa identificada no documento",
                    inicio=anos[0],
                    descricao=texto.strip().splitlines()[0][:200],
                ),
                justificativa="Indícios de experiência profissional; revise antes de aprovar (provedor fake).",
                trecho_evidencia=texto.strip()[:200],
                confianca=0.3,
            ))
        return schemas.ResultadoExtracao(propostas=propostas[:12])

    def _analisar_vaga(self, entrada: dict) -> schemas.ResultadoAnaliseVaga:
        descricao = entrada.get("descricao", "")
        baixo = descricao.lower()
        linhas = [l.strip() for l in descricao.splitlines() if l.strip()]
        tecnologias = [t for t in TECNOLOGIAS_CONHECIDAS
                       if re.search(r"(?<![\w])" + re.escape(t.lower()) + r"(?![\w])", baixo)]
        senioridade = next((s for s, k in [("Sênior", "sênior"), ("Sênior", "senior"),
                                           ("Pleno", "pleno"), ("Júnior", "júnior"),
                                           ("Júnior", "junior")] if k in baixo), "")
        modalidade = next((m for m, k in [("Remoto", "remoto"), ("Híbrido", "híbrido"),
                                          ("Presencial", "presencial")] if k in baixo), "")
        idiomas = [i for i, k in [("Inglês", "inglês"), ("Inglês", "ingles"),
                                  ("Espanhol", "espanhol")] if k in baixo]
        obrigatorios = [schemas.RequisitoExtraido(descricao=f"Experiência com {t}", categoria="tecnologia")
                        for t in tecnologias[:8]]
        desejaveis = [schemas.RequisitoExtraido(descricao=f"Conhecimento em {t}", categoria="tecnologia")
                      for t in tecnologias[8:12]]
        for idioma in idiomas:
            desejaveis.append(schemas.RequisitoExtraido(descricao=idioma, categoria="idioma"))
        return schemas.ResultadoAnaliseVaga(
            titulo=linhas[0][:120] if linhas else "",
            senioridade=senioridade,
            modalidade=modalidade,
            tecnologias=tecnologias,
            idiomas=idiomas,
            palavras_chave=tecnologias[:10],
            requisitos_obrigatorios=obrigatorios,
            requisitos_desejaveis=desejaveis,
        )

    def _relacionar(self, entrada: dict) -> schemas.ResultadoRelacionamento:
        relacoes = []
        for req in entrada.get("requisitos", []):
            tokens_req = _tokens(req.get("descricao", ""))
            candidatos = []
            for fato in entrada.get("fatos", []):
                comuns = tokens_req & _tokens(_texto_do_fato(fato))
                if comuns:
                    candidatos.append((len(comuns), fato["id"]))
            candidatos.sort(reverse=True)
            if not candidatos:
                nivel = "AUSENTE"
            elif candidatos[0][0] >= 2:
                nivel = "ALTA"
            else:
                nivel = "PARCIAL"
            relacoes.append(schemas.RelacaoRequisito(
                requisito_id=req["id"],
                compatibilidade=nivel,
                fatos=[c[1] for c in candidatos[:3]],
                justificativa="Correspondência por sobreposição de termos (provedor fake).",
            ))
        return schemas.ResultadoRelacionamento(relacoes=relacoes)

    def _estrategia(self, entrada: dict) -> schemas.ResultadoEstrategia:
        fatos = entrada.get("fatos", [])
        habilidades = [(f.get("payload") or {}).get("nome") for f in fatos
                       if f.get("tipo") == "HABILIDADE"]
        experiencias = [f["id"] for f in fatos if f.get("tipo") == "EXPERIENCIA"]
        projetos = [f["id"] for f in fatos if f.get("tipo") == "PROJETO"]
        lacunas = [c.get("descricao", "") for c in entrada.get("compatibilidade", [])
                   if c.get("compatibilidade") == "AUSENTE"]
        perfil = entrada.get("perfil", {})
        return schemas.ResultadoEstrategia(
            posicionamento=perfil.get("titulo_profissional") or "Profissional de tecnologia",
            competencias_prioritarias=[h for h in habilidades if h][:6],
            experiencias_relevantes=experiencias[:4],
            projetos_destaque=projetos[:3],
            lacunas=lacunas[:5],
        )

    def _secao(self, entrada: dict) -> schemas.ResultadoSecao:
        tipo = entrada.get("tipo_secao", "")
        fatos = entrada.get("fatos", [])
        perfil = entrada.get("perfil", {})
        estrategia = entrada.get("estrategia", {})

        def do_tipo(t: str) -> list[dict]:
            return [f for f in fatos if f.get("tipo") == t]

        if tipo == "TITULO":
            conteudo = estrategia.get("posicionamento") or perfil.get("titulo_profissional") or ""
            return schemas.ResultadoSecao(titulo="Título", conteudo=conteudo)
        if tipo == "RESUMO":
            competencias = ", ".join(estrategia.get("competencias_prioritarias", [])[:5])
            conteudo = (f"{estrategia.get('posicionamento', 'Profissional')} com atuação em "
                        f"{competencias or 'tecnologia'}.")
            return schemas.ResultadoSecao(titulo="Resumo", conteudo=conteudo,
                                          fatos_utilizados=[f["id"] for f in do_tipo("HABILIDADE")[:5]])
        mapa = {
            "EXPERIENCIAS": ("Experiência Profissional", "EXPERIENCIA",
                             lambda p: f"{p.get('cargo', '')} — {p.get('empresa', '')} "
                                       f"({p.get('inicio', '')}–{p.get('fim') or 'atual'})\n"
                                       f"{p.get('descricao', '')}"),
            "PROJETOS": ("Projetos", "PROJETO",
                         lambda p: f"{p.get('nome', '')}: {p.get('descricao', '')}"),
            "COMPETENCIAS": ("Competências", "HABILIDADE", lambda p: p.get("nome", "")),
            "FORMACAO": ("Formação Acadêmica", "FORMACAO",
                         lambda p: f"{p.get('curso', '')} — {p.get('instituicao', '')}"),
            "CURSOS": ("Cursos e Certificações", "CURSO",
                       lambda p: f"{p.get('nome', '')}"
                                 + (f" — {p['instituicao']}" if p.get("instituicao") else "")),
            "IDIOMAS": ("Idiomas", "IDIOMA",
                        lambda p: f"{p.get('idioma', '')} — {p.get('nivel', '')}"),
        }
        if tipo in mapa:
            titulo, tipo_fato, formatar = mapa[tipo]
            selecionados = do_tipo(tipo_fato)
            if tipo == "CURSOS":
                selecionados = selecionados + do_tipo("CERTIFICACAO")
            linhas = [formatar(f.get("payload") or {}).strip() for f in selecionados]
            return schemas.ResultadoSecao(
                titulo=titulo,
                conteudo="\n".join(l for l in linhas if l),
                fatos_utilizados=[f["id"] for f in selecionados],
            )
        return schemas.ResultadoSecao(titulo=tipo.title(), conteudo="")

    def _validar(self, entrada: dict) -> schemas.ResultadoValidacao:
        conteudo = entrada.get("conteudo", "")
        fatos = entrada.get("fatos", [])
        afirmacoes = []
        for frase in [f.strip() for f in re.split(r"[.\n]", conteudo) if len(f.strip()) > 15]:
            tokens_frase = _tokens(frase)
            suportes = [f["id"] for f in fatos if tokens_frase & _tokens(_texto_do_fato(f))]
            afirmacoes.append(schemas.Afirmacao(
                texto=frase[:160],
                sustentada=bool(suportes),
                fatos=suportes[:3],
                nota="" if suportes else "Nenhum fato aprovado cobre esta afirmação (provedor fake).",
            ))
        return schemas.ResultadoValidacao(afirmacoes=afirmacoes[:20])

    def _resumir(self, entrada: dict) -> schemas.ResultadoResumo:
        texto = entrada.get("texto", "")
        frases = re.split(r"(?<=[.!?])\s+", texto.strip())
        return schemas.ResultadoResumo(resumo=" ".join(frases[:2])[:500])

    @staticmethod
    def _trecho_com(texto: str, termo: str) -> str:
        indice = texto.lower().find(termo.lower())
        if indice < 0:
            return ""
        inicio = max(0, indice - 60)
        return texto[inicio:indice + len(termo) + 60].strip()
