package com.example.unitv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private val PrestigieGold = Color(0xFFE6B85C)
private val Wine = Color(0xFF47090E)
private val WineDark = Color(0xFF16060A)
private val WinePanel = Color(0xB51F0A12)
private val TextMuted = Color(0xFFC7B9BC)
private val FocusBlue = Color(0xFF9FD5FF)

private data class HeaderAction(val icon: ImageVector, val label: String, val action: () -> Unit)

@Composable
fun UnitvApp(vm: UnitvViewModel = viewModel()) {
    val context = LocalContext.current
    var showIntro by remember { mutableStateOf(true) }
    var showDeviceSetup by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1500)
        showIntro = false
    }
    LaunchedEffect(context) {
        vm.updateDeviceId(DeviceIdentity.read12(context))
    }
    LaunchedEffect(vm.deviceAccess?.allowed, vm.playlists) {
        if ((vm.deviceAccess?.allowed == true || ProductConfig.api.useDemoData) && vm.playlists.isNotEmpty()) {
            delay(450)
            showDeviceSetup = false
            vm.backHome()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = WineDark) {
        when {
            showIntro -> PrestigieIntro()
            showDeviceSetup -> DeviceIdentityScreen(vm) {
                showDeviceSetup = false
                vm.backHome()
            }
            else -> PrestigieShell(vm)
        }
    }

    vm.notice?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissNotice,
            title = { Text("Prestigie") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = vm::dismissNotice) { Text("OK") } }
        )
    }
}

@Composable
private fun PrestigieIntro() {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.prestigie_catalog_style),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xAA16060A), Color(0xF016060A)))))
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.prestigie_logo),
                contentDescription = "Prestigie",
                modifier = Modifier.width(360.dp).height(96.dp),
                contentScale = ContentScale.Fit
            )
            Text("ENTRETENIMENTO COM PRESTÍGIO", color = PrestigieGold, fontSize = 13.sp, letterSpacing = 2.2.sp)
            Text("Carregando sua experiência", color = TextMuted, fontSize = 14.sp)
            Box(Modifier.width(220.dp).height(4.dp).clip(RoundedCornerShape(4.dp)).background(Color(0x663F2225))) {
                Box(Modifier.fillMaxWidth(0.64f).fillMaxHeight().background(PrestigieGold))
            }
        }
    }
}

@Composable
private fun DeviceIdentityScreen(vm: UnitvViewModel, onContinue: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.prestigie_catalog_style),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xDD16060A), Color(0xF916060A)))))
        Column(
            modifier = Modifier.align(Alignment.Center).width(620.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.prestigie_logo),
                contentDescription = "Prestigie",
                modifier = Modifier.width(300.dp).height(82.dp),
                contentScale = ContentScale.Fit
            )
            Text("Ative seu aparelho", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Copie o identificador abaixo e cadastre-o no seu backend para liberar as listas.", color = TextMuted, fontSize = 15.sp)
            Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MAC / ID DO APARELHO", color = PrestigieGold, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text(vm.deviceId, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text(vm.macAddress, color = TextMuted, fontSize = 13.sp, letterSpacing = 1.4.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MAC do aparelho", vm.macAddress))
                                vm.showNotice("MAC copiado no formato AA:BB:CC:DD:EE:FF. Cole o valor no backend para vincular as listas.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar")
                        }
                        OutlinedButton(onClick = vm::refreshPlaylists) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Verificar e atualizar")
                        }
                    }
                }
            }
            Text("A tela exibe 12 caracteres; o botão Copiar usa o formato MAC do backend.", color = TextMuted, fontSize = 12.sp)
            when {
                vm.accessLoading -> Text("Validando aparelho no servidor…", color = TextMuted)
                vm.deviceAccess?.allowed == true -> Text("Aparelho liberado · ${vm.deviceAccess?.status.orEmpty()}", color = Color(0xFF78E39A))
                vm.deviceAccess != null -> Text("Acesso indisponível para este aparelho.", color = Color(0xFFFFB4AB))
                vm.playlistsError != null -> Text(vm.playlistsError.orEmpty(), color = Color(0xFFFFB4AB))
            }
            Button(
                onClick = onContinue,
                enabled = ProductConfig.api.useDemoData || vm.deviceAccess?.allowed == true,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC8C5215), contentColor = Color.White)
            ) {
                Text("Continuar")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PrestigieShell(vm: UnitvViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (vm.visualConfig.backgroundUrl.isBlank()) {
            Image(
                painter = painterResource(R.drawable.prestigie_catalog_style),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = vm.visualConfig.backgroundUrl,
                contentDescription = "Fundo configurado",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Color(0xC814060A)))
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(vm)
            CategoryTabs(vm)
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 14.dp)) {
                ScreenContent(vm)
            }
        }
    }
}

@Composable
private fun TopBar(vm: UnitvViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .background(Color(0xAA26070D))
            .padding(horizontal = 28.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (vm.visualConfig.logoUrl.isBlank()) {
            Image(
                painter = painterResource(R.drawable.prestigie_logo),
                contentDescription = vm.visualConfig.appName,
                modifier = Modifier.width(185.dp).height(52.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            AsyncImage(
                model = vm.visualConfig.logoUrl,
                contentDescription = vm.visualConfig.appName,
                modifier = Modifier.width(185.dp).height(52.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(24.dp))
        Button(
            onClick = { vm.openAccountScreen(AppScreen.PURCHASE) },
            modifier = Modifier.height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC8C5215), contentColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp), tint = PrestigieGold)
            Spacer(Modifier.width(8.dp))
            Text("Seja um membro", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
        val actions = listOf(
            HeaderAction(Icons.Default.Search, "Buscar", vm::openSearch),
            HeaderAction(Icons.Default.FilterList, "Filtros", vm::openFilters),
            HeaderAction(Icons.Default.History, "Histórico", vm::openHistory),
            HeaderAction(Icons.Default.List, "Listas", vm::openLists),
            HeaderAction(Icons.Default.HelpOutline, "Ajuda", vm::openHelp),
            HeaderAction(Icons.Default.Notifications, "Notificações", vm::openNotifications)
        )
        actions.forEach { action -> HeaderIconButton(action.icon, action.label, action.action) }
        Spacer(Modifier.width(12.dp))
        Icon(Icons.Default.Wifi, contentDescription = "Conectado", tint = Color(0xFF6FE38F), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text("11:59", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF57DB73)))
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(50.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clip(RoundedCornerShape(25.dp))
            .background(if (focused) PrestigieGold else Color(0x552A1920))
            .border(if (focused) 2.dp else 1.dp, if (focused) Color.White else Color(0x66D6B9BC), RoundedCornerShape(25.dp))
    ) {
        Icon(icon, contentDescription = label, tint = if (focused) WineDark else Color.White, modifier = Modifier.size(23.dp))
    }
    Spacer(Modifier.width(8.dp))
}

@Composable
private fun CategoryTabs(vm: UnitvViewModel) {
    val tabs = listOf("Home", "Destaques", "Filmes", "Séries", "Kids", "Anime", "Explorar")
    Row(
        modifier = Modifier.fillMaxWidth().height(68.dp).background(Color(0xB516060A)).padding(horizontal = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEach { tab ->
            val selected = vm.selectedCategory.equals(tab, ignoreCase = true)
            TextButton(onClick = { vm.selectCategory(tab) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tab.uppercase(), color = if (selected) Color.White else TextMuted, fontSize = 17.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.width(if (selected) 52.dp else 0.dp).height(3.dp).clip(RoundedCornerShape(3.dp)).background(PrestigieGold))
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(vm: UnitvViewModel) {
    when (vm.currentScreen) {
        AppScreen.HOME -> HomeScreen(vm)
        AppScreen.LIVE -> LiveScreen(vm)
        AppScreen.VOD -> CatalogScreen(vm, "Filmes e séries", vm.vodItems.map { it.title }, listOf(R.drawable.prestigie_card_01, R.drawable.prestigie_card_03, R.drawable.prestigie_card_05, R.drawable.prestigie_card_04))
        AppScreen.SPORTS -> SportsScreen(vm)
        AppScreen.PROFILE -> ProfileScreen(vm)
        AppScreen.LISTS -> PlaylistScreen(vm)
        AppScreen.KIDS -> CatalogScreen(vm, "Kids", listOf("Pequenos exploradores", "Aventuras no céu", "Clube dos amigos", "O mapa dourado"), listOf(R.drawable.prestigie_card_02, R.drawable.prestigie_card_04, R.drawable.prestigie_card_02, R.drawable.prestigie_card_05))
        AppScreen.ANIME -> CatalogScreen(vm, "Anime", listOf("Navegador celeste", "A bússola de ouro", "Cidade orbital", "Guardião da aurora"), listOf(R.drawable.prestigie_card_05, R.drawable.prestigie_card_03, R.drawable.prestigie_card_01, R.drawable.prestigie_card_05))
        AppScreen.EXPLORE -> ExploreScreen(vm)
        AppScreen.NOTIFICATIONS -> InfoListScreen("Notificações", "Avisos, novidades e recomendações", Icons.Default.Notifications, vm::backHome, listOf("Nova seleção Prestigie disponível", "Seu catálogo foi atualizado", "Confira os destaques da semana"))
        AppScreen.HISTORY -> InfoListScreen("Histórico", "Continue de onde parou", Icons.Default.History, vm::backHome, listOf("Horizonte de Inverno · 42 min", "Cidade orbital · 18 min", "Arena Sports · 1 evento"))
        AppScreen.FILTERS -> FilterScreen(vm)
        AppScreen.HELP -> InfoListScreen("Central de ajuda", "Orientações para usar o Prestigie na TV", Icons.Default.HelpOutline, vm::backHome, listOf("Como navegar pelo controle remoto", "Como alterar legendas e áudio", "Como acessar o controle parental"))
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
private fun HomeScreen(vm: UnitvViewModel) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Destaques para você", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Uma seleção de filmes, séries, Kids e Anime", color = TextMuted, fontSize = 14.sp)
            }
            OutlinedButton(onClick = vm::openSearch, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x99D7C6C9))) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Buscar")
            }
        }
        EditorialGrid(vm)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Continue assistindo", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = vm::openHistory) { Text("Ver tudo", color = PrestigieGold) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(listOf(R.drawable.prestigie_card_03, R.drawable.prestigie_card_04, R.drawable.prestigie_card_05)) { image ->
                ImageCard(image, "Retomar conteúdo", Modifier.width(230.dp).height(126.dp)) { vm.showNotice("A reprodução requer um PlayerGateway autorizado.") }
            }
        }
    }
}

@Composable
private fun EditorialGrid(vm: UnitvViewModel) {
    Row(modifier = Modifier.fillMaxWidth().height(360.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ImageCard(R.drawable.prestigie_card_01, "Horizonte de Inverno", Modifier.weight(1.9f).fillMaxHeight()) { vm.openVod(vm.vodItems.first()) }
        Column(modifier = Modifier.weight(0.82f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ImageCard(R.drawable.prestigie_card_02, "Kids", Modifier.weight(1f).fillMaxWidth()) { vm.selectCategory("Kids") }
            ImageCard(R.drawable.prestigie_card_05, "Anime", Modifier.weight(1f).fillMaxWidth()) { vm.selectCategory("Anime") }
        }
        Column(modifier = Modifier.weight(1.25f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ImageCard(R.drawable.prestigie_card_03, "Séries", Modifier.weight(1.3f).fillMaxWidth()) { vm.selectCategory("Séries") }
            ImageCard(R.drawable.prestigie_card_04, "Esportes", Modifier.weight(0.7f).fillMaxWidth()) { vm.selectSection(AppSection.SPORTS) }
        }
    }
}

@Composable
private fun ImageCard(image: Int, label: String, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .border(if (focused) 3.dp else 1.dp, if (focused) FocusBlue else Color(0x446D3942), RoundedCornerShape(10.dp))
    ) {
        Image(painter = painterResource(image), contentDescription = label, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = Modifier.fillMaxWidth().height(80.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6000000)))))
        Row(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PrestigieGold, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(5.dp))
            Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun CatalogScreen(vm: UnitvViewModel, title: String, labels: List<String>, images: List<Int>) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Categorias, recomendações e conteúdo selecionado", color = TextMuted)
            }
            Button(onClick = vm::openSearch, colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)) { Text("Buscar") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(listOf("Em destaque", "Mais vistos", "Novidades", "Favoritos", "Por gênero")) { category ->
                OutlinedButton(onClick = { vm.showNotice("Categoria: $category") }) { Text(category) }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(labels.indices.toList()) { index ->
                ImageCard(images[index % images.size], labels[index], Modifier.width(205.dp).height(285.dp)) { vm.openVod(vm.vodItems[index % vm.vodItems.size]) }
            }
        }
        ContentRow("Recomendados", vm.vodItems, vm::openVod)
    }
}

@Composable
private fun ContentRow(title: String, items: List<VodItem>, onClick: (VodItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items) { item ->
                ImageCard(R.drawable.prestigie_card_01, item.title, Modifier.width(190.dp).height(116.dp)) { onClick(item) }
            }
        }
    }
}

@Composable
private fun PlaylistScreen(vm: UnitvViewModel) {
    val context = LocalContext.current
    ScreenFrame("Listas", "Escolha qual lista deseja usar neste aparelho", vm::backHome) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MAC / ID DO APARELHO", color = PrestigieGold, fontSize = 11.sp, letterSpacing = 1.6.sp)
                        Text(vm.deviceId, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    }
                    OutlinedButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MAC do aparelho", vm.macAddress))
                        vm.showNotice("MAC copiado no formato AA:BB:CC:DD:EE:FF.")
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("Copiar")
                    }
                    Spacer(Modifier.width(8.dp))
                    ActionButton("Atualizar", Icons.Default.Refresh, vm::refreshPlaylists)
                }
            }
            if (vm.playlistsLoading) {
                Text("Consultando listas para este aparelho…", color = TextMuted)
            }
            vm.playlistsError?.let { error ->
                Text("Não foi possível carregar as listas: $error", color = Color(0xFFFFB4AB))
            }
            Text("Listas disponíveis", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (vm.playlists.isEmpty() && !vm.playlistsLoading) {
                Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PrestigieGold)
                        Spacer(Modifier.width(12.dp))
                        Text("Nenhuma lista foi vinculada ainda. Copie o MAC, cadastre-o no backend e toque em Atualizar.", color = TextMuted)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(vm.playlists) { playlist ->
                        val selected = vm.selectedPlaylistId == playlist.id
                        FocusRow(
                            title = playlist.name,
                            subtitle = if (selected) "Lista ativa" else "Toque para usar esta lista",
                            icon = if (selected) Icons.Default.CheckCircle else Icons.Default.List
                        ) { vm.selectPlaylist(playlist) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveScreen(vm: UnitvViewModel) {
    ScreenFrame("TV ao vivo", "Canais, EPG e controles para a sala", vm::backHome) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(modifier = Modifier.weight(0.9f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Canais", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vm.channels) { channel ->
                        FocusRow(title = channel.name, subtitle = "${channel.category} · ${channel.program}", icon = Icons.Default.PlayArrow) {
                            vm.showNotice("Canal selecionado: ${channel.name}. Conecte um PlayerGateway para reproduzir.")
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(248.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Reproduzir", tint = PrestigieGold, modifier = Modifier.size(54.dp))
                    Text("PLAYER AO VIVO", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), letterSpacing = 2.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Guia", Icons.Default.Event) { vm.showNotice("EPG demonstrativo aberto.") }
                    ActionButton("Áudio", Icons.Default.Tune) { vm.showNotice("Seleção de áudio disponível no player.") }
                    ActionButton("Legenda", Icons.Default.Info) { vm.showNotice("Seleção de legenda disponível no player.") }
                }
            }
        }
    }
}

@Composable
private fun SportsScreen(vm: UnitvViewModel) {
    ScreenFrame("Esportes", "Agenda, partidas e destaques", vm::backHome) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(vm.matches) { match ->
                FocusRow(title = "${match.homeTeam}  ×  ${match.awayTeam}", subtitle = "${match.competition} · ${match.dateLabel} · ${match.status}", icon = Icons.Default.Event) {
                    vm.showNotice("Detalhes do evento disponíveis para integração.")
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(vm: UnitvViewModel) {
    ScreenFrame("Perfil", "Conta, assinatura e preferências", vm::backHome) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Card(modifier = Modifier.weight(0.9f), colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrestigieGold, modifier = Modifier.size(52.dp))
                    Text(vm.session.accountLabel, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(vm.session.membershipLabel, color = PrestigieGold)
                    Text(if (vm.session.isAuthenticated) "Sessão ativa" else "Entre para salvar seus favoritos e continuar assistindo", color = TextMuted)
                    if (vm.session.isAuthenticated) OutlinedButton(onClick = vm::logout) { Text("Sair") }
                    else Button(onClick = vm::openLogin, colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)) { Text("Entrar") }
                }
            }
            Column(modifier = Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                FocusRow("Compras e planos", "Escolha uma assinatura", Icons.Default.ShoppingCart) { vm.openAccountScreen(AppScreen.PURCHASE) }
                FocusRow("Cupons", "Benefícios disponíveis", Icons.Default.LocalOffer) { vm.openAccountScreen(AppScreen.COUPONS) }
                FocusRow("Segurança da conta", "Senha e controle parental", Icons.Default.Lock) { vm.openAccountScreen(AppScreen.ACCOUNT_SECURITY) }
                FocusRow("Configurações", "Player, legendas e preferências", Icons.Default.Settings) { vm.openAccountScreen(AppScreen.SETTINGS) }
            }
        }
    }
}

@Composable
private fun ExploreScreen(vm: UnitvViewModel) {
    ScreenFrame("Explorar", "Descubra por gênero, tema, elenco e recomendação", vm::backHome) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Gêneros", Icons.Default.Tune) { vm.showNotice("Gêneros: drama, aventura, família, ficção e esportes.") }
            ActionButton("Mais vistos", Icons.Default.Star) { vm.showNotice("Lista de mais vistos atualizada.") }
            ActionButton("Novidades", Icons.Default.Add) { vm.showNotice("Novidades do catálogo abertas.") }
        }
        Spacer(Modifier.height(18.dp))
        ContentRow("Para você", vm.vodItems, vm::openVod)
    }
}

@Composable
private fun InfoListScreen(title: String, subtitle: String, icon: ImageVector, back: () -> Unit, entries: List<String>) {
    ScreenFrame(title, subtitle, back) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(entries) { entry -> FocusRow(entry, "Toque para abrir", icon) { } }
        }
    }
}

@Composable
private fun FilterScreen(vm: UnitvViewModel) {
    ScreenFrame("Filtros", "Refine sua busca", vm::backHome) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Filmes", "Séries", "Kids", "Anime", "Esportes").forEach { label ->
                FocusRow(label, "Selecionar categoria", Icons.Default.FilterList) { vm.selectCategory(label) }
            }
        }
    }
}

@Composable
private fun VodDetailsScreen(vm: UnitvViewModel) {
    val item = vm.selectedVod ?: vm.vodItems.first()
    ScreenFrame(item.title, "${item.subtitle} · ${item.year} · ★ ${item.rating}", vm::backHome) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Image(painter = painterResource(R.drawable.prestigie_card_01), contentDescription = item.title, modifier = Modifier.width(240.dp).fillMaxHeight(), contentScale = ContentScale.Crop)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("Detalhes do conteúdo", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Uma experiência original Prestigie para filmes, séries, novelas, desenhos e Anime. Este detalhe reproduz a estrutura observada sem copiar pôsteres ou textos licenciados.", color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionButton("Assistir", Icons.Default.PlayArrow) { vm.showNotice("A reprodução requer um PlayerGateway autorizado.") }
                    ActionButton("Favoritar", Icons.Default.FavoriteBorder) { vm.showNotice("Adicionado aos favoritos locais.") }
                }
                Text("Temporadas e episódios", color = Color.White, style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("T1", "T2", "T3")) { OutlinedButton(onClick = { vm.showNotice("Temporada $it selecionada") }) { Text(it) } } }
            }
        }
    }
}

@Composable
private fun SearchScreen(vm: UnitvViewModel) {
    ScreenFrame("Busca", "Filmes, séries, diretores, atores ou canais", vm::backHome) {
        OutlinedTextField(value = vm.searchQuery, onValueChange = { vm.searchQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Digite uma busca") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        if (vm.filteredVod.isEmpty()) Text("Nenhum resultado. Tente outro termo.", color = TextMuted)
        else ContentRow("Resultados", vm.filteredVod, vm::openVod)
    }
}

@Composable
private fun LoginScreen(vm: UnitvViewModel) {
    var account by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    ScreenFrame("Entrar", "Acesse seus favoritos, histórico e planos", vm::backHome) {
        Column(modifier = Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            OutlinedTextField(value = account, onValueChange = { account = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Conta ou e-mail") }, singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Senha") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(onClick = { vm.login(account) }, colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)) { Text("Entrar") }
            TextButton(onClick = { vm.showNotice("Recuperação de senha reservada ao backend autorizado.") }) { Text("Esqueci a senha") }
        }
    }
}

@Composable
private fun PurchaseScreen(vm: UnitvViewModel) {
    ScreenFrame("Seja um membro", "Planos demonstrativos para a experiência Prestigie", vm::backHome) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(vm.plans) { plan ->
                Card(modifier = Modifier.width(250.dp), colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Text(plan.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(plan.highlight, color = PrestigieGold)
                        Text(plan.duration, color = TextMuted)
                        Text(plan.price, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Button(onClick = { vm.showNotice("Checkout desativado nesta reconstrução. Conecte um gateway autorizado.") }, colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)) { Text("Selecionar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CouponsScreen(vm: UnitvViewModel) {
    ScreenFrame("Cupons", "Benefícios da sua conta", vm::backHome) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(vm.coupons) { coupon ->
                FocusRow(coupon.title, "${coupon.description} · ${coupon.expires}", Icons.Default.LocalOffer) { vm.showNotice(if (coupon.claimed) "Cupom já utilizado." else "Cupom reservado localmente.") }
            }
        }
    }
}

@Composable
private fun SettingsScreen(vm: UnitvViewModel) {
    ScreenFrame("Configurações", "Personalize sua experiência", vm::backHome) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FocusRow("Legendas", "Idioma e estilo", Icons.Default.Info) { vm.showNotice("Preferências de legenda prontas para persistência local.") }
            FocusRow("Áudio", "Faixa e idioma", Icons.Default.Tune) { vm.showNotice("Seleção de áudio depende do player configurado.") }
            FocusRow("Proporção da tela", "16:9, preencher ou adaptar", Icons.Default.Settings) { vm.showNotice("Opções de proporção modeladas no contrato do player.") }
            FocusRow("Limpar histórico", "Remover registros locais", Icons.Default.Refresh) { vm.showNotice("Histórico local limpo.") }
        }
    }
}

@Composable
private fun SecurityScreen(vm: UnitvViewModel) {
    ScreenFrame("Segurança da conta", "Senha, vínculo e controle parental", vm::backHome) {
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            FocusRow("Alterar senha", "Atualize sua senha", Icons.Default.Lock) { vm.showNotice("Fluxo requer AuthRepository autorizado.") }
            FocusRow("Controle parental", "Proteja conteúdo restrito", Icons.Default.Lock) { vm.showNotice("Controle parental local demonstrativo.") }
            FocusRow("Vincular contato", "E-mail ou telefone", Icons.Default.Person) { vm.showNotice("Vínculo requer backend legítimo.") }
        }
    }
}

@Composable
private fun ScreenFrame(title: String, subtitle: String, back: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back, modifier = Modifier.size(42.dp)) { Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White) }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, color = TextMuted, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun FocusRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color(0xCC8C5215) else WinePanel)
            .border(if (focused) 2.dp else 1.dp, if (focused) FocusBlue else Color(0x446D3942), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (focused) Color.White else PrestigieGold, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = if (focused) Color.White else TextMuted, fontSize = 12.sp)
        }
        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = if (focused) Color.White else TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.onFocusChanged { focused = it.isFocused }.focusable().border(if (focused) 2.dp else 1.dp, if (focused) FocusBlue else Color(0x779F7B80), RoundedCornerShape(22.dp)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (focused) WineDark else Color.White, containerColor = if (focused) PrestigieGold else Color.Transparent),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}
