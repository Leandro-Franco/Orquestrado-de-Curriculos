import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api";
import type { RequisitoVaga, Vaga } from "../types";

export default function VagaDetalhe() {
  const { id } = useParams();
  const [vaga, setVaga] = useState<Vaga | null>(null);
  const [requisitos, setRequisitos] = useState<RequisitoVaga[]>([]);
  const [analisando, setAnalisando] = useState(false);
  const [erro, setErro] = useState("");

  const carregar = async () => {
    setVaga(await api.get<Vaga>(`/api/vagas/${id}`));
    setRequisitos(await api.get<RequisitoVaga[]>(`/api/vagas/${id}/requisitos`));
  };

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, [id]);

  async function analisar() {
    setAnalisando(true); setErro("");
    try {
      await api.post(`/api/vagas/${id}/analisar`);
      await carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setAnalisando(false);
    }
  }

  if (!vaga) return <p>{erro || "Carregando…"}</p>;

  const analise = (vaga.analise ?? {}) as Record<string, unknown>;
  const camposSimples: [string, string][] = [
    ["senioridade", "Senioridade"], ["modalidade", "Modalidade"], ["localizacao", "Localização"],
  ];

  return (
    <div>
      <h2>{vaga.titulo || "Vaga"} {vaga.empresa ? `— ${vaga.empresa}` : ""}</h2>
      <button onClick={analisar} disabled={analisando}>
        {analisando ? "Analisando…" : vaga.status === "ANALISADA" ? "Reanalisar com IA" : "Analisar com IA"}
      </button>{" "}
      {vaga.status === "ANALISADA" && (
        <Link to="/curriculos" state={{ vagaId: vaga.id }}>
          <button className="secundario">Gerar currículo para esta vaga</button>
        </Link>
      )}
      {erro && <p className="mensagem-erro">{erro}</p>}

      {vaga.status === "ANALISADA" && (
        <div className="cartao">
          <h3>Análise</h3>
          {camposSimples.map(([campo, rotulo]) =>
            analise[campo] ? <p key={campo}><strong>{rotulo}:</strong> {String(analise[campo])}</p> : null)}
          {Array.isArray(analise.tecnologias) && analise.tecnologias.length > 0 && (
            <p><strong>Tecnologias:</strong> {(analise.tecnologias as string[]).join(", ")}</p>
          )}
          {Array.isArray(analise.idiomas) && analise.idiomas.length > 0 && (
            <p><strong>Idiomas:</strong> {(analise.idiomas as string[]).join(", ")}</p>
          )}
        </div>
      )}

      {requisitos.length > 0 && (
        <div className="cartao">
          <h3>Compatibilidade requisito a requisito</h3>
          <table>
            <thead>
              <tr><th>Requisito</th><th>Tipo</th><th>Compatibilidade</th><th>Evidências (fatos)</th></tr>
            </thead>
            <tbody>
              {requisitos.map((r) => (
                <tr key={r.id}>
                  <td>{r.descricao}{r.justificativa && (
                    <div style={{ fontSize: 12, color: "var(--tinta-suave)" }}>{r.justificativa}</div>
                  )}</td>
                  <td><span className={`selo ${r.tipo}`}>{r.tipo}</span></td>
                  <td><span className={`selo ${r.compatibilidade ?? ""}`}>{r.compatibilidade ?? "—"}</span></td>
                  <td>{r.fatosRelacionados?.length ? `#${r.fatosRelacionados.join(", #")}` : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="cartao">
        <h3>Descrição original</h3>
        <pre style={{ whiteSpace: "pre-wrap", fontFamily: "inherit" }}>{vaga.descricaoBruta}</pre>
      </div>
    </div>
  );
}
