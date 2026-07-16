import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import type { Vaga } from "../types";

export default function Vagas() {
  const [vagas, setVagas] = useState<Vaga[]>([]);
  const [titulo, setTitulo] = useState("");
  const [empresa, setEmpresa] = useState("");
  const [descricao, setDescricao] = useState("");
  const [erro, setErro] = useState("");

  const carregar = () =>
    api.get<Vaga[]>("/api/vagas").then(setVagas).catch((e) => setErro(e.message));

  useEffect(() => { carregar(); }, []);

  async function criar() {
    setErro("");
    try {
      await api.post("/api/vagas", { titulo, empresa, descricao });
      setTitulo(""); setEmpresa(""); setDescricao("");
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  return (
    <div>
      <h2>Vagas</h2>

      <div className="cartao">
        <h3>Cadastrar vaga</h3>
        <label>Título (opcional — pode ser extraído da descrição)</label>
        <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        <label>Empresa (opcional)</label>
        <input value={empresa} onChange={(e) => setEmpresa(e.target.value)} />
        <label>Descrição completa da vaga</label>
        <textarea value={descricao} onChange={(e) => setDescricao(e.target.value)} />
        <button onClick={criar}>Cadastrar</button>
        {erro && <p className="mensagem-erro">{erro}</p>}
      </div>

      <div className="cartao">
        <h3>Vagas cadastradas</h3>
        <table>
          <thead>
            <tr><th>Título</th><th>Empresa</th><th>Status</th><th></th></tr>
          </thead>
          <tbody>
            {vagas.map((v) => (
              <tr key={v.id}>
                <td>{v.titulo || "(sem título)"}</td>
                <td>{v.empresa || "—"}</td>
                <td><span className="selo">{v.status}</span></td>
                <td><Link to={`/vagas/${v.id}`}>Abrir</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
