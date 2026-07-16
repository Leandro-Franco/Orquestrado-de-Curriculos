# ADR-001 — Renderização e exportação em PDF

**Status:** Aceita (definida no Contexto Mestre, seção 12)

## Contexto
Preview e PDF precisam ser visualmente idênticos, e o PDF é decisão arquitetural desde o início.

## Decisão
- Thymeleaf gera o HTML do currículo no Backend Core, a partir do modelo estruturado do banco.
- O mesmo HTML/CSS serve para o preview (iframe no frontend) e para o PDF.
- Playwright Java controla um Chromium headless que imprime o HTML em PDF A4.
- O frontend não mantém implementação visual própria do currículo.

## Consequências
- Um único ponto de verdade visual (template Thymeleaf + CSS de impressão).
- O backend precisa do Chromium disponível (imagem Docker `mcr.microsoft.com/playwright/java`, versões fixadas; localmente `mvn exec` instala o browser).
- CSS de impressão controla margens, quebras de página e blocos indivisíveis (`break-inside: avoid`).
