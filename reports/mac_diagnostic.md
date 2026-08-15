# Diagnóstico sanitizado do MAC

Data da verificação: 2026-08-15.

A rota de validação do backend Rencia foi consultada em modo somente leitura para o identificador exibido na captura.

| Formato enviado | Resultado |
|---|---|
| `AA:BB:CC:DD:EE:FF` | O backend reconhece o aparelho, informa status `Liberado` e retorna `allowed: true`. |
| `AABBCCDDEEFF` | O backend não encontra o aparelho e retorna `allowed: false`. |

Conclusão: o problema está no formato usado pelo fluxo de validação do APK. O painel está correto quando mostra o dispositivo online porque ele foi cadastrado no formato MAC com dois-pontos. A versão do APK que validava somente o identificador compacto podia permanecer bloqueada mesmo com o aparelho online.

Nenhuma URL de playlist, usuário ou senha foi preservada neste relatório.
