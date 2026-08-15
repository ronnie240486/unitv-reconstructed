# Diagnóstico sanitizado do MAC

Data da verificação: 2026-08-15.

A rota de validação do backend Rencia foi consultada em modo somente leitura para o identificador exibido na captura.

| Formato enviado | Resultado |
|---|---|
| `AA:BB:CC:DD:EE:FF` | O backend reconhece o aparelho, informa status `Liberado` e retorna `allowed: true`. |
| `AABBCCDDEEFF` | O backend não encontra o aparelho e retorna `allowed: false`. |

Conclusão: o problema está no formato usado pelo fluxo de validação do APK. O painel está correto quando mostra o dispositivo online porque ele foi cadastrado no formato MAC com dois-pontos. A versão do APK que validava somente o identificador compacto podia permanecer bloqueada mesmo com o aparelho online.

Nenhuma URL de playlist, usuário ou senha foi preservada neste relatório.

## Verificação da lista

A consulta sanitizada ao MAC no formato com dois-pontos retornou uma resposta objeto com `data` contendo 1 fonte válida. A fonte possui `id`, `mac`, `url`, `username`, `password` e `type`, com tipo `m3u_plus`. A mesma estrutura foi confirmada em `/api/guim.php`, `/api/v4/guim.php` e `/api/v5/guim.php`.

A correção passa a liberar automaticamente a Home quando a validação retorna `allowed: true` e pelo menos uma fonte foi carregada. O botão **Verificar e atualizar** continua disponível para recarregar as fontes manualmente.
