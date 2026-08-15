package com.example.unitv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val Gold = Color(0xFFE6B85C)
private val Muted = Color(0xFF9CA6B8)
private val SurfaceRaised = Color(0xFF1B2230)
private val SurfaceSoft = Color(0xFF222A39)

@Composable
fun UnitvApp(vm: UnitvViewModel = viewModel()) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            SideRail(vm)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp, vertical = 22.dp)
            ) {
                ScreenContent(vm)
            }
        }
    }

    vm.notice?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissNotice,
            title = { Text("UniTV Reconstruído") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = vm::dismissNotice) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SideRail(vm: UnitvViewModel) {
    Column(
        modifier = Modifier
            .width(218.dp)
            .fillMaxHeight()
            .background(Color(0xFF10131B))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("UniTV", color = Gold, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("RECONSTRUÍDO", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(18.dp))
        AppSection.entries.forEach { section ->
            val active = vm.selectedSection == section && vm.currentScreen in setOf(
                AppScreen.HOME, AppScreen.LIVE, AppScreen.VOD, AppScreen.SPORTS, AppScreen.PROFILE
            )
            Button(
                onClick = { vm.selectSection(section) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (active) Gold else Color.Transparent,
                    contentColor = if (active) Color(0xFF211A0B) else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(section.label, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.weight(1f))
        Text("Modo demonstração", color = Muted, fontSize = 11.sp)
        Text("Sem endpoints ou ativos do APK", color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun ScreenContent(vm: UnitvViewModel) {
    when (vm.currentScreen) {
        AppScreen.HOME -> HomeScreen(vm)
        AppScreen.LIVE -> LiveScreen(vm)
        AppScreen.VOD -> VodScreen(vm)
        AppScreen.SPORTS -> SportsScreen(vm)
        AppScreen.PROFILE -> ProfileScreen(vm)
        AppScreen.VOD_DETAILS -> VodDetailsScreen(vm)
        AppScreen.SEARCH -> SearchScreen(vm)
        AppScreen.LOGIN -> LoginScreen(vm)
        AppScreen.PURCHASE -> PurchaseScreen(vm)
        AppScreen.COUPONS -> CouponsScreen(vm)
        AppScreen.SETTINGS -> SettingsScreen(vm)
        AppScreen.ACCOUNT_SECURITY -> SecurityScreen(vm)
    }
}

@Composable
private fun ScreenFrame(
    title: String,
    subtitle: String? = null,
    back: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (back != null) {
                TextButton(onClick = back) { Text("‹ Voltar", color = Gold) }
                Spacer(Modifier.width(10.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                subtitle?.let { Text(it, color = Muted, fontSize = 14.sp) }
            }
        }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@Composable
private fun HomeScreen(vm: UnitvViewModel) {
    ScreenFrame("Olá, descubra algo para assistir", "Conteúdo local de demonstração e navegação baseada no inventário do APK") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            item {
                HeroBanner(
                    title = "Uma experiência de TV para a sala",
                    subtitle = "Live, VOD, esportes e perfil em uma navegação simples.",
                    action = { vm.selectSection(AppSection.LIVE) }
                )
            }
            item { ContentRow("Canais em destaque", vm.channels.map { it.name }) { vm.selectSection(AppSection.LIVE) } }
            item { ContentRow("Filmes e séries", vm.vodItems) { vm.openVod(it) } }
            item { ContentRow("Próximos eventos", vm.matches.map { "${it.homeTeam} x ${it.awayTeam}" }) { vm.selectSection(AppSection.SPORTS) } }
        }
    }
}

@Composable
private fun HeroBanner(title: String, subtitle: String, action: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF34415F), Color(0xFF171D2B))))
            .padding(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("EM DESTAQUE", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
            Text(title, style = MaterialTheme.typography.headlineLarge)
            Text(subtitle, color = Muted, fontSize = 15.sp)
        }
        Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF211A0B))) {
            Text("Explorar")
        }
    }
}

@Composable
private fun ContentRow(title: String, items: List<String>, onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { label ->
                SmallTile(label = label, onClick = onClick)
            }
        }
    }
}

@Composable
private fun ContentRow(title: String, items: List<VodItem>, onClick: (VodItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { item -> VodTile(item, onClick) }
        }
    }
}

@Composable
private fun SmallTile(label: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.size(width = 190.dp, height = 92.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Bottom) {
            Text("● AO VIVO", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun VodTile(item: VodItem, onClick: (VodItem) -> Unit) {
    Card(
        modifier = Modifier.size(width = 190.dp, height = 126.dp).clickable { onClick(item) },
        colors = CardDefaults.cardColors(containerColor = SurfaceRaised),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.fillMaxWidth().height(38.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF3A4663)))
            Column {
                Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${item.year} · ★ ${item.rating}", color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun LiveScreen(vm: UnitvViewModel) {
    ScreenFrame("TV ao vivo", "Canais, EPG e controles de reprodução podem ser ligados a um PlayerGateway") {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(0.95f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Canais", style = MaterialTheme.typography.titleLarge)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.channels) { channel ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                vm.showNotice("Canal selecionado: ${channel.name}. O player é um adapter configurável.")
                            },
                            colors = CardDefaults.cardColors(containerColor = SurfaceRaised)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF3B4E73)))
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(channel.name, fontWeight = FontWeight.SemiBold)
                                    Text("${channel.category} · ${channel.program}", color = Muted, fontSize = 12.sp)
                                }
                                Text("AO VIVO", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pré-visualização", style = MaterialTheme.typography.titleLarge)
                Box(
                    modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) { Text("PLAYER GATEWAY", color = Muted, letterSpacing = 2.sp) }
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Guia de programação", fontWeight = FontWeight.SemiBold)
                        Text("Hoje · Próximos programas · Reservas", color = Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VodScreen(vm: UnitvViewModel) {
    ScreenFrame("Filmes e séries", "Categorias, busca e detalhes de conteúdo") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::openSearch, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF211A0B))) { Text("Buscar") }
            OutlinedButton(onClick = { vm.showNotice("Filtros de gênero, ano e classificação prontos para integração.") }) { Text("Filtros") }
        }
        Spacer(Modifier.height(18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("Em destaque", "Filmes", "Séries", "Infantil", "Favoritos")) { label ->
                OutlinedButton(onClick = { vm.showNotice("Categoria: $label") }) { Text(label) }
            }
        }
        Spacer(Modifier.height(18.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(vm.vodItems) { item -> VodTile(item, vm::openVod) }
        }
    }
}

@Composable
private fun SportsScreen(vm: UnitvViewModel) {
    ScreenFrame("Esportes", "Agenda, detalhes, rankings e estatísticas") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vm.matches) { match ->
                Card(modifier = Modifier.fillMaxWidth().clickable { vm.showNotice("Detalhes do evento ${match.id} disponíveis para integração.") }, colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(match.competition, color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text("${match.homeTeam}  ×  ${match.awayTeam}", style = MaterialTheme.typography.titleLarge)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(match.dateLabel, color = Muted)
                            Text(match.status, color = Color(0xFF9CC7FF), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(vm: UnitvViewModel) {
    ScreenFrame("Perfil", "Conta, compras, cupons e configurações") {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(vm.session.accountLabel, style = MaterialTheme.typography.headlineSmall)
                    Text(vm.session.membershipLabel, color = Gold)
                    Text(if (vm.session.isAuthenticated) "Sessão ativa" else "Acesse sua conta para continuar", color = Muted)
                    Spacer(Modifier.height(8.dp))
                    if (vm.session.isAuthenticated) {
                        OutlinedButton(onClick = vm::logout) { Text("Sair") }
                    } else {
                        Button(onClick = vm::openLogin, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF211A0B))) { Text("Entrar") }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileAction("Compras e planos", "Escolha um plano de demonstração") { vm.openAccountScreen(AppScreen.PURCHASE) }
                ProfileAction("Cupons", "Consulte benefícios disponíveis") { vm.openAccountScreen(AppScreen.COUPONS) }
                ProfileAction("Segurança da conta", "Senha, vínculo e controle parental") { vm.openAccountScreen(AppScreen.ACCOUNT_SECURITY) }
                ProfileAction("Configurações", "Player, legendas e preferências") { vm.openAccountScreen(AppScreen.SETTINGS) }
            }
        }
    }
}

@Composable
private fun ProfileAction(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Gold))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun VodDetailsScreen(vm: UnitvViewModel) {
    val item = vm.selectedVod
    ScreenFrame(item?.title ?: "Detalhes", item?.subtitle, back = { vm.selectSection(AppSection.VOD) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(26.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(width = 260.dp, height = 260.dp).clip(RoundedCornerShape(18.dp)).background(Color(0xFF3A4663)), contentAlignment = Alignment.Center) { Text("CAPA", color = Muted, letterSpacing = 2.sp) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("★ ${item?.rating ?: 0.0}", color = Gold, style = MaterialTheme.typography.titleLarge)
                Text("Sinopse e metadados do conteúdo", color = Muted)
                Text("Este detalhe representa as telas de VOD observadas, sem copiar imagens, textos licenciados ou endpoints.")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { vm.showNotice("A reprodução requer um PlayerGateway configurado.") }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF211A0B))) { Text("Assistir") }
                    OutlinedButton(onClick = { vm.showNotice("Adicionado aos favoritos locais.") }) { Text("Favoritar") }
                }
            }
        }
    }
}

@Composable
private fun SearchScreen(vm: UnitvViewModel) {
    ScreenFrame("Buscar conteúdo", "Filmes, séries, diretores ou atores", back = { vm.selectSection(AppSection.VOD) }) {
        OutlinedTextField(value = vm.searchQuery, onValueChange = { vm.searchQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Digite uma busca") }, singleLine = true)
        Spacer(Modifier.height(18.dp))
        if (vm.filteredVod.isEmpty()) {
            Text("Nenhum resultado. Tente outro termo.", color = Muted)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) { items(vm.filteredVod) { VodTile(it, vm::openVod) } }
        }
    }
}

@Composable
private fun LoginScreen(vm: UnitvViewModel) {
    var account = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    var password = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    ScreenFrame("Entrar", "A autenticação real deve ser fornecida por um AuthRepository autorizado", back = { vm.selectSection(AppSection.HOME) }) {
        Column(modifier = Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(value = account.value, onValueChange = { account.value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Conta ou e-mail") }, singleLine = true)
            OutlinedTextField(value = password.value, onValueChange = { password.value = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(onClick = { vm.login(account.value) }, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color(0xFF211A0B))) { Text("Entrar localmente") }
            Text("Esqueci a senha", color = Gold, modifier = Modifier.clickable { vm.showNotice("Fluxo de recuperação reservado ao backend legítimo.") })
        }
    }
}

@Composable
private fun PurchaseScreen(vm: UnitvViewModel) {
    ScreenFrame("Planos", "Os valores abaixo são fictícios e servem apenas para demonstrar a tela", back = { vm.selectSection(AppSection.PROFILE) }) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(vm.plans) { plan ->
                Card(modifier = Modifier.width(240.dp), colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(plan.title, style = MaterialTheme.typography.titleLarge)
                        Text(plan.highlight, color = Gold, fontSize = 12.sp)
                        Text(plan.duration, color = Muted)
                        Text(plan.price, style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { vm.showNotice("Checkout desativado no scaffold. Conecte um gateway autorizado.") }) { Text("Selecionar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponsScreen(vm: UnitvViewModel) {
    ScreenFrame("Cupons", "Benefícios e validade", back = { vm.selectSection(AppSection.PROFILE) }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vm.coupons) { coupon ->
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(coupon.title, fontWeight = FontWeight.SemiBold)
                            Text(coupon.description, color = Muted)
                            Text(coupon.expires, color = Gold, fontSize = 12.sp)
                        }
                        if (coupon.claimed) Text("Usado", color = Muted) else OutlinedButton(onClick = { vm.showNotice("Cupom reservado no estado local.") }) { Text("Resgatar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: UnitvViewModel) {
    ScreenFrame("Configurações", "Preferências do player e da interface", back = { vm.selectSection(AppSection.PROFILE) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingRow("Legendas", "Idioma e estilo") { vm.showNotice("Preferências de legenda prontas para persistência local.") }
            SettingRow("Áudio", "Faixa e idioma") { vm.showNotice("Seleção de áudio depende do player configurado.") }
            SettingRow("Proporção da tela", "Ajuste para 16:9, preencher ou adaptar") { vm.showNotice("Opções de proporção modeladas no contrato do player.") }
            SettingRow("Limpar histórico", "Remove registros locais") { vm.showNotice("Histórico local limpo.") }
        }
    }
}

@Composable
private fun SecurityScreen(vm: UnitvViewModel) {
    ScreenFrame("Segurança da conta", "Senha, vínculo e controle parental", back = { vm.selectSection(AppSection.PROFILE) }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingRow("Alterar senha", "Atualize a senha de acesso") { vm.showNotice("Fluxo de alteração requer AuthRepository.") }
            SettingRow("Controle parental", "Proteja conteúdo restrito") { vm.showNotice("Controle parental local demonstrativo.") }
            SettingRow("Vincular e-mail ou telefone", "Melhore a recuperação da conta") { vm.showNotice("Vínculo requer backend legítimo e verificação.") }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = SurfaceRaised)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 13.sp)
            }
            Text("›", color = Gold, fontSize = 26.sp)
        }
    }
}
