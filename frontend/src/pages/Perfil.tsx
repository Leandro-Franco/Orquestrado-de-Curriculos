import { useEffect, useState } from "react";
import { api } from "../api";
import type { Perfil as PerfilTipo } from "../types";

export default function Perfil() {
  const [perfil, setPerfil] = useState<PerfilTipo>({});
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");

  useEffect(() => {
    api.get<PerfilTipo>("/api/perfil").then(setPerfil).catch((e) => setErro(e.message));
  }, []);

  const campo = (chave: keyof PerfilTipo) => ({
    value: perfil[chave] ?? "",
    onChange: (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setPerfil({ ...perfil, [chave]: e.target.value }),
  });

  async function salvar() {
    setMensagem(""); setErro("");
    try {
      setPerfil(await api.put<PerfilTipo>("/api/perfil", perfil));
      setMensagem("Perfil salvo.");
    } catch (e) {
      setErro((e as Error).message);
    }
  }

  return (
    <div>
      <h2>Perfil profissional</h2>
      <div className="cartao">
        <label>Nome completo</label>
        <input {...campo("nomeCompleto")} />
        <label>Título profissional</label>
        <input {...campo("tituloProfissional")} />
        <label>E-mail</label>
        <input {...campo("email")} />
        <label>Telefone</label>
        <input {...campo("telefone")} />
        <label>Localização</label>
        <input {...campo("localizacao")} />
        <label>Objetivo profissional</label>
        <textarea {...campo("objetivo")} />
        <button onClick={salvar}>Salvar</button>
        {mensagem && <p className="mensagem-ok">{mensagem}</p>}
        {erro && <p className="mensagem-erro">{erro}</p>}
      </div>
    </div>
  );
}
