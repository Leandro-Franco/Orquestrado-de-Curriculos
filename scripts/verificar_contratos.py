#!/usr/bin/env python3
"""Confere se os contratos das duas pontas continuam alinhados (ADR-005).

O serviço de IA descreve suas saídas em `ai-service/app/schemas.py` (Pydantic,
snake_case) e o Backend Core as consome em `ContratosIa.java` (records Java,
camelCase). Nada obriga os dois a evoluírem juntos — este script obriga.

Uso: python3 scripts/verificar_contratos.py   (sai com 1 se divergirem)
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
SCHEMAS = RAIZ / "ai-service/app/schemas.py"
CONTRATOS = RAIZ / "backend-core/src/main/java/br/com/curriculos/servico/ia/ContratosIa.java"

# Modelo Pydantic -> record Java. None = fora do pareamento, com o motivo.
EQUIVALENCIAS = {
    "PropostaExtraida": "PropostaExtraida",
    "ResultadoExtracao": "Extracao",
    "RequisitoExtraido": "RequisitoExtraido",
    "ResultadoAnaliseVaga": "AnaliseVaga",
    "RelacaoRequisito": "Relacao",
    "ResultadoRelacionamento": "Relacionamento",
    "ResultadoEstrategia": "Estrategia",
    "ResultadoSecao": "Secao",
    "Afirmacao": "Afirmacao",
    "ResultadoValidacao": "Validacao",
    "ResultadoResumo": "Resumo",
    "ResultadoConversa": "Conversa",
}
IGNORADOS = {"PayloadFato": "payload é dinâmico por tipo de fato (ADR-002)"}

# Campos que o harness acrescenta à proposta depois da validação Pydantic.
EXTRAS_DO_HARNESS = {"PropostaExtraida": {"modelo", "versaoPrompt"}}


def campos_pydantic(fonte: str) -> dict[str, set[str]]:
    modelos: dict[str, set[str]] = {}
    atual = None
    for linha in fonte.splitlines():
        classe = re.match(r"class (\w+)\(BaseModel\):", linha)
        if classe:
            atual = classe.group(1)
            modelos[atual] = set()
            continue
        if atual:
            campo = re.match(r"    (\w+)\s*:", linha)
            if campo:
                modelos[atual].add(campo.group(1))
            elif linha.strip() and not linha.startswith((" ", "\t")):
                atual = None
    return modelos


def campos_java(fonte: str) -> dict[str, set[str]]:
    """Extrai os componentes de cada record, respeitando parênteses aninhados."""
    records: dict[str, set[str]] = {}
    for casamento in re.finditer(r"public record (\w+)\(", fonte):
        nome = casamento.group(1)
        inicio = casamento.end()
        profundidade, fim = 1, inicio
        while profundidade and fim < len(fonte):
            profundidade += {"(": 1, ")": -1}.get(fonte[fim], 0)
            fim += 1
        corpo = fonte[inicio:fim - 1]
        corpo = re.sub(r"<[^<>]*>", "", corpo)  # remove genéricos (Map<String, Object>)
        records[nome] = {
            parte.strip().split()[-1]
            for parte in corpo.split(",")
            if parte.strip()
        }
    return records


def camel(nome: str) -> str:
    partes = nome.split("_")
    return partes[0] + "".join(p.capitalize() for p in partes[1:])


def main() -> int:
    modelos = campos_pydantic(SCHEMAS.read_text(encoding="utf-8"))
    records = campos_java(CONTRATOS.read_text(encoding="utf-8"))

    divergencias = 0
    for py_nome, motivo in IGNORADOS.items():
        print(f"  (fora do pareamento) {py_nome}: {motivo}")

    for py_nome, java_nome in EQUIVALENCIAS.items():
        if py_nome not in modelos:
            print(f"  AUSENTE  modelo Pydantic '{py_nome}' não existe mais em schemas.py")
            divergencias += 1
            continue
        if java_nome not in records:
            print(f"  AUSENTE  record Java '{java_nome}' não existe em ContratosIa.java")
            divergencias += 1
            continue

        esperados = {camel(c) for c in modelos[py_nome]} | EXTRAS_DO_HARNESS.get(py_nome, set())
        declarados = records[java_nome]
        faltando, sobrando = esperados - declarados, declarados - esperados
        if faltando or sobrando:
            divergencias += 1
            print(f"  DIVERGE  {py_nome} -> {java_nome}")
            if faltando:
                print(f"             falta no Java: {sorted(faltando)}")
            if sobrando:
                print(f"             sem par no Python: {sorted(sobrando)}")
        else:
            print(f"  ok       {py_nome} -> {java_nome} ({len(declarados)} campos)")

    if divergencias:
        print(f"\n✘ {divergencias} contrato(s) divergindo entre o serviço de IA e o Backend Core.")
        return 1
    print("\n✔ Contratos alinhados entre schemas.py e ContratosIa.java.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
