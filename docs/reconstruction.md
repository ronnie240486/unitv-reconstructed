# Matriz de reconstrução

| Componente observado | Equivalente no scaffold | Estado |
|---|---|---|
| `WelcomeActivity` / Home | `MainActivity` + `HomeScreen` | Reimplementado como entrada Compose |
| `HomeActivity` | `HomeScreen` | Navegação e conteúdo local demonstrativos |
| `LiveFreeActivity` | `LiveScreen` | Lista, prévia e EPG representativos |
| `VodCategoryActivity` / `VodSearchActivity` | `VodScreen` + `SearchScreen` | Categorias e busca local |
| `VodDetailsActivity` | `VodDetailsScreen` | Detalhes e ações modeladas |
| `Match*Activity` | `SportsScreen` | Agenda e cartões de eventos |
| `UserCenterActivity` | `ProfileScreen` | Conta e atalhos |
| `PurchaseActivity` | `PurchaseScreen` | Planos fictícios; checkout desativado |
| `CouponActivity` | `CouponsScreen` | Lista e ação local |
| `AccountSecurityActivity` | `SecurityScreen` | Ações representativas |
| `SettingsActivity` | `SettingsScreen` | Preferências representativas |
| `AppWrapper` funcional | `UnitvViewModel` | Estado local, sem carregamento dinâmico |
| Player IJK/FFmpeg | `PlayerGateway` | Interface sem binário ou stream |
| Portal/EPG/upgrade/notice/ads | `ApiConfig` | Campos vazios e injeção futura |
| DNS | `DnsConfig` | Até cinco servidores, sem valores herdados |

## Critérios de segurança

O projeto não usa o pacote original, não tenta chamar seus endpoints, não extrai ou copia os payloads protegidos e não inclui ativos com marcação `encrypted`. O application ID foi alterado para evitar colisão acidental com o produto analisado.

## Próximos passos autorizados

Para transformar o scaffold em produto, o integrador deverá fornecer um contrato de API próprio, autenticação própria, catálogo licenciado, URLs de streams autorizadas, política de anúncios, gateway de pagamento, textos e imagens sob licença, além de testes em dispositivos Android TV. Cada integração deve ser adicionada como adapter isolado e coberta por testes.
