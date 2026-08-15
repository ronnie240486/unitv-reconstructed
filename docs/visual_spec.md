# Especificação visual Prestigie — reconstrução fiel

## Referência visual

A captura enviada mostra uma interface de TV em orientação paisagem, com tela 16:9, fundo vinho escuro e composição densa de catálogo. A hierarquia visual observada é a seguinte: no topo há a marca no canto esquerdo; ao centro ficam o botão de assinatura, busca, filtro, histórico, perfil, ajuda, notificações e conectividade; no canto direito aparecem hora e indicador de conexão. Abaixo existe uma navegação horizontal com as categorias **GRATUITO**, **DESTAQUES**, **FILMES**, **SÉRIES**, **KIDS**, **ANIME** e **EXPLORAR**.

O corpo da tela usa uma grade de conteúdo com destaque grande ao centro-esquerda, cards secundários empilhados ao lado, cards grandes à direita e uma segunda faixa inferior. O foco precisa ser claramente visível para uso com controle remoto, com transições suaves, áreas clicáveis amplas e suporte a D-pad/teclado.

## Componentes que precisam existir

| Área | Componente reconstruído |
|---|---|
| Apresentação | Splash com logo Prestigie, carregamento, estados de rede e entrada para Home |
| Cabeçalho | Logo, botão de assinatura, busca, filtros, histórico, perfil, ajuda, notificações, conectividade e relógio |
| Navegação | Tabs horizontais com seleção por foco e rolagem para categorias |
| Home | Hero editorial, grade assimétrica, cards de destaque, faixas horizontais e selo de conteúdo |
| VOD | Categorias, busca, filtros, detalhes, temporadas, episódios, favoritos e histórico |
| Live | Lista de canais, EPG, guia, reserva, player, áudio, legenda, proporção e feedback |
| Kids/Anime | Categorias próprias, bloqueio parental e cards com classificação |
| Explorar | Pesquisa global, gêneros, diretores, atores, recomendações e mais vistos |
| Conta | Login, perfil, plano, compras, cupons, convite, segurança e configurações |
| Estados | Loading, sem conteúdo, erro de rede, sessão expirada, bloqueio parental e atualização |
| Apresentação de conteúdo | Carrossel/grade para filmes, séries, novelas, desenhos, Kids e Anime |

## Direção de implementação

A versão anterior usava uma barra lateral e um hero genérico, o que não corresponde à captura. A nova versão deve usar um **top bar horizontal**, tabs no topo e uma **grade editorial assimétrica**. Os elementos devem usar fundo vinho/preto, superfícies translúcidas, destaque dourado para foco e texto claro. A marca Prestigie substitui o nome UniTV, enquanto o layout e as interações seguem a referência visual.

Os pôsteres e marcas de terceiros serão substituídos por cards e artes próprios de demonstração. Isso permite preservar composição, proporções, densidade e comportamento da interface sem redistribuir material protegido.

## Fluxos prioritários

O primeiro fluxo é Splash → Home → seleção de categoria → foco em card → detalhes → ação Assistir/Favoritar. O segundo é Home → busca → filtros → resultado → detalhes. O terceiro é Home → Perfil → login → plano/cupons/segurança/configurações. O quarto é Home → Live → canal → guia EPG → player/controles.

## Critério de aceite visual

O APK final deverá apresentar, ao iniciar, uma tela de apresentação Prestigie e, em seguida, uma Home com a mesma hierarquia espacial da captura: cabeçalho superior, tabs horizontais, hero grande e grade de cards. O usuário deve conseguir percorrer as categorias e cards pelo controle remoto, abrir detalhes, voltar, buscar, entrar no perfil e acessar as áreas Live/VOD/Kids/Anime/Explorar.
