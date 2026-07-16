/** Cliente HTTP do Backend Core. O frontend nunca fala com o serviço de IA. */

async function requisicao<T>(url: string, opcoes?: RequestInit): Promise<T> {
  const resposta = await fetch(url, {
    headers: opcoes?.body instanceof FormData
      ? undefined
      : { "Content-Type": "application/json" },
    ...opcoes,
  });
  if (!resposta.ok) {
    let mensagem = `Erro ${resposta.status}`;
    try {
      const corpo = await resposta.json();
      if (corpo.erro) mensagem = corpo.erro;
    } catch { /* corpo não-JSON */ }
    throw new Error(mensagem);
  }
  const texto = await resposta.text();
  return (texto ? JSON.parse(texto) : undefined) as T;
}

export const api = {
  get: <T>(url: string) => requisicao<T>(url),
  post: <T>(url: string, corpo?: unknown) =>
    requisicao<T>(url, { method: "POST", body: corpo ? JSON.stringify(corpo) : undefined }),
  put: <T>(url: string, corpo: unknown) =>
    requisicao<T>(url, { method: "PUT", body: JSON.stringify(corpo) }),
  del: (url: string) => requisicao<void>(url, { method: "DELETE" }),
  upload: <T>(url: string, form: FormData) =>
    requisicao<T>(url, { method: "POST", body: form }),
};
