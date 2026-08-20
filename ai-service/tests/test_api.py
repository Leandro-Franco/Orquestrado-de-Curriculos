"""Testes da superfície HTTP do serviço de IA (FastAPI TestClient)."""
from fastapi.testclient import TestClient

from app.main import app

cliente = TestClient(app)

# Todos os testes usam o provedor fake via cabeçalho — sem rede, sem custo.
FAKE = {"X-AI-Provider": "fake"}


def test_health_expoe_provider_e_modelos():
    resposta = cliente.get("/health")
    assert resposta.status_code == 200
    corpo = resposta.json()
    assert "provider" in corpo and "modelos" in corpo


def test_registro_de_operacoes_e_fechado_e_conhecido():
    resposta = cliente.get("/v1/operations")
    assert resposta.status_code == 200
    operacoes = set(resposta.json())
    assert operacoes == {
        "extrair-conhecimento", "analisar-vaga", "relacionar-requisitos",
        "gerar-estrategia", "gerar-secao", "validar-afirmacoes",
        "resumir-documento", "conversar",
    }


def test_operacao_desconhecida_retorna_404():
    resposta = cliente.post("/v1/operations/executar-sql", json={}, headers=FAKE)
    assert resposta.status_code == 404


def test_operacao_valida_retorna_envelope_completo():
    resposta = cliente.post("/v1/operations/resumir-documento",
                            json={"texto": "Uma frase. Outra frase."}, headers=FAKE)
    assert resposta.status_code == 200
    corpo = resposta.json()
    assert corpo["result"]["resumo"]
    assert corpo["usage"]["attempts"] >= 1


def test_cabecalhos_x_ai_sobrepoem_o_modelo_por_nivel():
    resposta = cliente.post(
        "/v1/operations/resumir-documento",
        json={"texto": "Frase."},
        headers={**FAKE, "X-AI-Modelo-Economico": "modelo-de-teste"})
    assert resposta.status_code == 200
    # o provedor fake prefixa o nome do modelo solicitado
    assert resposta.json()["usage"]["model"] == "fake:modelo-de-teste"


def test_conversar_via_api():
    resposta = cliente.post("/v1/operations/conversar", json={
        "mensagem": "resuma esta tela",
        "contexto_tela": "Propostas pendentes: 2",
        "historico": [{"papel": "usuario", "texto": "oi"}],
        "trechos": [],
    }, headers=FAKE)
    assert resposta.status_code == 200
    assert resposta.json()["result"]["resposta"]
