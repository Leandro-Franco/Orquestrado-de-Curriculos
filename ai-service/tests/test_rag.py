"""Testes das funções puras do RAG (embedding e fatiamento) — sem banco."""
import math

from app import rag


def test_embedding_e_deterministico_e_normalizado():
    a = rag.embutir("desenvolvedor java com spring boot")
    b = rag.embutir("desenvolvedor java com spring boot")
    assert a == b
    assert len(a) == rag.DIMENSOES
    norma = math.sqrt(sum(v * v for v in a))
    assert norma == aproximadamente(1.0)


def test_textos_parecidos_ficam_mais_proximos_que_diferentes():
    consulta = rag.embutir("experiência com java e spring")
    parecido = rag.embutir("três anos de java, spring e apis")
    diferente = rag.embutir("culinária vegana e jardinagem urbana")
    assert _cosseno(consulta, parecido) > _cosseno(consulta, diferente)


def test_fatiamento_respeita_tamanho_e_sobreposicao():
    texto = "x" * 2000
    trechos = rag.fatiar(texto)
    assert all(len(t) <= rag.TAMANHO_TRECHO for t in trechos)
    assert len(trechos) >= 2
    # sobreposição: o fim de um trecho reaparece no início do seguinte
    assert trechos[0][-rag.SOBREPOSICAO:] == trechos[1][:rag.SOBREPOSICAO]


def test_texto_curto_vira_um_unico_trecho_e_vazio_nenhum():
    assert rag.fatiar("curto") == ["curto"]
    assert rag.fatiar("   ") == []


def _cosseno(a: list[float], b: list[float]) -> float:
    return sum(x * y for x, y in zip(a, b))


def aproximadamente(valor: float, tolerancia: float = 1e-6):
    class _Aproximado:
        def __eq__(self, outro):
            return abs(outro - valor) < tolerancia
    return _Aproximado()
