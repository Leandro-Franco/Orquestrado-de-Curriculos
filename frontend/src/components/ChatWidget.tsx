import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router-dom";
import { api } from "../api";

interface Mensagem {
  papel: "usuario" | "assistente";
  texto: string;
}

/**
 * Chat fixo presente em todas as telas (ADR-004). Ao enviar, captura o texto
 * visível da área de conteúdo e o manda como contexto — a IA responde sobre o
 * que está na tela, mas não altera nada (somente leitura).
 */
export default function ChatWidget() {
  const [aberto, setAberto] = useState(false);
  const [mensagens, setMensagens] = useState<Mensagem[]>([]);
  const [texto, setTexto] = useState("");
  const [ocupado, setOcupado] = useState(false);
  const fimRef = useRef<HTMLDivElement>(null);
  const rota = useLocation().pathname;

  useEffect(() => {
    fimRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [mensagens, aberto]);

  function contextoDaTela(): string {
    const conteudo = document.querySelector("main.conteudo") as HTMLElement | null;
    const textoTela = conteudo?.innerText ?? "";
    return `Página: ${rota}\n${textoTela}`.slice(0, 6000);
  }

  async function enviar() {
    const mensagem = texto.trim();
    if (!mensagem || ocupado) return;
    const novas: Mensagem[] = [...mensagens, { papel: "usuario", texto: mensagem }];
    setMensagens(novas);
    setTexto("");
    setOcupado(true);
    try {
      const resposta = await api.post<{ resposta: string }>("/api/chat", {
        mensagem,
        contexto: contextoDaTela(),
        historico: novas.slice(-6),
      });
      setMensagens([...novas, { papel: "assistente", texto: resposta.resposta }]);
    } catch (e) {
      setMensagens([...novas, {
        papel: "assistente",
        texto: `Não consegui responder: ${(e as Error).message}`,
      }]);
    } finally {
      setOcupado(false);
    }
  }

  if (!aberto) {
    return (
      <button className="chat-botao" onClick={() => setAberto(true)} title="Assistente">
        💬
      </button>
    );
  }

  return (
    <div className="chat-painel">
      <div className="chat-cabecalho">
        <strong>Assistente</strong>
        <span>vê a tela atual · somente leitura</span>
        <button className="secundario" onClick={() => setAberto(false)}>✕</button>
      </div>
      <div className="chat-mensagens">
        {mensagens.length === 0 && (
          <p className="chat-vazio">
            Pergunte sobre o que está na tela — ex.: "o que falta aprovar aqui?",
            "resuma a compatibilidade desta vaga".
          </p>
        )}
        {mensagens.map((m, i) => (
          <div key={i} className={`chat-mensagem ${m.papel}`}>{m.texto}</div>
        ))}
        {ocupado && <div className="chat-mensagem assistente">…</div>}
        <div ref={fimRef} />
      </div>
      <div className="chat-entrada">
        <input
          value={texto}
          onChange={(e) => setTexto(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && enviar()}
          placeholder="Escreva sua pergunta…"
        />
        <button onClick={enviar} disabled={ocupado}>Enviar</button>
      </div>
    </div>
  );
}
