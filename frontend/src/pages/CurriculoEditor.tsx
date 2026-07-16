import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import type { Curriculo, Secao, Versao } from "../types";

export default function CurriculoEditor() {
  const { id } = useParams();
  const [curriculo, setCurriculo] = useState<Curriculo | null>(null);
  const [secoes, setSecoes] = useState<Secao[]>([]);
  const [versoes, setVersoes] = useState<Versao[]>([]);
  const [edicoes, setEdicoes] = useState<Record<number, { titulo: string; conteudo: string }>>({});
  const [ocupado, setOcupado] = useState<number | null>(null);
  const [previewKey, setPreviewKey] = useState(0);
  const [aba, setAba] = useState<"editor" | "preview">("editor");
  const [erro, setErro] = useState("");

  const carregar = async () => {
    const dados = await api.get<{ curriculo: Curriculo; secoes: Secao[] }>(`/api/curriculos/${id}`);
    setCurriculo(dados.curriculo);
    setSecoes(dados.secoes);
    setVersoes(await api.get<Versao[]>(`/api/curriculos/${id}/versoes`));
    setPreviewKey((k) => k + 1);
  };

  useEffect(() => { carregar().catch((e) => setErro(e.message)); }, [id]);

  function edicaoDe(s: Secao) {
    return edicoes[s.id] ?? { titulo: s.titulo ?? "", conteudo: s.conteudo ?? "" };
  }

  async function salvar(s: Secao) {
    setOcupado(s.id); setErro("");
    try {
      const e = edicaoDe(s);
      await api.put(`/api/curriculos/${id}/secoes/${s.id}`, e);
      await carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(null);
    }
  }

  async function regerar(s: Secao) {
    setOcupado(s.id); setErro("");
    try {
      await api.post(`/api/curriculos/${id}/secoes/${s.id}/regerar`);
      setEdicoes((atual) => { const { [s.id]: _, ...resto } = atual; return resto; });
      await carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setOcupado(null);
    }
  }

  if (!curriculo) return <p>{erro || "Carregando…"}</p>;

  return (
    <div>
      <h2>{curriculo.titulo}</h2>
      <button className={aba === "editor" ? "" : "secundario"} onClick={() => setAba("editor")}>Editor</button>
      <button className={aba === "preview" ? "" : "secundario"} onClick={() => setAba("preview")}>Preview</button>
      <a href={`/api/curriculos/${id}/pdf`}>
        <button className="secundario">Exportar PDF</button>
      </a>
      {erro && <p className="mensagem-erro">{erro}</p>}

      {aba === "preview" ? (
        <div className="cartao">
          <p>O preview usa o mesmo HTML, CSS e renderização do PDF.</p>
          <iframe key={previewKey} className="preview" src={`/api/curriculos/${id}/preview`} title="Preview" />
        </div>
      ) : (
        <>
          {secoes.map((s) => (
            <div className="cartao" key={s.id}>
              <h3>{s.tipo}</h3>
              <label>Título da seção</label>
              <input value={edicaoDe(s).titulo}
                onChange={(e) => setEdicoes({ ...edicoes, [s.id]: { ...edicaoDe(s), titulo: e.target.value } })} />
              <label>Conteúdo (um item por linha)</label>
              <textarea value={edicaoDe(s).conteudo}
                onChange={(e) => setEdicoes({ ...edicoes, [s.id]: { ...edicaoDe(s), conteudo: e.target.value } })} />
              {s.alertasValidacao?.map((a, i) => (
                <div key={i} className="alerta-validacao">⚠ {a}</div>
              ))}
              {s.fatosUtilizados && s.fatosUtilizados.length > 0 && (
                <p style={{ fontSize: 12, color: "var(--tinta-suave)" }}>
                  Fatos utilizados: #{s.fatosUtilizados.join(", #")}
                </p>
              )}
              <button onClick={() => salvar(s)} disabled={ocupado === s.id}>Salvar edição</button>
              <button className="secundario" onClick={() => regerar(s)} disabled={ocupado === s.id}>
                {ocupado === s.id ? "Processando…" : "Regenerar com IA"}
              </button>
            </div>
          ))}

          <div className="cartao">
            <h3>Histórico de versões</h3>
            <table>
              <thead><tr><th>Versão</th><th>Nota</th><th>Data</th></tr></thead>
              <tbody>
                {versoes.map((v) => (
                  <tr key={v.id}>
                    <td>v{v.numero}</td>
                    <td>{v.nota}</td>
                    <td>{new Date(v.criadoEm).toLocaleString("pt-BR")}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
