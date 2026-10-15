# Plano técnico

O orquestrador recebe uma lista estática de tools permitidas para o endpoint. Ele valida a chamada contra um schema Java, aplica limite por análise e grava `ToolInvocation` antes e depois da execução. Não há reflexão, shell, SQL livre ou URL arbitrária como tool.

O adapter Spring AI converte o contrato de tool para a API da biblioteca. A regra de negócio continua em Java e pode ser testada sem modelo remoto.
