import { useEffect, useState } from "react";
import { api } from "../api";
import type { Payload, Proposta } from "../types";

/** Diff visual: compara payload anterior e proposto campo a campo (seção 6, passo 12). */
function Diff({ anterior, proposto }: { anterior?: Payload; proposto: Payload }) {
  const chaves = Array.from(new Set([...Object.keys(anterior ?? {}), ...Object.keys(proposto)]));
  return (
    <div className="diff">
      <div className="coluna">
        <strong>Atual na base</strong>
        <dl>
          {chaves.map((k) => (
            <div key={k}>
              <dt>{k}</dt>
              <dd className={anterior?.[k] !== proposto[k] ? "alterado" : ""}>
                {anterior?.[k] ?? "—"}
              </dd>
            </div>
          ))}
        </dl>
      </div>
      <div className="coluna">
        <strong>Proposto pela IA</strong>
        <dl>
          {chaves.map((k) => (
            <div key={k}>
              <dt>{k}</dt>
              <dd className={anterior?.[k] !== proposto[k] ? "alterado" : ""}>
                {proposto[k] ?? "—"}
              </dd>
            </div>
          ))}
        </dl>
      </div>
    </div>
  );
}

export default function Propostas() {
  const [propostas, setPropostas] = useState<Proposta[]>([]);
  const [filtro, setFiltro] = useState("PENDENTE");
  const [erro, setErro] = useState("");

  const carregar = () =>
    api.get<Proposta[]>(`/api/propostas?status=${filtro}`)
      .then(setPropostas).catch((e) => setErro(e.message));

  useEffect(() => { carregar(); }, [filtro]);

  async function decidir(id: number, acao: "aprovar" | "rejeitar") {
    setErro("");
    try {
      await api.post(`/api/propostas/${id}/${acao}`, acao === "rejeitar" ? { motivo: "" } : undefined);
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  return (
    <div>
      <h2>Propostas de atualização</h2>
      <p>Nada entra na base oficial sem a sua aprovação.</p>
      <label>Filtrar por status</label>
      <select value={filtro} onChange={(e) => setFiltro(e.target.value)} style={{ maxWidth: 240 }}>
        <option>PENDENTE</option>
        <option>APROVADA</option>
        <option>REJEITADA</option>
      </select>
      {erro && <p className="mensagem-erro">{erro}</p>}

      {propostas.length === 0 && <div className="cartao">Nenhuma proposta {filtro.toLowerCase()}.</div>}

      {propostas.map((p) => (
        <div className="cartao" key={p.id}>
          <div>
            <span className="selo">{p.acao}</span>{" "}
            <span className="selo">{p.tipoFato}</span>{" "}
            {p.confianca != null && <span className="selo">confiança {(p.confianca * 100).toFixed(0)}%</span>}{" "}
            {p.modelo && <span className="selo">{p.modelo}</span>}
          </div>
          <Diff anterior={p.payloadAnterior} proposto={p.payloadProposto} />
          {p.justificativa && <p><strong>Justificativa:</strong> {p.justificativa}</p>}
          {p.trechoEvidencia && (
            <p className="alerta-validacao"><strong>Trecho de evidência:</strong> “{p.trechoEvidencia}”</p>
          )}
          {p.status === "PENDENTE" && (
            <div>
              <button onClick={() => decidir(p.id, "aprovar")}>Aprovar</button>
              <button className="perigo" onClick={() => decidir(p.id, "rejeitar")}>Rejeitar</button>
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
