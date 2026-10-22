package com.musicplayer.app.data.local

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicplayer.app.data.model.Song
import java.io.File

class MusicRepository(private val context: Context) {

    fun scanDeviceMusic(): List<Song> {
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            projection.add(MediaStore.Audio.Media.GENRE)
        }

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection.toTypedArray(),
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Sin título"
                    val rawArtist = cursor.getString(artistCol) ?: "Artista desconocido"
                    val artist = if (rawArtist == "<unknown>") "Artista desconocido" else rawArtist
                    val album = cursor.getString(albumCol) ?: "Álbum desconocido"
                    val path = cursor.getString(dataCol) ?: ""
                    val folder = if (path.isNotEmpty()) File(path).parentFile?.name ?: "Memoria Interna" else "Memoria Interna"
                    val genre = if (genreCol != -1) cursor.getString(genreCol) ?: "General" else "General"

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    ).toString()

                    songList.add(
                        Song(
                            id = id.toString(),
                            title = title,
                            artist = artist,
                            album = album,
                            genre = genre.ifBlank { "General" },
                            folder = folder,
                            uri = contentUri,
                            isStream = false,
                            dateAdded = cursor.getLong(dateAddedCol)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songList
    }

    /**
     * Android 11+ (API 30): crea la solicitud de borrado que muestra al usuario
     * el diálogo del sistema para confirmar la eliminación de los archivos.
     * Devuelve null si no aplica o si falla.
     */
    fun createDeleteIntentSender(songs: List<Song>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || songs.isEmpty()) return null
        return try {
            val uris = songs.map { Uri.parse(it.uri) }
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Android 10 o menor: borrado directo. En Android 10 exactamente puede fallar
     * para archivos de otras apps (requiere permiso especial).
     */
    fun deleteSongsLegacy(songs: List<Song>) {
        songs.forEach { song ->
            try {
                context.contentResolver.delete(Uri.parse(song.uri), null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}