#!/usr/bin/env python3
"""Teste de ponta a ponta do caminho feliz, contra a pilha local em execução.

Fluxo: importar documento -> propostas -> aprovar -> vaga -> análise ->
compatibilidade -> gerar currículo -> preview -> PDF (1 página e 2+ páginas,
cobrindo a PoC de renderização da seção 12) -> versões.

Tudo o que o teste cria é removido ao final (fatos, documento, vaga,
currículo); propostas decididas permanecem como trilha de auditoria.

Requisitos: pilha no ar (docker compose up) e somente a stdlib do Python.
Uso: python3 scripts/teste_e2e.py
"""
import json
import sys
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080"
MARCA = f"E2E-{int(time.time())}"

criados = {"fatos": [], "documento": None, "vaga": None, "curriculo": None}
passos_ok = 0


def chamar(metodo: str, rota: str, corpo=None, binario: bool = False):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    requisicao = urllib.request.Request(
        BASE + rota, data=dados, method=metodo,
        headers={"Content-Type": "application/json"} if dados else {})
    with urllib.request.urlopen(requisicao, timeout=300) as resposta:
        conteudo = resposta.read()
        if binario:
            return conteudo
        return json.loads(conteudo) if conteudo else None


def passo(descricao: str, condicao: bool, detalhe: str = ""):
    global passos_ok
    situacao = "ok " if condicao else "FALHOU"
    print(f"  [{situacao}] {descricao}" + (f" — {detalhe}" if detalhe else ""))
    if not condicao:
        raise AssertionError(descricao)
    passos_ok += 1


def executar():
    # 1. Importação de documento -> propostas pendentes
    doc = chamar("POST", "/api/documentos/texto", {
        "titulo": f"{MARCA} currículo de teste",
        "conteudo": (f"{MARCA}\nExperiência profissional com Java, Spring Boot e "
                     "Docker desde 2020, trabalhando com PostgreSQL e Git."),
    })
    criados["documento"] = doc["documentoId"]
    passo("importar documento gera propostas", doc["propostasGeradas"] >= 1,
          f"{doc['propostasGeradas']} proposta(s)")

    # 2. Aprovação humana -> fato oficial
    pendentes = [p for p in chamar("GET", "/api/propostas?status=PENDENTE")
                 if p.get("documentoOrigemId") == criados["documento"]]
    passo("propostas pendentes localizadas", len(pendentes) >= 1)

    fato = chamar("POST", f"/api/propostas/{pendentes[0]['id']}/aprovar")
    criados["fatos"].append(fato["id"])
    passo("aprovar proposta cria fato aprovado", fato["status"] == "APROVADO",
          f"fato #{fato['id']} ({fato['tipo']})")

    evidencias = chamar("GET", f"/api/fatos/{fato['id']}/evidencias")
    passo("fato aprovado tem evidência vinculada ao documento",
          any(e["documentoId"] == criados["documento"] for e in evidencias))

    for proposta in pendentes[1:]:
        chamar("POST", f"/api/propostas/{proposta['id']}/rejeitar",
               {"motivo": "encerramento do teste E2E"})

    # 3. Vaga -> análise -> compatibilidade
    vaga = chamar("POST", "/api/vagas", {
        "titulo": f"{MARCA} Dev Backend", "empresa": "Empresa Teste",
        "descricao": ("Vaga Pleno remoto para desenvolvedor backend.\n"
                      "Requisitos: Java, Spring Boot, PostgreSQL, Docker.\n"
                      "Inglês desejável."),
    })
    criados["vaga"] = vaga["id"]
    vaga = chamar("POST", f"/api/vagas/{vaga['id']}/analisar")
    passo("análise estrutura a vaga", vaga["status"] == "ANALISADA")

    requisitos = chamar("GET", f"/api/vagas/{criados['vaga']}/requisitos")
    niveis_validos = {"ALTA", "MEDIA", "PARCIAL", "AUSENTE", "INCONCLUSIVA"}
    passo("requisitos extraídos e classificados",
          len(requisitos) >= 1 and all(r["compatibilidade"] in niveis_validos for r in requisitos),
          f"{len(requisitos)} requisito(s)")

    # 4. Geração do currículo
    curriculo = chamar("POST", "/api/curriculos", {
        "vagaId": criados["vaga"], "titulo": f"{MARCA} currículo dirigido",
        "template": "classico"})
    criados["curriculo"] = curriculo["id"]
    passo("currículo gerado", curriculo["status"] == "GERADO")

    detalhe = chamar("GET", f"/api/curriculos/{criados['curriculo']}")
    secoes = detalhe["secoes"]
    passo("currículo tem as seções padrão", len(secoes) == 8, f"{len(secoes)} seções")

    # 5. Preview e PDF — mesma origem visual (PoC da seção 12)
    html = chamar("GET", f"/api/curriculos/{criados['curriculo']}/preview", binario=True)
    passo("preview renderiza HTML", b"<h1" in html and b"@page" in html)

    pdf_curto = chamar("GET", f"/api/curriculos/{criados['curriculo']}/pdf", binario=True)
    passo("PDF exportado (currículo curto)",
          pdf_curto[:5] == b"%PDF-" and len(pdf_curto) > 5_000,
          f"{len(pdf_curto)} bytes")

    # Conteúdo excessivo + título longo -> deve continuar gerando PDF válido
    secao_exp = next(s for s in secoes if s["tipo"] == "EXPERIENCIAS")
    linhas = "\n".join(
        f"Responsabilidade {i}: manutenção e evolução de sistemas corporativos "
        "com integrações, testes automatizados e observabilidade em produção."
        for i in range(1, 81))
    chamar("PUT", f"/api/curriculos/{criados['curriculo']}/secoes/{secao_exp['id']}", {
        "titulo": "Experiência Profissional Extremamente Detalhada Para Teste de "
                  "Quebra de Página em Múltiplas Páginas",
        "conteudo": linhas})
    pdf_longo = chamar("GET", f"/api/curriculos/{criados['curriculo']}/pdf", binario=True)
    passo("PDF exportado (conteúdo excessivo, 2+ páginas)",
          pdf_longo[:5] == b"%PDF-" and len(pdf_longo) > len(pdf_curto),
          f"{len(pdf_longo)} bytes")

    # 6. Versionamento
    versoes = chamar("GET", f"/api/curriculos/{criados['curriculo']}/versoes")
    passo("edição gerou nova versão no histórico", len(versoes) >= 2,
          f"{len(versoes)} versão(ões)")

    # 7. Métricas registradas
    metricas = chamar("GET", "/api/metricas/ia")
    passo("chamadas de IA registradas nas métricas", len(metricas["ultimas"]) >= 1)


def limpar():
    print("\nLimpeza (removendo tudo que o teste criou):")
    def tentar(descricao, metodo, rota):
        try:
            chamar(metodo, rota)
            print(f"  [ok ] {descricao}")
        except Exception as erro:
            print(f"  [aviso] {descricao}: {erro}")

    if criados["curriculo"]:
        tentar(f"currículo #{criados['curriculo']}", "DELETE",
               f"/api/curriculos/{criados['curriculo']}")
    if criados["vaga"]:
        tentar(f"vaga #{criados['vaga']}", "DELETE", f"/api/vagas/{criados['vaga']}")
    for fato_id in criados["fatos"]:
        tentar(f"fato #{fato_id}", "DELETE", f"/api/fatos/{fato_id}")
    if criados["documento"]:
        tentar(f"documento #{criados['documento']}", "DELETE",
               f"/api/documentos/{criados['documento']}")


if __name__ == "__main__":
    print(f"Teste E2E ({MARCA}) contra {BASE}\n")
    try:
        executar()
        print(f"\n✔ E2E PASSOU — {passos_ok} verificações.")
        codigo = 0
    except AssertionError:
        print("\n✘ E2E FALHOU.")
        codigo = 1
    except urllib.error.URLError as erro:
        print(f"\n✘ Não foi possível falar com {BASE}: {erro}\n"
              "  A pilha está no ar? (docker compose up)")
        codigo = 2
    finally:
        limpar()
    sys.exit(codigo)
