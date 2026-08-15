# Contrato do backend de playlists

O aplicativo envia o identificador normalizado com 12 caracteres hexadecimais no parâmetro `mac`.

## Requisição

```http
GET https://SEU-DOMINIO.example/api/playlists?mac=001122AABBCC
Accept: application/json
```

A URL é configurada no `ApiConfig.playlistsUrl`. Quando ela estiver vazia, o APK utiliza quatro listas locais demonstrativas para permitir testar a tela sem backend.

## Resposta aceita

A resposta pode ser um array JSON diretamente ou um objeto com a propriedade `playlists`. O aplicativo considera somente os quatro primeiros itens válidos.

```json
{
  "playlists": [
    {
      "playlist_name": "Lista principal",
      "playlist_url": "https://seu-dominio.example/playlist/001122AABBCC/main"
    },
    {
      "playlist_name": "Filmes e séries",
      "playlist_url": "https://seu-dominio.example/playlist/001122AABBCC/vod"
    }
  ]
}
```

Os nomes dos campos devem ser exatamente `playlist_name` e `playlist_url`. Itens sem nome ou URL são ignorados. A URL retornada deve ser HTTPS e deve apontar para uma playlist que o `PlayerGateway`/adaptador de conteúdo autorizado consiga consumir.

## Fluxo dentro do APK

Depois da apresentação, o APK lê o identificador disponível no aparelho, normaliza para 12 caracteres, exibe o valor e permite copiá-lo. O usuário cadastra esse valor no backend. Ao tocar em **Atualizar listas**, o aplicativo consulta o endpoint e exibe até quatro opções na tela **Listas**. A lista escolhida fica marcada como ativa e pode ser usada pelos adaptadores de conteúdo.

## Observação sobre Android

Em versões modernas do Android, o sistema pode não liberar o endereço MAC físico para aplicativos comuns. Por isso, o código tenta primeiro uma interface de rede disponível e, quando o sistema não expõe o MAC, usa um identificador estável de fallback normalizado para 12 caracteres. Se o seu backend exigir exclusivamente o MAC físico, essa restrição precisa ser tratada por um identificador fornecido pelo fabricante, por uma API do dispositivo ou por um fluxo de ativação próprio.
