import { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { api } from "../api";
import type { Curriculo, Vaga } from "../types";

export default function Curriculos() {
  const [curriculos, setCurriculos] = useState<Curriculo[]>([]);
  const [vagas, setVagas] = useState<Vaga[]>([]);
  const estadoRota = useLocation().state as { vagaId?: number } | null;
  const [vagaId, setVagaId] = useState<string>(estadoRota?.vagaId ? String(estadoRota.vagaId) : "");
  const [titulo, setTitulo] = useState("");
  const [gerando, setGerando] = useState(false);
  const [erro, setErro] = useState("");

  const carregar = async () => {
    setCurriculos(await api.get<Curriculo[]>("/api/curriculos"));
    setVagas(await api.get<Vaga[]>("/api/vagas"));
  };

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, []);

  async function gerar() {
    setGerando(true); setErro("");
    try {
      await api.post("/api/curriculos", {
        vagaId: vagaId ? Number(vagaId) : null,
        titulo,
        template: "classico",
      });
      setTitulo("");
      await carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setGerando(false);
    }
  }

  return (
    <div>
      <h2>Currículos</h2>

      <div className="cartao">
        <h3>Gerar novo currículo</h3>
        <p>A geração usa somente fatos aprovados; a vaga direciona a estratégia.</p>
        <label>Vaga (opcional)</label>
        <select value={vagaId} onChange={(e) => setVagaId(e.target.value)}>
          <option value="">Currículo geral (sem vaga)</option>
          {vagas.filter((v) => v.status === "ANALISADA").map((v) => (
            <option key={v.id} value={v.id}>{v.titulo || `Vaga ${v.id}`} {v.empresa ? `— ${v.empresa}` : ""}</option>
          ))}
        </select>
        <label>Título interno</label>
        <input value={titulo} onChange={(e) => setTitulo(e.target.value)}
          placeholder="ex.: Currículo — Desenvolvedor Backend na Empresa X" />
        <button onClick={gerar} disabled={gerando}>
          {gerando ? "Gerando (estratégia + seções + validação)…" : "Gerar currículo"}
        </button>
        {erro && <p className="mensagem-erro">{erro}</p>}
      </div>

      <div className="cartao">
        <h3>Currículos gerados</h3>
        <table>
          <thead>
            <tr><th>Título</th><th>Status</th><th>Criado em</th><th></th></tr>
          </thead>
          <tbody>
            {curriculos.map((c) => (
              <tr key={c.id}>
                <td>{c.titulo}</td>
                <td><span className="selo">{c.status}</span></td>
                <td>{new Date(c.criadoEm).toLocaleString("pt-BR")}</td>
                <td><Link to={`/curriculos/${c.id}`}>Editar</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
