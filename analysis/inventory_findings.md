# Inventário inicial — unitv_RS-NPWN(4.18).apk

## Identificação

- Arquivo analisado: `unitv_RS-NPWN(4.18).apk`
- Pacote declarado: `com.global.unitviptv`
- `compileSdkVersion`: 34 (`android-14`)
- Orientação declarada: paisagem (`landscape`)
- Ponto de entrada declarado: `com.interactive.brasiliptv.ui.activity.WelcomeActivity`
- Application declarado: `s.h.e.l.l.S`, um carregador/protetor, e não o Application funcional.
- Tamanho e SHA-256 do APK: consultar `file.txt` neste diretório.

## Componentes observáveis

O manifesto declara as áreas funcionais `Home`, `VOD`, `Live`, `Mine`, `Login`, `Match` e Web. Entre as Activities próprias estão `HomeActivity`, `VodDetailsActivity`, `VodCategoryActivity`, `VodSearchActivity`, `TopicActivity`, `SmartvListActivity`, `LiveFreeActivity`, `CouponActivity`, `UserCenterActivity`, `AccountSecurityActivity`, `PurchaseActivity`, `InviteFriendsActivity`, `SettingsActivity`, `ForcePasswordChangeActivity`, `MatchScheduleActivity`, `MatchDetailActivity`, `MatchCategoryActivity`, `MatchRankCategoryActivity`, `CommonWebActivity` e `WebActivity`.

O arquivo `assets/therouter/routeMap.json` confirma rotas para VOD, centro do usuário, compra, convite, cupons, segurança da conta, tela inicial e troca obrigatória de senha. O arquivo `assets/domain_test.json` contém chaves para endpoints de upgrade, portal, EPG, mercado, avisos e anúncios, porém os valores encontrados são placeholders `xx`.

## Segurança e empacotamento

O APK contém apenas quatro classes Smali do pacote `s.h.e.l.l` após a decodificação convencional. O `Application` inicializa um mecanismo de proteção que seleciona a ABI, extrai `assets/ijm_lib/<abi>/libexec.so` e `libexecmain.so` para o diretório privado de arquivos, carrega as bibliotecas nativas e delega para `com.interactive.brasiliptv.app.AppWrapper`.

O auxiliar `N.smali` expõe métodos nativos de carregamento/reflexão (`l`, `r`, `ra`, `al`, `sa`, entre outros). A rotina `S.sp()` procura reflexivamente `com.ijm.dataencryption.DETool.loadDEso(...)`. Esse desenho explica por que o JADX produziu somente classes do carregador e não o código funcional completo.

Há artefatos de proteção adicionais (`assets/IJMDal.Data`, `assets/af.bin`, `assets/ijiami.ajm`, `assets/ijiami.dat`, `assets/signed.bin`) e imagens com nomes `encrypted`. Portanto, não é possível recuperar honestamente o código-fonte original completo apenas por decompilação convencional; qualquer reimplementação deverá ser uma reconstrução limpa baseada no comportamento e nos componentes observáveis.

## Bibliotecas e integrações

O APK inclui bibliotecas nativas de reprodução IJK/FFmpeg (`libijkplayer.so`, `libijkffmpeg.so`, `libijksdl.so`), Crashlytics, Firebase Messaging/Analytics, Umeng/Alibaba push, Qiniu DNS, Room/SQLite e bibliotecas de suporte Android. O manifesto declara acesso à Internet, estado de rede/Wi-Fi, armazenamento, áudio, notificações, wake lock e instalação de pacotes, entre outras permissões.

## Limites adotados para a reconstrução

A entrega será organizada como uma implementação limpa, sem copiar o código protegido, os payloads nativos, credenciais, assinatura do APK ou ativos marcados como criptografados. O primeiro código-fonte deverá implementar a arquitetura de navegação, telas e contratos locais observáveis, deixando integrações de backend e reprodução como interfaces/adapters configuráveis e claramente identificados como pontos que exigem autorização, endpoints e regras de negócio legítimos.

## Evidências primárias

- `decoded/AndroidManifest.xml`
- `unpacked/assets/domain_test.json`
- `unpacked/assets/therouter/routeMap.json`
- `decoded/smali/s/h/e/l/l/S.smali`
- `decoded/smali/s/h/e/l/l/N.smali`
- `reports/protected_payload.txt`
- `reports/jadx.log`
