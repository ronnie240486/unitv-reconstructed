# Relatório técnico resumido

## Identificação

O arquivo fornecido declara o pacote `com.global.unitviptv`, compile SDK 34, orientação paisagem e suporte opcional a Android TV. O launcher é `com.interactive.brasiliptv.ui.activity.WelcomeActivity`, enquanto o Application declarado é `s.h.e.l.l.S`.

## Arquitetura observável

O manifesto expõe módulos de inicialização, autenticação, Home, Live, VOD, perfil, compras, cupons, convites, configurações, partidas e Web. O mapa de rotas em `assets/therouter/routeMap.json` confirma os pontos principais de Home, VOD e perfil.

O pacote contém apenas quatro classes Smali do carregador após a decodificação convencional. Em `attachBaseContext`, o carregador escolhe a ABI do dispositivo, extrai `libexec.so` e `libexecmain.so` de `assets/ijm_lib/<abi>/` para o diretório privado e os carrega. Em seguida, usa métodos nativos e reflexão para delegar ao `AppWrapper` funcional.

## Payload protegido

O arquivo também contém `assets/IJMDal.Data`, `assets/af.bin`, `assets/ijiami.ajm`, `assets/ijiami.dat`, `assets/signed.bin` e bibliotecas nativas `libexec*`. Os binários possuem sinais de empacotamento e símbolos de inicialização JNI, mas não foram executados nem incorporados ao novo projeto. O comportamento funcional interno do payload não é tratado como recuperado.

## Superfície de produto

| Área | Capacidades inferidas |
|---|---|
| Home | Canais, atalhos, recomendações e entrada para Live/VOD/Esportes |
| Live | Lista de canais, EPG, reserva, busca, player, áudio, legenda e proporção |
| VOD | Categorias, busca, detalhes, temporadas, episódios, atores, diretores, favoritos, histórico e player |
| Perfil | Conta, compras, cupons, convites, histórico, QR code, segurança e configurações |
| Esportes | Agenda, categorias, detalhes, ranking, estatísticas e replay |
| Login | Conta/e-mail, senha, recuperação, troca obrigatória e mensagens de sessão |

## Decisão de reconstrução

A implementação Prestigie reproduz a arquitetura de navegação e os contratos de domínio com dados locais fictícios. A UI usa Compose e mantém o application ID próprio `com.example.prestigie.reconstructed`. Backend, autenticação, DNS, player, pagamentos, push e telemetria aparecem apenas como interfaces ou pontos de extensão.

## Limitações

Não é possível afirmar equivalência funcional ou visual total com o aplicativo original, pois a lógica e parte dos ativos estão protegidos ou encapsulados. O código neste repositório deve ser tratado como um scaffold independente, não como uma restauração do código original.
