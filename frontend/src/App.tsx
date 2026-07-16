import { useEffect, useState } from "react";
import { NavLink, Navigate, Route, Routes } from "react-router-dom";
import Conhecimento from "./pages/Conhecimento";
import CurriculoEditor from "./pages/CurriculoEditor";
import Curriculos from "./pages/Curriculos";
import Documentos from "./pages/Documentos";
import Metricas from "./pages/Metricas";
import Perfil from "./pages/Perfil";
import Propostas from "./pages/Propostas";
import VagaDetalhe from "./pages/VagaDetalhe";
import Vagas from "./pages/Vagas";

const LINKS = [
  ["/perfil", "Perfil"],
  ["/conhecimento", "Base de Conhecimento"],
  ["/documentos", "Documentos"],
  ["/propostas", "Propostas"],
  ["/vagas", "Vagas"],
  ["/curriculos", "Currículos"],
  ["/metricas", "Métricas de IA"],
] as const;

type Tema = "claro" | "escuro";

function temaInicial(): Tema {
  const salvo = localStorage.getItem("tema");
  if (salvo === "claro" || salvo === "escuro") return salvo;
  return window.matchMedia("(prefers-color-scheme: dark)").matches ? "escuro" : "claro";
}

export default function App() {
  const [tema, setTema] = useState<Tema>(temaInicial);

  useEffect(() => {
    document.documentElement.dataset.theme = tema === "escuro" ? "dark" : "light";
    localStorage.setItem("tema", tema);
  }, [tema]);

  return (
    <div className="layout">
      <nav className="menu">
        <h1>Gerador de Currículos</h1>
        {LINKS.map(([rota, rotulo]) => (
          <NavLink key={rota} to={rota} className={({ isActive }) => (isActive ? "ativo" : "")}>
            {rotulo}
          </NavLink>
        ))}
        <button className="tema" onClick={() => setTema(tema === "escuro" ? "claro" : "escuro")}>
          {tema === "escuro" ? "☀ Modo claro" : "🌙 Modo noturno"}
        </button>
      </nav>
      <main className="conteudo">
        <Routes>
          <Route path="/" element={<Navigate to="/perfil" replace />} />
          <Route path="/perfil" element={<Perfil />} />
          <Route path="/conhecimento" element={<Conhecimento />} />
          <Route path="/documentos" element={<Documentos />} />
          <Route path="/propostas" element={<Propostas />} />
          <Route path="/vagas" element={<Vagas />} />
          <Route path="/vagas/:id" element={<VagaDetalhe />} />
          <Route path="/curriculos" element={<Curriculos />} />
          <Route path="/curriculos/:id" element={<CurriculoEditor />} />
          <Route path="/metricas" element={<Metricas />} />
        </Routes>
      </main>
    </div>
  );
}
