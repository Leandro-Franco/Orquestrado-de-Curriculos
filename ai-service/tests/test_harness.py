"""Testes do harness: registro fechado, saída estruturada, custo e rastreabilidade.
Rodam inteiramente com o provedor fake — sem rede, sem banco, sem custo."""
import pytest

from app import config, harness


def executar(nome: str, entrada: dict) -> dict:
    cfg = config.RuntimeConfig(provider="fake")
    return harness.executar(nome, entrada, cfg)


def test_operacao_fora_do_registro_e_recusada():
    with pytest.raises(KeyError):
        executar("executar-comando-livre", {})


def test_envelope_sempre_tem_result_e_usage():
    resposta = executar("resumir-documento", {"texto": "Primeira frase. Segunda frase. Terceira."})
    assert "result" in resposta and "usage" in resposta
    uso = resposta["usage"]
    for campo in ("model", "input_tokens", "output_tokens", "duration_ms",
                  "estimated_cost", "attempts", "valid"):
        assert campo in uso
    assert uso["valid"] is True
    assert uso["estimated_cost"] == 0.0  # fake nunca custa


def test_extracao_carimba_modelo_e_versao_do_prompt():
    resposta = executar("extrair-conhecimento", {
        "documento_id": 1,
        "texto": "Tenho experiência com Java e Docker desde 2020.",
        "fatos_existentes": [],
    })
    propostas = resposta["result"]["propostas"]
    assert len(propostas) >= 1
    for proposta in propostas:
        assert proposta["modelo"].startswith("fake:")
        assert proposta["versao_prompt"] == config.VERSAO_PROMPT
        assert proposta["tipo_fato"] in (
            "EXPERIENCIA", "FORMACAO", "CURSO", "CERTIFICACAO",
            "PROJETO", "HABILIDADE", "IDIOMA", "LINK")
        # payload achatado: sem campos None
        assert all(valor is not None for valor in proposta["payload"].values())


def test_extracao_nao_repropoe_habilidade_existente():
    entrada = {
        "documento_id": 1,
        "texto": "Uso Java no dia a dia.",
        "fatos_existentes": [
            {"id": 1, "tipo": "HABILIDADE", "payload": {"nome": "Java"}}],
    }
    propostas = executar("extrair-conhecimento", entrada)["result"]["propostas"]
    assert all(p["payload"].get("nome") != "Java" for p in propostas)


def test_analise_de_vaga_estrutura_requisitos():
    resposta = executar("analisar-vaga", {
        "descricao": "Vaga Dev Backend Pleno remoto.\nRequisitos: Java, Spring Boot, "
                     "PostgreSQL, Docker. Inglês desejável."})
    resultado = resposta["result"]
    assert resultado["senioridade"] == "Pleno"
    assert resultado["modalidade"] == "Remoto"
    assert len(resultado["requisitos_obrigatorios"]) >= 3
    assert "Inglês" in resultado["idiomas"]


def test_relacionamento_classifica_com_niveis_validos():
    resposta = executar("relacionar-requisitos", {
        "requisitos": [
            {"id": 1, "descricao": "Experiência com Java", "tipo": "OBRIGATORIO"},
            {"id": 2, "descricao": "Conhecimento em COBOL", "tipo": "DESEJAVEL"}],
        "fatos": [
            {"id": 10, "tipo": "HABILIDADE", "payload": {"nome": "Java"}}],
    })
    relacoes = {r["requisito_id"]: r for r in resposta["result"]["relacoes"]}
    assert relacoes[1]["compatibilidade"] in ("ALTA", "MEDIA", "PARCIAL")
    assert 10 in relacoes[1]["fatos"]
    assert relacoes[2]["compatibilidade"] == "AUSENTE"


def test_validacao_factual_aponta_afirmacao_sem_suporte():
    resposta = executar("validar-afirmacoes", {
        "conteudo": "Liderei uma equipe de vinte engenheiros aeroespaciais.",
        "fatos": [{"id": 1, "tipo": "HABILIDADE", "payload": {"nome": "Java"}}],
    })
    afirmacoes = resposta["result"]["afirmacoes"]
    assert any(not a["sustentada"] for a in afirmacoes)


def test_conversar_e_somente_leitura_e_responde():
    resposta = executar("conversar", {
        "mensagem": "o que estou vendo?",
        "contexto_tela": "Página: /vagas\nVaga Dev Backend — compatibilidade ALTA",
        "historico": [],
        "trechos": [],
    })
    assert resposta["result"]["resposta"]


def test_custo_estimado_usa_tabela_de_precos():
    # claude-haiku-4-5: US$ 1/MTok entrada, US$ 5/MTok saída
    custo = harness._custo_estimado("claude-haiku-4-5", 1_000_000, 1_000_000)
    assert custo == pytest.approx(6.0)
    assert harness._custo_estimado("fake:claude-haiku-4-5", 100, 100) == 0.0
    assert harness._custo_estimado("modelo-desconhecido", 100, 100) == 0.0


def test_modelo_por_nivel_respeita_configuracao_da_requisicao():
    cfg = config.RuntimeConfig(provider="fake", modelos={"ECONOMICO": "meu-modelo"})
    assert cfg.modelo_para("ECONOMICO") == "meu-modelo"
    assert cfg.modelo_para("AVANCADO") == config.MODELOS["AVANCADO"]
