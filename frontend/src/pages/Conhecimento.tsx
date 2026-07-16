import { useEffect, useState } from "react";
import { api } from "../api";
import { CAMPOS_POR_TIPO, TIPOS_FATO, type Evidencia, type Fato, type Payload } from "../types";

export default function Conhecimento() {
  const [fatos, setFatos] = useState<Fato[]>([]);
  const [tipo, setTipo] = useState<string>("HABILIDADE");
  const [payload, setPayload] = useState<Payload>({});
  const [evidencias, setEvidencias] = useState<Record<number, Evidencia[]>>({});
  const [erro, setErro] = useState("");

  const carregar = () =>
    api.get<Fato[]>("/api/fatos").then(setFatos).catch((e) => setErro(e.message));

  useEffect(() => { carregar(); }, []);

  async function criar() {
    setErro("");
    try {
      await api.post("/api/fatos", { tipo, payload });
      setPayload({});
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  async function remover(id: number) {
    if (!confirm("Remover este fato aprovado?")) return;
    await api.del(`/api/fatos/${id}`);
    carregar();
  }

  async function verEvidencias(id: number) {
    const lista = await api.get<Evidencia[]>(`/api/fatos/${id}/evidencias`);
    setEvidencias({ ...evidencias, [id]: lista });
  }

  const porTipo = fatos.reduce<Record<string, Fato[]>>((acc, f) => {
    (acc[f.tipo] ??= []).push(f);
    return acc;
  }, {});

  return (
    <div>
      <h2>Base de conhecimento (fatos aprovados)</h2>

      <div className="cartao">
        <h3>Adicionar fato manualmente</h3>
        <label>Tipo</label>
        <select value={tipo} onChange={(e) => { setTipo(e.target.value); setPayload({}); }}>
          {TIPOS_FATO.map((t) => <option key={t}>{t}</option>)}
        </select>
        {CAMPOS_POR_TIPO[tipo].map((campo) => (
          <div key={campo}>
            <label>{campo}</label>
            <input
              value={payload[campo] ?? ""}
              onChange={(e) => setPayload({ ...payload, [campo]: e.target.value })}
            />
          </div>
        ))}
        <button onClick={criar}>Adicionar</button>
        {erro && <p className="mensagem-erro">{erro}</p>}
      </div>

      {Object.entries(porTipo).map(([nomeTipo, lista]) => (
        <div className="cartao" key={nomeTipo}>
          <h3>{nomeTipo} ({lista.length})</h3>
          <table>
            <tbody>
              {lista.map((f) => (
                <tr key={f.id}>
                  <td>
                    {Object.entries(f.payload).map(([k, v]) => (
                      <div key={k}><strong>{k}:</strong> {String(v)}</div>
                    ))}
                    {evidencias[f.id]?.map((ev) => (
                      <div key={ev.id} className="alerta-validacao">
                        Evidência (documento {ev.documentoId}): {ev.trecho || "sem trecho"}
                      </div>
                    ))}
                  </td>
                  <td style={{ width: 220, textAlign: "right" }}>
                    <button className="secundario" onClick={() => verEvidencias(f.id)}>Evidências</button>
                    <button className="perigo" onClick={() => remover(f.id)}>Remover</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ))}
    </div>
  );
}
