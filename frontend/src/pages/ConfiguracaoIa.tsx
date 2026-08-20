import { useEffect, useState } from "react";
import { api } from "../api";

interface ConfigIa {
  provider: string;
  apiKeyMascarada?: string | null;
  baseUrl?: string | null;
  modeloEconomico?: string | null;
  modeloIntermediario?: string | null;
  modeloAvancado?: string | null;
}

/** Dicas por provedor: base URL e exemplos de modelos por nível. */
const DICAS: Record<string, { baseUrl: string; eco: string; inter: string; avan: string }> = {
  fake: { baseUrl: "", eco: "—", inter: "—", avan: "—" },
  anthropic: { baseUrl: "(padrão da Anthropic)", eco: "claude-haiku-4-5", inter: "claude-sonnet-5", avan: "claude-opus-4-8" },
  openai: { baseUrl: "(padrão da OpenAI)", eco: "gpt-4o-mini", inter: "gpt-4o", avan: "gpt-4o" },
  grok: { baseUrl: "https://api.x.ai/v1", eco: "grok-3-mini", inter: "grok-3", avan: "grok-3" },
  ollama: { baseUrl: "http://localhost:11434/v1", eco: "llama3.2", inter: "llama3.1:8b", avan: "llama3.1:70b" },
  personalizado: { baseUrl: "https://sua-api/v1 (obrigatória)", eco: "nome do modelo", inter: "nome do modelo", avan: "nome do modelo" },
};

export default function ConfiguracaoIa() {
  const [config, setConfig] = useState<ConfigIa>({ provider: "fake" });
  const [apiKey, setApiKey] = useState("");
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");

  useEffect(() => {
    api.get<ConfigIa>("/api/configuracao-ia").then(setConfig).catch((e) => setErro(e.message));
  }, []);

  const dica = DICAS[config.provider] ?? DICAS.personalizado;

  const campo = (chave: keyof ConfigIa, placeholder: string) => ({
    value: (config[chave] as string) ?? "",
    placeholder,
    onChange: (e: React.ChangeEvent<HTMLInputElement>) =>
      setConfig({ ...config, [chave]: e.target.value }),
  });

  async function salvar() {
    setMensagem(""); setErro("");
    try {
      const corpo: Record<string, string> = {
        provider: config.provider,
        baseUrl: config.baseUrl ?? "",
        modeloEconomico: config.modeloEconomico ?? "",
        modeloIntermediario: config.modeloIntermediario ?? "",
        modeloAvancado: config.modeloAvancado ?? "",
      };
      if (apiKey) corpo.apiKey = apiKey; // ausente = mantém a chave atual
      setConfig(await api.put<ConfigIa>("/api/configuracao-ia", corpo));
      setApiKey("");
      setMensagem("Configuração salva. As próximas chamadas de IA já usam este provedor.");
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  return (
    <div>
      <h2>Configuração de IA</h2>
      <div className="cartao">
        <p>
          A chave fica <strong>somente no backend</strong> e nunca volta ao navegador
          (aqui aparece apenas mascarada). O provedor <em>fake</em> funciona sem chave,
          para desenvolvimento.
        </p>

        <label>Provedor</label>
        <select
          value={config.provider}
          onChange={(e) => setConfig({ ...config, provider: e.target.value })}
        >
          <option value="fake">Fake (local, sem custo)</option>
          <option value="anthropic">Anthropic (Claude)</option>
          <option value="openai">OpenAI (GPT)</option>
          <option value="grok">Grok (x.ai)</option>
          <option value="ollama">Ollama / Llama (local)</option>
          <option value="personalizado">Personalizado (API compatível com OpenAI)</option>
        </select>

        {config.provider !== "fake" && (
          <>
            <label>
              Chave de API {config.apiKeyMascarada ? `— atual: ${config.apiKeyMascarada}` : ""}
            </label>
            <input
              type="password"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder={config.apiKeyMascarada
                ? "deixe em branco para manter a atual"
                : config.provider === "ollama" ? "não é necessária para Ollama local" : "cole a chave aqui"}
            />

            <label>Base URL</label>
            <input {...campo("baseUrl", dica.baseUrl)} />

            <label>Modelo econômico (classificação, resumos)</label>
            <input {...campo("modeloEconomico", dica.eco)} />
            <label>Modelo intermediário (extração, análise de vaga, chat)</label>
            <input {...campo("modeloIntermediario", dica.inter)} />
            <label>Modelo avançado (estratégia e redação do currículo)</label>
            <input {...campo("modeloAvancado", dica.avan)} />
          </>
        )}

        <button onClick={salvar}>Salvar</button>
        {mensagem && <p className="mensagem-ok">{mensagem}</p>}
        {erro && <p className="mensagem-erro">{erro}</p>}
      </div>

      <div className="cartao">
        <h3>Como funciona</h3>
        <p>
          OpenAI, Grok, Ollama e "Personalizado" usam o mesmo protocolo (OpenAI-compatible):
          para qualquer API compatível — Groq, DeepSeek, vLLM etc. — escolha "Personalizado"
          e informe a Base URL. Modelos em branco usam os padrões do servidor. O custo
          estimado nas métricas só é calculado para modelos com preço conhecido.
        </p>
      </div>
    </div>
  );
}
