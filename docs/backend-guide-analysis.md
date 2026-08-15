# Análise do Guia de Backend — Rencia

## Base de produção

O guia define `https://renciaapp.manus.space` como base de produção e exige HTTPS em todas as chamadas. O identificador de cada aparelho deve ser enviado como MAC normalizado no formato `AA:BB:CC:DD:EE:FF`.

## Fluxo obrigatório

| Ordem | Rota | Finalidade |
|---:|---|---|
| 1 | `GET /api/device/check?mac={MAC}` | Validar existência, permissão, status, aplicativo, M3U8, EPG e vencimento |
| 2 | `GET /api/guim.php?mac={MAC}` | Buscar fontes/listas e credenciais cadastradas |
| 3 | `GET /api/v5/ultra-config?mac={MAC}` | Buscar aparência visual quando aplicável |
| 4 | `GET /api/v5/heartbeat?mac={MAC}&current_content={CONTEUDO}` | Registrar presença e conteúdo atual |
| 5 | `GET /api/v5/list-notifications?mac={MAC}` | Consultar vencimento, alertas e failover |
| 6 | `GET /api/v5/remote-commands?mac={MAC}` | Consultar comando remoto pendente |
| 7 | Rota específica do aplicativo | Consultar atualização do APK |

## Validação de acesso

A resposta de `device/check` deve ser tratada antes da tela principal. Os campos relevantes são `found`, `allowed`, `status`, `app`, `urlM3u8`, `urlEpg` e `dataExpiracao`. Quando `allowed` for falso, o aplicativo deve bloquear a reprodução e mostrar somente uma mensagem amigável, sem termos internos do painel.

## Listas e credenciais

A rota de produção para listas é `/api/guim.php`, com aliases `/api/v4/guim.php` e `/api/v5/guim.php`. A resposta contém `data`, uma lista de objetos que podem conter `id`, `mac`, `url`, `username`, `password` e `type`. Portanto, o contrato provisório anterior baseado em `playlists`, `playlist_name` e `playlist_url` precisa ser substituído pela leitura de `data` e pela adaptação de cada fonte para a tela de seleção. Senhas não podem aparecer em logs.

## Visual configurável

Para aparelhos Ultra Player, a rota `/api/v5/ultra-config` retorna `app_name`, `logo_url`/`ultra_logo_url`, `banner_url`/`ultra_banner_url`, `background_url`/`ultra_background_url`, mensagem e ícones para Live, filmes e séries. Os campos podem estar vazios e devem ser tratados como opcionais. A configuração visual deve permanecer isolada por aplicativo.

## Presença, alertas e failover

O heartbeat deve ser enviado ao iniciar, ao trocar de conteúdo e a cada 60 segundos. Pode carregar somente `mac` quando o conteúdo não mudou. A rota de notificações também deve ser consultada junto do heartbeat. Alertas precisam ser exibidos sem termos internos e confirmados por `POST /api/v5/list-notifications/ack` com `mac` e `alert_id`.

Quando `playlist_sync_required` for verdadeiro, o APK deve buscar novamente as listas em segundo plano e atualizar o conteúdo sem fechar. Falhas reais de reprodução devem ser reportadas imediatamente por `POST /api/v5/playback-failure` com `mac` e `active_list_number`; quando `switch_applied` for verdadeiro, o app deve recarregar a lista priorizada.

## Comandos remotos e atualização

Os comandos possíveis são `refresh_playlist`, `switch_playlist`, `update_dns`, `show_message`, `restart_player` e `sync_access`. O app deve processar um por vez, ignorar comandos vencidos e confirmar em `/api/v5/remote-commands/ack` com `status` `executed` ou `failed`.

A atualização deve usar a URL própria de cada aplicativo. Para uma nova configuração Prestigie, o painel precisa receber nome, ícone, logo, fundo, banner, ícones e URL de atualização.

## Consequência para a implementação

A integração deve deixar de usar o endpoint genérico provisório `?mac=...` e passar a executar o fluxo Rencia completo. A normalização visual do MAC deve ser `AA:BB:CC:DD:EE:FF`, embora a tela possa também mostrar uma versão compacta de 12 caracteres para cópia, desde que a chamada ao backend use a forma com dois-pontos. O código deve ter um cliente HTTP centralizado, um estado de acesso bloqueado/liberado, um repositório de fontes, um heartbeat periódico, processamento de avisos/comandos e adaptadores para troca de lista.
