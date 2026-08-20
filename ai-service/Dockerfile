FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt requirements-dev.txt ./
# Dependências de teste incluídas na imagem para permitir
# "docker compose run --rm ai-service pytest" (MVP local).
RUN pip install --no-cache-dir -r requirements.txt -r requirements-dev.txt
COPY app ./app
COPY tests ./tests
EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
