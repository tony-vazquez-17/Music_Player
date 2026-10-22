package com.musicplayer.app.data.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val folder: String,
    val uri: String,
    val isStream: Boolean = false,
    val dateAdded: Long = 0L
)

enum class SortOption(val label: String){
    TITLE("Nombre de la canción"),
    ARTIST("Nombre del artista"),
    ALBUM("Nombre del albun"),
    DATE_ADDED("Tiempo añadido")
}