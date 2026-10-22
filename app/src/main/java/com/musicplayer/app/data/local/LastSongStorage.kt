package com.musicplayer.app.data.local

import android.content.Context
import com.musicplayer.app.data.model.Song

class LastSongStorage(context: Context) {
    private val prefs = context.getSharedPreferences("music_prefs", Context.MODE_PRIVATE)

    fun saveLastSong(song: Song) {
        prefs.edit()
            .putString("LAST_ID", song.id)
            .putString("LAST_TITLE", song.title)
            .putString("LAST_ARTIST", song.artist)
            .putString("LAST_ALBUM", song.album)
            .putString("LAST_GENRE", song.genre)
            .putString("LAST_FOLDER", song.folder)
            .putString("LAST_URI", song.uri)
            .putBoolean("LAST_STREAM", song.isStream)
            .putLong("LAST_DATE_ADDED", song.dateAdded)
            .apply()
    }

    fun getLastSong(): Song? {
        val id = prefs.getString("LAST_ID", null) ?: return null
        val uri = prefs.getString("LAST_URI", null) ?: return null
        return Song(
            id = id,
            title = prefs.getString("LAST_TITLE", "Canción") ?: "Canción",
            artist = prefs.getString("LAST_ARTIST", "Artista desconocido") ?: "Artista desconocido",
            album = prefs.getString("LAST_ALBUM", "Álbum") ?: "Álbum",
            genre = prefs.getString("LAST_GENRE", "General") ?: "General",
            folder = prefs.getString("LAST_FOLDER", "Música") ?: "Música",
            uri = uri,
            isStream = prefs.getBoolean("LAST_STREAM", false),
            dateAdded = prefs.getLong("LAST_DATE_ADDED", 0L)
        )
    }

    //Favoritos
    fun saveFavoriteIds(ids: Set<String>){
        prefs.edit().putStringSet("FAVORITE_IDS", ids).apply()
    }

    fun getFavoriteIds(): Set<String>{
        return prefs.getStringSet("FAVORITE_IDS", emptySet()) ?: emptySet()
    }

    //Recientes
    fun saveRecentIds(ids: List<String>){
        prefs.edit().putString("RECENT_IDS", ids.joinToString(",")).apply() 
    }

    fun getRecentIds(): List<String> {
        val raw = prefs.getString("RECENT_IDS", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",").filter { it.isNotBlank() }
    }

    //Mas escuchadas
    fun incrementPlayCount(songId: String): Map<String, Int>{
        val currentMap = getPlayCounts().toMutableMap()
        currentMap[songId] = (currentMap[songId] ?: 0) + 1
        val serialized = currentMap.entries.joinToString(",") { "${it.key}:${it.value}" }
        prefs.edit().putString("PLAY_COUNTS", serialized).apply()
        return currentMap
    }

    fun getPlayCounts(): Map<String, Int> {
        val raw = prefs.getString("PLAY_COUNTS", "") ?: ""
        if  (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Int>()
        raw.split(",").forEach { pair ->
            val parts = pair.split(":")
            if (parts.size == 2){
                val count = parts[1].toIntOrNull() ?: 0
                map[parts[0]] = count
            }
         }
         return map
    }
}