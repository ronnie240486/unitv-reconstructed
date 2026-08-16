package com.example.unitv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ViewGroup
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView

private val PrestigieGold = Color(0xFFE6B85C)
private val Wine = Color(0xFF47090E)
private val WineDark = Color(0xFF16060A)
private val WinePanel = Color(0xB51F0A12)
private val TextMuted = Color(0xFFC7B9BC)
private val FocusBlue = Color(0xFF9FD5FF)

private fun formatDuration(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    val minutes = safe / 60
    val remaining = safe % 60
    return "%02d:%02d".format(minutes, remaining)
}

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
        vm.attachContext(context)
        vm.updateDeviceId(DeviceIdentity.read12(context))
    }
    LaunchedEffect(vm.deviceAccess?.allowed, vm.playlists, vm.catalogReady, vm.catalogLoading, vm.catalogError) {
        if ((vm.deviceAccess?.allowed == true || ProductConfig.api.useDemoData) && vm.playlists.isNotEmpty() && (vm.catalogReady || vm.catalogLoading) && vm.catalogError == null) {
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
                modifier = Modifier.fillMaxWidth(0.82f).widthIn(max = 360.dp).height(96.dp),
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
    var copied by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.prestigie_catalog_style),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xDD16060A), Color(0xF916060A)))))
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f).widthIn(max = 620.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.prestigie_logo),
                contentDescription = "Prestigie",
                modifier = Modifier.fillMaxWidth(0.78f).widthIn(max = 300.dp).height(82.dp),
                contentScale = ContentScale.Fit
            )
            Text("Ative seu aparelho", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Copie este código e envie ao seu revendedor para ativar seu acesso.", color = TextMuted, fontSize = 15.sp)
            Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("MAC / ID DO APARELHO", color = PrestigieGold, fontSize = 12.sp, letterSpacing = 2.sp)
                    Text(vm.macAddress, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Column(modifier = Modifier.fillMaxWidth(0.86f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("MAC do aparelho", vm.macAddress))
                                copied = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrestigieGold, contentColor = WineDark)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copiar")
                        }
                    }
                }
            }
            if (copied) {
                Text("MAC copiado", color = Color(0xFF78E39A), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            when {
                vm.accessLoading -> Text("Validando aparelho no servidor…", color = TextMuted)
                vm.deviceAccess?.allowed == true && vm.playlistsLoading && !vm.catalogLoading -> Text("Aparelho reconhecido · carregando listas do painel…", color = PrestigieGold)
                vm.deviceAccess?.allowed == true && vm.catalogLoading -> {
                    val progress = vm.catalogProgress
                    val percent = progress.percent.coerceIn(0, 100)
                    Column(
                        modifier = Modifier.fillMaxWidth(0.92f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Importando conteúdo… $percent%", color = PrestigieGold, fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(
                            progress = percent / 100f,
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                            color = PrestigieGold,
                            trackColor = Color(0x663F2225)
                        )
                        val remaining = progress.remainingSeconds
                        val timeLabel = if (remaining != null) {
                            "Tempo decorrido ${formatDuration(progress.elapsedSeconds)} · faltam aproximadamente ${formatDuration(remaining)}"
                        } else {
                            "Tempo decorrido ${formatDuration(progress.elapsedSeconds)} · calculando o tempo restante"
                        }
                        Text(
                            if (progress.estimated) "$timeLabel · progresso estimado" else timeLabel,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                vm.deviceAccess?.allowed == true && vm.catalogError != null -> Text(vm.catalogError.orEmpty(), color = Color(0xFFFFB4AB))
                vm.deviceAccess?.allowed == true && vm.catalogReady -> Text("Aparelho liberado · catálogo carregado", color = Color(0xFF78E39A))
                vm.deviceAccess != null -> Text("Acesso indisponível para este aparelho.", color = Color(0xFFFFB4AB))
                vm.playlistsError != null -> Text(vm.playlistsError.orEmpty(), color = Color(0xFFFFB4AB))
            }
            if (vm.catalogError != null) {
                Text("O aplicativo continuará tentando automaticamente…", color = TextMuted, fontSize = 12.sp)
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
    val actions = listOf(
        HeaderAction(Icons.Default.Search, "Buscar", vm::openSearch),
        HeaderAction(Icons.Default.FilterList, "Filtros", vm::openFilters),
        HeaderAction(Icons.Default.History, "Histórico", vm::openHistory),
        HeaderAction(Icons.Default.List, "Listas", vm::openLists),
        HeaderAction(Icons.Default.HelpOutline, "Ajuda", vm::openHelp),
        HeaderAction(Icons.Default.Notifications, "Notificações", vm::openNotifications)
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().background(Color(0xAA26070D))) {
        val compact = maxWidth < 720.dp
        if (compact) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    BrandLogo(vm, 145.dp, 46.dp)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Wifi, contentDescription = "Conectado", tint = Color(0xFF6FE38F), modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF57DB73)))
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(actions) { action -> HeaderIconButton(action.icon, action.label, action.action) }
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 28.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(vm, 185.dp, 52.dp)
                Spacer(Modifier.width(24.dp))
                Spacer(Modifier.weight(1f))
                actions.forEach { action -> HeaderIconButton(action.icon, action.label, action.action) }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Wifi, contentDescription = "Conectado", tint = Color(0xFF6FE38F), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text("11:59", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF57DB73)))
            }
        }
    }
}

@Composable
private fun BrandLogo(vm: UnitvViewModel, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    if (vm.visualConfig.logoUrl.isBlank()) {
        Image(painter = painterResource(R.drawable.prestigie_logo), contentDescription = vm.visualConfig.appName, modifier = Modifier.width(width).height(height), contentScale = ContentScale.Fit)
    } else {
        AsyncImage(model = vm.visualConfig.logoUrl, contentDescription = vm.visualConfig.appName, modifier = Modifier.width(width).height(height), contentScale = ContentScale.Fit)
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
    val tabs = listOf("Home", "Destaques", "Canais", "Filmes", "Séries", "Kids", "Anime", "Explorar")
    LazyRow(
        modifier = Modifier.fillMaxWidth().height(68.dp).background(Color(0xB516060A)),
        contentPadding = PaddingValues(horizontal = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs) { tab ->
            val selected = vm.selectedCategory.equals(tab, ignoreCase = true)
            TextButton(onClick = { vm.selectCategory(tab) }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tab.uppercase(), color = if (selected) Color.White else TextMuted, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
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
        AppScreen.HIGHLIGHTS -> HighlightsScreen(vm)
        AppScreen.LIVE -> LiveScreen(vm)
        AppScreen.PLAYER -> PlayerScreen(vm)
        AppScreen.SERIES_EPISODES -> SeriesEpisodesScreen(vm)
        AppScreen.VOD -> CatalogScreen(vm, if (vm.selectedCategory.equals("Séries", true)) "Séries" else "Filmes", if (vm.selectedCategory.equals("Séries", true)) vm.catalog.series else vm.catalog.movies)
        AppScreen.SPORTS -> SportsScreen(vm)
        AppScreen.PROFILE -> ProfileScreen(vm)
        AppScreen.LISTS -> PlaylistScreen(vm)
        AppScreen.KIDS -> CatalogScreen(vm, "Kids", vm.allCatalog.filter { it.category.contains("kids", true) || it.title.contains("kids", true) })
        AppScreen.ANIME -> CatalogScreen(vm, "Anime", vm.allCatalog.filter { it.category.contains("anime", true) || it.title.contains("anime", true) })
        AppScreen.EXPLORE -> CatalogScreen(vm, "Explorar", vm.allCatalog)
        AppScreen.NOTIFICATIONS -> InfoListScreen("Notificações", "Avisos, novidades e recomendações", Icons.Default.Notifications, vm::backHome, listOf("Novidades do catálogo", "Seu conteúdo foi atualizado", "Confira os destaques da semana"))
        AppScreen.HISTORY -> InfoListScreen("Histórico", "Continue de onde parou", Icons.Default.History, vm::backHome, listOf("Conteúdos recentes", "Seu histórico será preenchido pelo player"))
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp))) {
                if (vm.visualConfig.bannerUrl.isBlank()) {
                    Image(painter = painterResource(R.drawable.prestigie_catalog_style), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    AsyncImage(model = vm.visualConfig.bannerUrl, contentDescription = "Banner Prestigie", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xF016060A), Color(0x4416060A), Color(0xE616060A)))))
                Column(Modifier.align(Alignment.CenterStart).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Bem-vindo ao ${vm.visualConfig.appName}", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Sua experiência de entretenimento em um só lugar", color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionButton("TV ao vivo", Icons.Default.PlayArrow) { vm.selectCategory("Ao vivo") }
                        ActionButton("Listas", Icons.Default.List, vm::openLists)
                    }
                }
            }
        }
        item {
            if (vm.catalogLoading && !vm.catalogReady) {
                Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = PrestigieGold, modifier = Modifier.size(25.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Carregando catálogo completo", color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Aguarde a importação da M3U…", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            } else if (vm.catalogError != null) {
                Text(vm.catalogError.orEmpty(), color = Color(0xFFFFB4AB))
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Canais", vm.catalog.live.size, Icons.Default.Wifi, Modifier.weight(1f)) { vm.selectCategory("Canais") }
                    SummaryCard("Filmes", vm.catalog.movies.size, Icons.Default.PlayArrow, Modifier.weight(1f)) { vm.selectCategory("Filmes") }
                    SummaryCard("Séries", vm.catalog.series.size, Icons.Default.Event, Modifier.weight(1f)) { vm.selectCategory("Séries") }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = WinePanel), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Lista ativa", color = PrestigieGold, fontSize = 12.sp, letterSpacing = 1.5.sp)
                        Text(vm.selectedPlaylist?.name ?: "Nenhuma lista selecionada", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("O conteúdo é carregado automaticamente ao iniciar", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
        item {
            Text("Acesse pelo menu superior: Destaques, Filmes, Séries, Kids e Anime.", color = TextMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun HighlightsScreen(vm: UnitvViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Destaques", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Conteúdos selecionados da sua lista", color = TextMuted)
                }
            }
        }
        item { CatalogSection("Canais em destaque", vm.catalog.live.take(12), vm) }
        item { CatalogSection("Filmes em destaque", vm.catalog.movies.take(12), vm) }
        item { CatalogSection("Séries em destaque", vm.catalog.series.take(12), vm) }
    }
}

@Composable
private fun SummaryCard(title: String, count: Int, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.onFocusChanged { focused = it.isFocused }.focusable().clickable(onClick = onClick).border(if (focused) 2.dp else 1.dp, if (focused) FocusBlue else Color(0x446D3942), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (focused) Color(0xCC8C5215) else WinePanel),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = PrestigieGold, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, color = TextMuted, fontSize = 12.sp)
                Text(count.toString(), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CatalogSection(title: String, contentItems: List<CatalogItem>, vm: UnitvViewModel) {
    if (contentItems.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Text("${contentItems.size}", color = PrestigieGold, fontSize = 13.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(contentItems) { item -> CatalogCard(item, vm) }
        }
    }
}

@Composable
private fun CatalogCard(item: CatalogItem, vm: UnitvViewModel) {
    val portrait = item.kind != CatalogKind.LIVE
    val width = if (portrait) 156.dp else 230.dp
    val height = if (portrait) 222.dp else 130.dp
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable { vm.openCatalogItem(item) }
            .clip(RoundedCornerShape(10.dp))
            .border(if (focused) 3.dp else 1.dp, if (focused) FocusBlue else Color(0x446D3942), RoundedCornerShape(10.dp))
    ) {
        if (item.imageUrl.isBlank()) {
            Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF5F1822), Color(0xFF1D0A10)))))
            Icon(
                imageVector = when (item.kind) {
                    CatalogKind.LIVE -> Icons.Default.Wifi
                    CatalogKind.MOVIE -> Icons.Default.PlayArrow
                    CatalogKind.SERIES -> Icons.Default.Event
                },
                contentDescription = null,
                tint = PrestigieGold,
                modifier = Modifier.align(Alignment.Center).size(42.dp)
            )
        } else {
            AsyncImage(model = item.imageUrl, contentDescription = item.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Box(Modifier.fillMaxWidth().height(72.dp).align(Alignment.BottomCenter).background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xF0000000)))))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(item.category, color = TextMuted, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CatalogScreen(vm: UnitvViewModel, title: String, contentItems: List<CatalogItem>) {
    var activeCategory by remember(title) { mutableStateOf("Todos") }
    val categories = listOf("Todos") + contentItems.map { it.category }.filter { it.isNotBlank() }.distinct()
    val filteredItems = if (activeCategory == "Todos") contentItems else contentItems.filter { it.category == activeCategory }
    ScreenFrame(title, "Conteúdo carregado da lista ativa", vm::backHome) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 12.dp)) {
                items(categories) { category ->
                    val selected = category == activeCategory
                    OutlinedButton(
                        onClick = { activeCategory = category },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) PrestigieGold else Color.Transparent,
                            contentColor = if (selected) WineDark else Color.White
                        )
                    ) { Text(category, maxLines = 1) }
                }
            }
            if (filteredItems.isEmpty()) {
                Text("Nenhum conteúdo disponível nesta categoria.", color = TextMuted)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(filteredItems) { item -> CatalogCard(item, vm) }
                }
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
                        Text("Nenhuma lista foi vinculada ainda. Copie o MAC e cadastre-o no backend. O aplicativo carregará automaticamente ao iniciar.", color = TextMuted)
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
    val liveItems = vm.catalog.live
    var activeCategory by remember(liveItems) { mutableStateOf("Todos") }
    val categories = listOf("Todos") + liveItems.map { it.category }.filter { it.isNotBlank() }.distinct()
    val filteredItems = if (activeCategory == "Todos") liveItems else liveItems.filter { it.category == activeCategory }
    ScreenFrame("Canais", "Todos os canais da lista ativa", vm::backHome) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 12.dp)) {
                items(categories) { category ->
                    val selected = category == activeCategory
                    OutlinedButton(
                        onClick = { activeCategory = category },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) PrestigieGold else Color.Transparent,
                            contentColor = if (selected) WineDark else Color.White
                        )
                    ) { Text(category, maxLines = 1) }
                }
            }
            if (filteredItems.isEmpty()) {
                Text("Nenhum canal foi encontrado nesta categoria.", color = TextMuted)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(filteredItems) { item -> CatalogCard(item, vm) }
                }
            }
        }
    }
}

@Composable
private fun PlayerScreen(vm: UnitvViewModel) {
    val context = LocalContext.current
    var playbackError by remember(vm.playingUrl) { mutableStateOf<String?>(null) }
    val player = remember(vm.playingUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(vm.playingUrl))
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = "Não foi possível reproduzir este conteúdo. Verifique a URL ou o acesso do servidor."
                }
            })
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    ScreenFrame(vm.playingTitle.ifBlank { "Reprodução" }, "Player Prestigie", vm::closePlayer) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize()
                )
            }
            playbackError?.let { Text(it, color = Color(0xFFFFB4AB)) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton("Voltar", Icons.Default.ArrowBack, vm::closePlayer)
                ActionButton("Tentar novamente", Icons.Default.Refresh) { player.prepare(); player.playWhenReady = true }
            }
        }
    }
}

@Composable
private fun SeriesEpisodesScreen(vm: UnitvViewModel) {
    val series = vm.selectedCatalogItem
    ScreenFrame(series?.title ?: "Série", "Temporadas e episódios", vm::backHome) {
        when {
            vm.episodesLoading -> Text("Carregando episódios…", color = TextMuted)
            vm.episodesError != null && vm.seriesEpisodes.isEmpty() -> Text(vm.episodesError.orEmpty(), color = Color(0xFFFFB4AB))
            vm.seriesEpisodes.isEmpty() -> Text("Nenhum episódio disponível.", color = TextMuted)
            else -> LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(vm.seriesEpisodes) { episode ->
                    FocusRow(
                        title = "T${episode.season} · E${episode.episode} · ${episode.title}",
                        subtitle = "Assistir episódio",
                        icon = Icons.Default.PlayArrow
                    ) { vm.openEpisode(episode) }
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
        CatalogSection("Para você", vm.catalog.movies + vm.catalog.series, vm)
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
            listOf("Canais", "Filmes", "Séries", "Kids", "Anime", "Esportes").forEach { label ->
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
    ScreenFrame("Busca", "Filmes, séries ou canais da lista ativa", vm::backHome) {
        OutlinedTextField(value = vm.searchQuery, onValueChange = { vm.searchQuery = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Digite uma busca") }, singleLine = true)
        Spacer(Modifier.height(16.dp))
        if (vm.filteredCatalog.isEmpty()) {
            Text("Nenhum resultado. Tente outro termo.", color = TextMuted)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(vm.filteredCatalog) { item -> CatalogCard(item, vm) }
            }
        }
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
