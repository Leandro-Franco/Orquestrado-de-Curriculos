import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api";
import type { Documento } from "../types";

export default function Documentos() {
  const [documentos, setDocumentos] = useState<Documento[]>([]);
  const [titulo, setTitulo] = useState("");
  const [conteudo, setConteudo] = useState("");
  const [arquivo, setArquivo] = useState<File | null>(null);
  const [processando, setProcessando] = useState(false);
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");
  const navegar = useNavigate();

  const carregar = () =>
    api.get<Documento[]>("/api/documentos").then(setDocumentos).catch((e) => setErro(e.message));

  useEffect(() => { carregar(); }, []);

  async function importar(acao: () => Promise<{ propostasGeradas: number }>) {
    setProcessando(true); setMensagem(""); setErro("");
    try {
      const resultado = await acao();
      setMensagem(`Importado. ${resultado.propostasGeradas} proposta(s) gerada(s) para sua revisão.`);
      setTitulo(""); setConteudo(""); setArquivo(null);
      carregar();
    } catch (e) {
      setErro((e as Error).message);
    } finally {
      setProcessando(false);
    }
  }

  return (
    <div>
      <h2>Documentos e importação</h2>

      <div className="cartao">
        <h3>Importar texto</h3>
        <label>Título</label>
        <input value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        <label>Conteúdo (currículo antigo, anotações, descrição de projeto…)</label>
        <textarea value={conteudo} onChange={(e) => setConteudo(e.target.value)} />
        <button disabled={processando}
          onClick={() => importar(() => api.post("/api/documentos/texto", { titulo, conteudo }))}>
          {processando ? "Processando…" : "Importar e extrair"}
        </button>
      </div>

      <div className="cartao">
        <h3>Importar arquivo (.pdf, .txt, .md)</h3>
        <input type="file" accept=".pdf,.txt,.md"
          onChange={(e) => setArquivo(e.target.files?.[0] ?? null)} />
        <button disabled={processando || !arquivo}
          onClick={() => importar(() => {
            const form = new FormData();
            form.append("arquivo", arquivo!);
            return api.upload("/api/documentos/arquivo", form);
          })}>
          {processando ? "Processando…" : "Enviar e extrair"}
        </button>
      </div>

      {mensagem && (
        <p className="mensagem-ok">
          {mensagem} <button className="secundario" onClick={() => navegar("/propostas")}>Revisar propostas</button>
        </p>
      )}
      {erro && <p className="mensagem-erro">{erro}</p>}

      <div className="cartao">
        <h3>Documentos importados</h3>
        <table>
          <thead>
            <tr><th>Título</th><th>Origem</th><th>Status</th><th>Data</th></tr>
          </thead>
          <tbody>
            {documentos.map((d) => (
              <tr key={d.id}>
                <td>{d.titulo}</td>
                <td>{d.origem}</td>
                <td>{d.status}</td>
                <td>{new Date(d.criadoEm).toLocaleString("pt-BR")}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
