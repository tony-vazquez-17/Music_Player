package com.musicplayer.app.ui

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicplayer.app.data.model.Song
import com.musicplayer.app.data.model.SortOption
import com.musicplayer.app.ui.components.FullPlayerScreen
import com.musicplayer.app.ui.components.MiniPlayerBar
import com.musicplayer.app.ui.components.ScrollableTabsBar
import com.musicplayer.app.ui.components.SongRowItem
import com.musicplayer.app.ui.theme.AppBackgroundBrush
import com.musicplayer.app.ui.theme.CoverArtBrush
import com.musicplayer.app.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    val tabs = listOf(
        "Para ti", "Canciones", "Carpetas", "Artistas",
        "Álbumes", "Géneros"
    )

    val pagerState = rememberPagerState(initialPage = 1, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val localSongs by viewModel.localSongs.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val sortOption by viewModel.sortOption.collectAsState()
    val sortAscending by viewModel.sortAscending.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val recentIds by viewModel.recentIds.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsState()
    val hiddenFolders by viewModel.hiddenFolders.collectAsState()

    // currentSong puede ser null; para las listas solo necesitamos el id (o vacío)
    val currentSongId = currentSong?.id ?: ""

    var showSearchBar by remember { mutableStateOf(false) }
    var showSettingsPanel by remember { mutableStateOf(false) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // Carpeta pendiente de confirmar eliminación: nombre + canciones que contiene
    var folderToDelete by remember { mutableStateOf<Pair<String, List<Song>>?>(null) }
    // Canciones enviadas a eliminar, a la espera de la respuesta del diálogo del sistema
    var pendingDeleteSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSongsDeleted(pendingDeleteSongs)
        }
        pendingDeleteSongs = emptyList()
    }

    // Manejo del botón Atrás del teléfono para cerrar paneles abiertos
    BackHandler(enabled = showFullPlayer || showSettingsPanel) {
        when {
            showFullPlayer -> showFullPlayer = false
            showSettingsPanel -> showSettingsPanel = false
        }
    }

    // Cálculo reactivo de las 3 listas de "Para ti"
    val allKnownSongs = localSongs

    val mostPlayedSongs = remember(allKnownSongs, playCounts) {
        val played = allKnownSongs.filter { (playCounts[it.id] ?: 0) > 0 }
            .sortedByDescending { playCounts[it.id] ?: 0 }
        if (played.isNotEmpty()) played else allKnownSongs.take(5)
    }

    val favoriteSongs = remember(allKnownSongs, favoriteIds) {
        allKnownSongs.filter { favoriteIds.contains(it.id) }
    }

    val recentSongs = remember(allKnownSongs, recentIds) {
        val mapped = recentIds.mapNotNull { id -> allKnownSongs.find { it.id == id } }
        if (mapped.isNotEmpty()) mapped else allKnownSongs.take(5)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                // El mini reproductor solo se muestra si hay una canción seleccionada
                currentSong?.let { song ->
                    MiniPlayerBar(
                        song = song,
                        isPlaying = isPlaying,
                        progress = progress,
                        onPlayPause = { viewModel.onPlayPauseClick() },
                        onNext = { viewModel.nextSong() },
                        onPrevious = { viewModel.previousSong() },
                        onOpenFullPlayer = { showFullPlayer = true }
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackgroundBrush)
                    .padding(paddingValues)
            ) {
                // 1. Menú Superior (Header): Buscador + Ícono Hamburguesa (Ajustes)
                if (showSearchBar) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text("Buscar canción o artista...", color = Color.White.copy(alpha = 0.5f)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.25f)
                            )
                        )
                        IconButton(onClick = {
                            viewModel.updateSearchQuery("")
                            showSearchBar = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda", tint = Color.White)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MusicPlayer",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showSearchBar = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Buscar",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = { showSettingsPanel = true }) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Ajustes",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // Barra de 6 pestañas desplazables
                ScrollableTabsBar(
                    tabs = tabs,
                    selectedTabIndex = pagerState.currentPage,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )

                // Contenido de cada pestaña
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val filteredLocal = if (searchQuery.isBlank()) {
                        localSongs
                    } else {
                        localSongs.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true) ||
                            it.album.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    when (pageIndex) {
                        // 2. Sección "Para ti": Únicamente "Más escuchadas", "Favoritas" y "Recientes"
                        0 -> ForYouSectionPage(
                            mostPlayedSongs = mostPlayedSongs,
                            favoriteSongs = favoriteSongs,
                            recentSongs = recentSongs,
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSelectSongFromCategory = { song, categoryQueue ->
                                viewModel.selectSong(song, categoryQueue)
                            }
                        )
                        // 3. Sección "Canciones": Botón de ordenamiento que abre el Bottom Sheet
                        1 -> SongListPage(
                            titleLabel = "${filteredLocal.size} canciones",
                            activeSortLabel = sortOption.label,
                            isAscending = sortAscending,
                            songs = filteredLocal,
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSortClick = { showSortBottomSheet = true },
                            onSongClick = { viewModel.selectSong(it, filteredLocal) }
                        )
                        // Carpetas: con menú de tres puntos (Ocultar / Eliminar)
                        2 -> CategoryGroupedPage(
                            categoryType = "Carpetas",
                            songs = filteredLocal,
                            groupSelector = { it.folder },
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSongClick = { song, groupQueue -> viewModel.selectSong(song, groupQueue) },
                            onHideGroup = { folderName -> viewModel.hideFolder(folderName) },
                            onDeleteGroup = { folderName ->
                                // Se usan TODAS las canciones de la carpeta, no solo las filtradas por búsqueda
                                folderToDelete = folderName to localSongs.filter { it.folder == folderName }
                            }
                        )
                        3 -> CategoryGroupedPage(
                            categoryType = "Artistas",
                            songs = filteredLocal,
                            groupSelector = { it.artist },
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSongClick = { song, groupQueue -> viewModel.selectSong(song, groupQueue) }
                        )
                        4 -> CategoryGroupedPage(
                            categoryType = "Álbumes",
                            songs = filteredLocal,
                            groupSelector = { it.album },
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSongClick = { song, groupQueue -> viewModel.selectSong(song, groupQueue) }
                        )
                        5 -> CategoryGroupedPage(
                            categoryType = "Géneros",
                            songs = filteredLocal,
                            groupSelector = { it.genre },
                            currentSongId = currentSongId,
                            favoriteIds = favoriteIds,
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onSongClick = { song, groupQueue -> viewModel.selectSong(song, groupQueue) }
                        )
                    }
                }
            }
        }

        // Diálogo de confirmación para eliminar una carpeta
        folderToDelete?.let { (folderName, songsToDelete) ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = { Text("Eliminar carpeta") },
                text = {
                    Text(
                        "Se eliminarán de forma permanente ${songsToDelete.size} canciones " +
                            "de \"$folderName\" del dispositivo. Esta acción no se puede deshacer."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val songs = songsToDelete
                        folderToDelete = null
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // Android 11+: el sistema pide confirmación y borra los archivos
                            val sender = viewModel.createDeleteIntentSender(songs)
                            if (sender != null) {
                                pendingDeleteSongs = songs
                                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                            }
                        } else {
                            viewModel.deleteSongsLegacy(songs)
                        }
                    }) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Bottom Sheet de Ordenamiento en "Canciones"
        if (showSortBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortBottomSheet = false },
                containerColor = Color(0xFF230E3B),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        text = "Ordenar canciones por",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    SortOption.values().forEach { option ->
                        val isSelected = sortOption == option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color.White.copy(alpha = 0.12f) else Color.Transparent
                                )
                                .clickable {
                                    viewModel.selectSortOption(option)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.label,
                                color = if (isSelected) Color(0xFFD0BCFF) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )

                            if (isSelected) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (sortAscending) "Ascendente" else "Descendente",
                                        color = Color(0xFFD0BCFF),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Icon(
                                        imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = null,
                                        tint = Color(0xFFD0BCFF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. Panel de Ajustes: carpetas ocultas (para volver a mostrarlas) y versión de la app abajo
        AnimatedVisibility(
            visible = showSettingsPanel,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable { showSettingsPanel = false }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.82f)
                        .align(Alignment.CenterEnd)
                        .background(AppBackgroundBrush)
                        .clickable(enabled = true, onClick = {})
                        .padding(24.dp)
                ) {
                    // Botón discreto superior para cerrar el panel
                    IconButton(
                        onClick = { showSettingsPanel = false },
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Cerrar ajustes",
                            tint = Color.White
                        )
                    }

                    // Carpetas ocultas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 56.dp, bottom = 40.dp)
                    ) {
                        Text(
                            text = "Carpetas ocultas",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        if (hiddenFolders.isEmpty()) {
                            Text(
                                text = "No tienes carpetas ocultas.",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                items(hiddenFolders.sorted(), key = { it }) { folderName ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = folderName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = { viewModel.unhideFolder(folderName) }) {
                                            Text("Mostrar", color = Color(0xFFD0BCFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Número de la versión anclado en la parte inferior
                    Text(
                        text = "Versión de la app: 1.0.0",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    )
                }
            }
        }

        // 4. Pantalla Completa del Reproductor (solo si hay una canción seleccionada)
        AnimatedVisibility(
            visible = showFullPlayer && currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentSong?.let { song ->
                FullPlayerScreen(
                    song = song,
                    isPlaying = isPlaying,
                    progress = progress,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    isFavorite = favoriteIds.contains(song.id),
                    isShuffleEnabled = isShuffleEnabled,
                    isRepeatEnabled = isRepeatEnabled,
                    onToggleFavorite = { viewModel.toggleFavorite(song) },
                    onSeek = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onPrevious = { viewModel.previousSong() },
                    onPlayPause = { viewModel.onPlayPauseClick() },
                    onNext = { viewModel.nextSong() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onClose = { showFullPlayer = false }
                )
            }
        }
    }
}

@Composable
fun ForYouSectionPage(
    mostPlayedSongs: List<Song>,
    favoriteSongs: List<Song>,
    recentSongs: List<Song>,
    currentSongId: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (Song) -> Unit,
    onSelectSongFromCategory: (Song, List<Song>) -> Unit
) {
    val categories = listOf(
        Triple("Más escuchadas", Icons.Default.TrendingUp, mostPlayedSongs),
        Triple("Favoritas", Icons.Default.Favorite, favoriteSongs),
        Triple("Recientes", Icons.Default.History, recentSongs)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        categories.forEach { (categoryTitle, icon, songList) ->
            item(key = "header_$categoryTitle") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$categoryTitle (${songList.size})",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (songList.isEmpty()) {
                item(key = "empty_$categoryTitle") {
                    Text(
                        text = "No hay canciones en $categoryTitle todavía.",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(songList, key = { "${categoryTitle}_${it.id}" }) { song ->
                    SongRowItem(
                        song = song,
                        isCurrentSong = song.id == currentSongId,
                        isFavorite = favoriteIds.contains(song.id),
                        onToggleFavorite = { onToggleFavorite(song) },
                        onClick = {
                            // Carga toda la lista de esta categoría en la cola y reproduce desde la canción seleccionada
                            onSelectSongFromCategory(song, songList)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SongListPage(
    titleLabel: String,
    activeSortLabel: String?,
    isAscending: Boolean,
    songs: List<Song>,
    currentSongId: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (Song) -> Unit,
    onSortClick: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = titleLabel,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )

            if (activeSortLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSortClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeSortLabel,
                        color = Color(0xFFD0BCFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = "Dirección de ordenamiento",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(songs, key = { it.id }) { song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = song.id == currentSongId,
                    isFavorite = favoriteIds.contains(song.id),
                    onToggleFavorite = { onToggleFavorite(song) },
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

/**
 * Si se pasan onHideGroup y onDeleteGroup, cada fila muestra un menú de tres puntos
 * con las opciones "Ocultar carpeta" y "Eliminar". Si no, muestra la flecha normal.
 */
@Composable
fun CategoryGroupedPage(
    categoryType: String,
    songs: List<Song>,
    groupSelector: (Song) -> String,
    currentSongId: String,
    favoriteIds: Set<String>,
    onToggleFavorite: (Song) -> Unit,
    onSongClick: (Song, List<Song>) -> Unit,
    onHideGroup: ((String) -> Unit)? = null,
    onDeleteGroup: ((String) -> Unit)? = null
) {
    val grouped = remember(songs) { songs.groupBy(groupSelector) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectedGroup == null) {
            Text(
                text = "${grouped.size} $categoryType",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(grouped.entries.toList(), key = { it.key }) { entry ->
                    var menuExpanded by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGroup = entry.key }
                            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CoverArtBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.key,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${entry.value.size} canciones",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }

                        if (onHideGroup != null && onDeleteGroup != null) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Más opciones",
                                        tint = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ocultar carpeta") },
                                        leadingIcon = {
                                            Icon(Icons.Default.VisibilityOff, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onHideGroup(entry.key)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar") },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null)
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteGroup(entry.key)
                                        }
                                    )
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    }
                }
            }
        } else {
            val groupSongs = grouped[selectedGroup] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedGroup = null }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$selectedGroup (${groupSongs.size})",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(groupSongs, key = { it.id }) { song ->
                    SongRowItem(
                        song = song,
                        isCurrentSong = song.id == currentSongId,
                        isFavorite = favoriteIds.contains(song.id),
                        onToggleFavorite = { onToggleFavorite(song) },
                        onClick = { onSongClick(song, groupSongs) }
                    )
                }
            }
        }
    }
}