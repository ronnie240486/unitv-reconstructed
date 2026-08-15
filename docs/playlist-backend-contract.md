# Contrato de playlists — Backend Rencia

## Identificação

Todas as chamadas usam HTTPS e o MAC deve ser enviado no formato `AA:BB:CC:DD:EE:FF`. A tela pode exibir os 12 caracteres compactos, mas o botão **Copiar** e as chamadas ao backend usam o formato com dois-pontos.

## Validação antes da Home

```http
GET https://renciaapp.manus.space/api/device/check?mac=AA:BB:CC:DD:EE:FF
```

O APK verifica `found`, `allowed`, `status`, `app`, `urlM3u8`, `urlEpg` e `dataExpiracao`. Quando `allowed` for falso, o aplicativo não libera reprodução e mostra somente uma mensagem amigável.

## Fontes/listas

```http
GET https://renciaapp.manus.space/api/guim.php?mac=AA:BB:CC:DD:EE:FF
```

Os aliases `/api/v4/guim.php` e `/api/v5/guim.php` são compatíveis. O formato esperado é:

```json
{
  "data": [
    {
      "id": 123,
      "mac": "AA:BB:CC:DD:EE:FF",
      "url": "https://servidor.exemplo.com",
      "username": "usuario",
      "password": "senha",
      "type": "xtream"
    }
  ]
}
```

O APK apresenta até quatro fontes devolvidas pelo painel como listas selecionáveis. O nome exibido é gerado como “Lista 1”, “Lista 2” etc. quando o objeto não possuir um campo de nome. Senhas não são exibidas nem registradas em logs.

## Presença e conteúdo

O APK envia `GET /api/v5/heartbeat?mac=...` ao iniciar, ao trocar de conteúdo e a cada 60 segundos. Quando houver conteúdo atual, acrescenta `current_content` sem enviá-lo vazio.

## Avisos, vencimento e failover

A rota `GET /api/v5/list-notifications?mac=...` é consultada junto do heartbeat. Alertas são apresentados uma vez por `id` e confirmados com:

```http
POST /api/v5/list-notifications/ack
Content-Type: application/json

{"mac":"AA:BB:CC:DD:EE:FF","alert_id":123}
```

Quando `playlist_sync_required` estiver ativo, o APK busca novamente `/api/guim.php` e atualiza as listas sem fechar. Erros reais de reprodução são enviados a `POST /api/v5/playback-failure` com `mac` e `active_list_number`.

## Configuração visual e comandos

A identidade visual opcional vem de `GET /api/v5/ultra-config?mac=...`, incluindo `app_name`, logo, banner, fundo e ícones. O APK usa esses URLs quando preenchidos e mantém os assets Prestigie como fallback.

Comandos remotos são consultados em `GET /api/v5/remote-commands?mac=...` e confirmados em `/api/v5/remote-commands/ack`. O cliente reconhece atualização/troca de lista, sincronização de acesso, mensagem, reinício do player e atualização de DNS conforme os adaptadores disponíveis.
