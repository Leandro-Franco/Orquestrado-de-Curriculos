# Atalhos de teste. Se seu usuário não estiver no grupo docker,
# rode os alvos que usam docker com sudo: `sudo make test-ia`.

.PHONY: test test-backend test-ia test-contratos test-e2e

test: test-contratos test-backend test-ia test-e2e

# Unitários do Backend Core (não precisam de banco nem docker)
test-backend:
	cd backend-core && mvn -q test

# Testes do serviço de IA (rodam dentro da imagem, com o provedor fake)
test-ia:
	docker compose run --rm --no-deps ai-service pytest -q

# Contratos das duas pontas alinhados: schemas.py <-> ContratosIa.java (ADR-005)
test-contratos:
	python3 scripts/verificar_contratos.py

# Ponta a ponta contra a pilha local (exige docker compose up)
test-e2e:
	python3 scripts/teste_e2e.py
