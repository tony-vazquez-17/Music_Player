package com.musicplayer.app.data.local

import android.content.Context

//Guarda los nombres de las carpetas que el usuario decidió ocultar
class HiddenFoldersStorage(context: Context) {
    private val prefs = context.getSharedPreferences("hidden_folders", Context.MODE_PRIVATE)

    fun getHiddenFolders(): Set<String> =
        prefs.getStringSet(KEY_HIDDEN, emptySet())?.toSet() ?: emptySet()

    fun saveHiddenFolders(folders: Set<String>) {
        prefs.edit().putStringSet(KEY_HIDDEN, folders).apply()
    }

    private companion object {
        const val KEY_HIDDEN = "hidden_folder_names"
    }
}