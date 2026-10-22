package com.musicplayer.app.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.app.data.local.HiddenFoldersStorage
import com.musicplayer.app.data.local.LastSongStorage
import com.musicplayer.app.data.local.MusicRepository
import com.musicplayer.app.data.model.Song
import com.musicplayer.app.data.model.SortOption
import com.musicplayer.app.service.MusicPlayerController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = LastSongStorage(application)
    private val hiddenStorage = HiddenFoldersStorage(application)
    private val repository = MusicRepository(application)
    private val playerController = MusicPlayerController(application) {
        onTrackEnded()
    }

    // Todas las canciones escaneadas (incluidas las de carpetas ocultas)
    private var allScannedSongs: List<Song> = emptyList()

    // Canciones visibles en la app (sin las carpetas ocultas)
    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())
    val localSongs: StateFlow<List<Song>> = _localSongs

    // Nombres de las carpetas ocultas
    private val _hiddenFolders = MutableStateFlow<Set<String>>(emptySet())
    val hiddenFolders: StateFlow<Set<String>> = _hiddenFolders

    // Cola de reproducción activa
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue

    // Puede ser null si todavía no hay ninguna canción seleccionada
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Estado de Ordenamiento (Por defecto: Nombre de la canción - Ascendente)
    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending

    // Estados de Aleatorio y Repetir
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled

    private val _isRepeatEnabled = MutableStateFlow(false)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled

    // Estados para la sección "Para ti"
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _recentIds = MutableStateFlow<List<String>>(emptyList())
    val recentIds: StateFlow<List<String>> = _recentIds

    private val _playCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playCounts: StateFlow<Map<String, Int>> = _playCounts

    init {
        _favoriteIds.value = storage.getFavoriteIds()
        _recentIds.value = storage.getRecentIds()
        _playCounts.value = storage.getPlayCounts()
        _hiddenFolders.value = hiddenStorage.getHiddenFolders()
        loadLocalSongs()
        restoreLastSelectedSong()
        startProgressUpdater()
    }

    fun loadLocalSongs() {
        allScannedSongs = repository.scanDeviceMusic()
        refreshLocalSongs()
        if (_currentQueue.value.isEmpty()) {
            _currentQueue.value = _localSongs.value
        }
    }

    /** Aplica el filtro de carpetas ocultas y el ordenamiento actual. */
    private fun refreshLocalSongs() {
        val hidden = _hiddenFolders.value
        val visible = allScannedSongs.filter { it.folder !in hidden }
        _localSongs.value = sortSongsList(visible, _sortOption.value, _sortAscending.value)
    }

    // ---------- Carpetas: ocultar / mostrar ----------

    fun hideFolder(folderName: String) {
        val updated = _hiddenFolders.value + folderName
        _hiddenFolders.value = updated
        hiddenStorage.saveHiddenFolders(updated)
        refreshLocalSongs()
    }

    fun unhideFolder(folderName: String) {
        val updated = _hiddenFolders.value - folderName
        _hiddenFolders.value = updated
        hiddenStorage.saveHiddenFolders(updated)
        refreshLocalSongs()
    }

    // ---------- Carpetas: eliminar ----------

    /** Android 11+: devuelve el diálogo del sistema para confirmar el borrado. */
    fun createDeleteIntentSender(songs: List<Song>): IntentSender? =
        repository.createDeleteIntentSender(songs)

    /** Android 10 o menor: borra directamente y actualiza la lista. */
    fun deleteSongsLegacy(songs: List<Song>) {
        repository.deleteSongsLegacy(songs)
        onSongsDeleted(songs)
    }

    /** Se llama cuando los archivos ya fueron eliminados del dispositivo. */
    fun onSongsDeleted(deleted: List<Song>) {
        val ids = deleted.map { it.id }.toSet()

        // Si la canción actual fue eliminada, se pausa y se limpia
        val current = _currentSong.value
        if (current != null && current.id in ids) {
            if (_isPlaying.value) {
                _isPlaying.value = playerController.togglePlayPause()
            }
            _currentSong.value = null
            _progress.value = 0f
            _currentPositionMs.value = 0L
        }

        _currentQueue.value = _currentQueue.value.filter { it.id !in ids }
        loadLocalSongs()
    }

    private fun restoreLastSelectedSong() {
        val savedSong = storage.getLastSong()?.takeIf { saved ->
            // Si ya se escaneó música, solo restaura la canción si todavía existe
            allScannedSongs.isEmpty() || allScannedSongs.any { it.id == saved.id }
        } ?: return
        _currentSong.value = savedSong
        playerController.prepareSongFromStart(savedSong, autoPlay = false)
        _progress.value = 0f
        _currentPositionMs.value = 0L
    }

    private fun startProgressUpdater() {
        viewModelScope.launch {
            while (isActive) {
                _isPlaying.value = playerController.isPlaying()
                _currentPositionMs.value = playerController.getCurrentPositionMs()
                _durationMs.value = playerController.getDurationMs()
                if (_isPlaying.value) {
                    _progress.value = playerController.getCurrentProgress()
                }
                delay(250L)
            }
        }
    }

    /**
     * Carga la lista completa a la que pertenece la canción seleccionada
     * y comienza a reproducir desde esa canción.
     */
    fun selectSong(song: Song, queue: List<Song> = _localSongs.value) {
        val targetQueue = if (queue.isNotEmpty()) queue.toList() else listOf(song)
        _currentQueue.value = targetQueue
        _currentSong.value = song
        storage.saveLastSong(song)

        // Actualizar Recientes
        val updatedRecents = listOf(song.id) + _recentIds.value.filter { it != song.id }
        val trimmedRecents = updatedRecents.take(30)
        _recentIds.value = trimmedRecents
        storage.saveRecentIds(trimmedRecents)

        // Actualizar Más escuchadas
        _playCounts.value = storage.incrementPlayCount(song.id)

        playerController.prepareSongFromStart(song, autoPlay = true)
        _isPlaying.value = true
        _progress.value = 0f
        _currentPositionMs.value = 0L
    }

    fun toggleFavorite(song: Song? = _currentSong.value) {
        if (song == null) return
        val currentFavs = _favoriteIds.value.toMutableSet()
        if (currentFavs.contains(song.id)) {
            currentFavs.remove(song.id)
        } else {
            currentFavs.add(song.id)
        }
        _favoriteIds.value = currentFavs
        storage.saveFavoriteIds(currentFavs)
    }

    fun onPlayPauseClick() {
        _isPlaying.value = playerController.togglePlayPause()
    }

    private fun onTrackEnded() {
        val song = _currentSong.value
        if (_isRepeatEnabled.value && song != null) {
            playerController.prepareSongFromStart(song, autoPlay = true)
            _isPlaying.value = true
        } else {
            nextSong()
        }
    }

    fun nextSong() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _currentSong.value?.id }

        val nextIndex = if (_isShuffleEnabled.value && queue.size > 1) {
            val candidates = queue.indices.filter { it != currentIndex }
            candidates.random()
        } else {
            if (currentIndex in 0 until queue.lastIndex) currentIndex + 1 else 0
        }
        selectSong(queue[nextIndex], queue)
    }

    fun previousSong() {
        val queue = _currentQueue.value
        if (queue.isEmpty()) return
        val currentIndex = queue.indexOfFirst { it.id == _currentSong.value?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else queue.lastIndex
        selectSong(queue[prevIndex], queue)
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun seekTo(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        playerController.seekToFraction(clamped)
        _progress.value = clamped
        val dur = playerController.getDurationMs()
        if (dur > 0) {
            _currentPositionMs.value = (dur * clamped).toLong()
        }
    }

    /**
     * Lógica de ordenamiento:
     * - Al elegir una opción por primera vez: ordena Ascendente.
     * - Si se vuelve a tocar la misma opción activa: invierte a Descendente (y viceversa).
     */
    fun selectSortOption(option: SortOption) {
        if (_sortOption.value == option) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortOption.value = option
            _sortAscending.value = true
        }
        _localSongs.value = sortSongsList(_localSongs.value, _sortOption.value, _sortAscending.value)
    }

    private fun sortSongsList(
        list: List<Song>,
        option: SortOption,
        ascending: Boolean
    ): List<Song> {
        return when (option) {
            SortOption.TITLE -> if (ascending) {
                list.sortedBy { it.title.lowercase() }
            } else {
                list.sortedByDescending { it.title.lowercase() }
            }
            SortOption.ARTIST -> if (ascending) {
                list.sortedBy { it.artist.lowercase() }
            } else {
                list.sortedByDescending { it.artist.lowercase() }
            }
            SortOption.ALBUM -> if (ascending) {
                list.sortedBy { it.album.lowercase() }
            } else {
                list.sortedByDescending { it.album.lowercase() }
            }
            SortOption.DATE_ADDED -> if (ascending) {
                list.sortedBy { it.dateAdded }
            } else {
                list.sortedByDescending { it.dateAdded }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
    }
}