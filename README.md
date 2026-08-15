# Prestigie

Este repositório contém uma **reimplementação limpa e demonstrativa** da superfície funcional observável em `unitv_RS-NPWN(4.18).apk`. O projeto não tenta recuperar nem redistribuir o código-fonte original, os payloads nativos protegidos, a assinatura do APK, credenciais, endpoints privados ou ativos licenciados.

> O APK analisado usa um `Application` carregador (`s.h.e.l.l.S`) que extrai bibliotecas em `assets/ijm_lib/<abi>/`, carrega código nativo e delega para `com.interactive.brasiliptv.app.AppWrapper`. Por isso, a decompilação convencional não recupera honestamente a lógica funcional completa.

## O que foi implementado

A base Android nativa em Kotlin/Jetpack Compose implementa a marca **Prestigie** e navegação em paisagem com as áreas observadas no manifesto: **Início**, **Ao vivo**, **Filmes e séries**, **Esportes** e **Perfil**. Também inclui telas demonstrativas de busca, detalhes VOD, login local, planos, cupons, segurança da conta e configurações.

A camada de dados usa `DemoContentRepository`, com modelos para canais, programação, VOD, partidas, planos, cupons e sessão. As interfaces `ContentRepository`, `PlayerGateway` e `ApiConfig` permitem conectar posteriormente um backend e um player legítimos, sem reutilizar os serviços do APK analisado. `DnsConfig` aceita até cinco servidores DNS para futuras integrações autorizadas.

## O que não está implementado

O projeto não contém autenticação real, checkout, reprodução de streams, push, telemetria, atualização automática, decodificação do payload protegido, bypass de licenças, engenharia reversa dinâmica, nem chamadas para os domínios encontrados no pacote. Os valores exibidos são fictícios e servem somente para validar a arquitetura e a navegação.

## Estrutura

| Caminho | Finalidade |
|---|---|
| `app/src/main/java/com/example/unitv/Models.kt` | Modelos de domínio e estados de tela |
| `app/src/main/java/com/example/unitv/DemoRepository.kt` | Dados locais e contratos de integração |
| `app/src/main/java/com/example/unitv/UnitvViewModel.kt` | Estado e ações da aplicação |
| `app/src/main/java/com/example/unitv/UnitvApp.kt` | UI Compose, logo Prestigie e navegação |
| `app/src/main/java/com/example/unitv/UnitvTheme.kt` | Tema visual escuro para TV |
| `app/src/main/res/drawable/prestigie_icon.png` | Ícone novo do launcher |
| `app/src/main/res/drawable/prestigie_logo.png` | Logo horizontal Prestigie |
| `docs/` | Relatório da análise, reconstrução e validação |
| `analysis/` | Evidências textuais sanitizadas da análise estática |

## Compilação

Abra o diretório no Android Studio recente com SDK Android 35. O módulo usa Gradle Kotlin DSL, Android Gradle Plugin 8.6.1, Kotlin 2.0.21, minSdk 26 e targetSdk 35. Também é possível executar `./gradlew :app:assembleDebug` no ambiente com o SDK Android configurado.

O pacote de saída terá o application ID `com.example.prestigie.reconstructed`, deliberadamente diferente do APK analisado.

## Nota de autorização

A análise foi limitada a metadados, manifesto, recursos e comportamento observável do arquivo fornecido. Qualquer uso comercial, publicação de marca, reutilização de imagens, conexão a serviços, reprodução de conteúdo, ou integração com contas deve ser feito somente com autorização dos respectivos titulares e com credenciais próprias.

## Ativação por aparelho e listas

Após a apresentação Prestigie, o aplicativo exibe o identificador disponível no aparelho em 12 caracteres hexadecimais e oferece o botão **Copiar**. O valor pode ser cadastrado no backend. A aba **Home** substitui a antiga aba Grátis, e o botão de conta do cabeçalho foi substituído por **Listas**, onde o usuário pode escolher entre uma e quatro playlists.

A integração remota está centralizada em `ProductConfig.api.playlistsUrl`. O endpoint recebe `?mac=001122AABBCC` e deve retornar `playlist_name` e `playlist_url`, conforme `docs/playlist-backend-contract.md`. Com a URL vazia, o APK usa quatro listas demonstrativas locais para permitir validar a interface sem um servidor.
