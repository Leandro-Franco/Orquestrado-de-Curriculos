import { useEffect, useState } from "react";
import { api } from "../api";
import type { ChamadaIa } from "../types";

interface Resumo {
  operacao: string;
  modelo: string;
  chamadas: number;
  tokens_entrada: number;
  tokens_saida: number;
  tokens_cache: number;
  custo_estimado: number;
  duracao_media_ms: number;
}

export default function Metricas() {
  const [resumo, setResumo] = useState<Resumo[]>([]);
  const [ultimas, setUltimas] = useState<ChamadaIa[]>([]);
  const [erro, setErro] = useState("");

  useEffect(() => {
    api.get<{ resumo: Resumo[]; ultimas: ChamadaIa[] }>("/api/metricas/ia")
      .then((d) => { setResumo(d.resumo); setUltimas(d.ultimas); })
      .catch((e) => setErro(e.message));
  }, []);

  const custoTotal = resumo.reduce((soma, r) => soma + Number(r.custo_estimado), 0);

  return (
    <div>
      <h2>Métricas de IA</h2>
      {erro && <p className="mensagem-erro">{erro}</p>}

      <div className="cartao">
        <h3>Custo estimado total: US$ {custoTotal.toFixed(4)}</h3>
        <table>
          <thead>
            <tr><th>Operação</th><th>Modelo</th><th>Chamadas</th><th>Tokens in</th>
                <th>Tokens out</th><th>Cache</th><th>Custo (US$)</th><th>Duração média</th></tr>
          </thead>
          <tbody>
            {resumo.map((r, i) => (
              <tr key={i}>
                <td>{r.operacao}</td>
                <td>{r.modelo}</td>
                <td>{r.chamadas}</td>
                <td>{r.tokens_entrada}</td>
                <td>{r.tokens_saida}</td>
                <td>{r.tokens_cache}</td>
                <td>{Number(r.custo_estimado).toFixed(4)}</td>
                <td>{Math.round(Number(r.duracao_media_ms))} ms</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cartao">
        <h3>Últimas chamadas</h3>
        <table>
          <thead>
            <tr><th>Quando</th><th>Operação</th><th>Modelo</th><th>Tokens</th><th>Duração</th></tr>
          </thead>
          <tbody>
            {ultimas.map((c) => (
              <tr key={c.id}>
                <td>{new Date(c.criadoEm).toLocaleString("pt-BR")}</td>
                <td>{c.operacao}</td>
                <td>{c.modelo}</td>
                <td>{c.tokensEntrada} → {c.tokensSaida}</td>
                <td>{c.duracaoMs} ms</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
