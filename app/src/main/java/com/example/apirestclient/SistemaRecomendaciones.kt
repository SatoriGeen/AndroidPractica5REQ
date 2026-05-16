package com.example.apirestclient

import android.content.Context
import android.database.Cursor

class SistemaRecomendaciones(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    // Generar sugerencias basadas en favoritos e historial del usuario
    fun generarRecomendacionesPersonalizadas(usuarioId: String): String {
        val favoritosCursor = dbHelper.obtenerFavoritosUsuario(usuarioId)
        val historialCursor = dbHelper.obtenerHistorialUsuario(usuarioId)

        var conteoTVMaze = 0
        var conteoOpenLibrary = 0
        val palabrasClave = mutableListOf<String>()

        // Analiza preferencias en Favoritos
        if (favoritosCursor.moveToFirst()) {
            do {
                val tipoApi = favoritosCursor.getString(favoritosCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TIPO_API))
                val titulo = favoritosCursor.getString(favoritosCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TITULO))
                if (tipoApi == "TVMAZE") conteoTVMaze += 2 else conteoOpenLibrary += 2
                palabrasClave.add(titulo.split(" ")[0]) // Extrae la primera palabra del título
            } while (favoritosCursor.moveToNext())
        }
        favoritosCursor.close()

        // Analiza intereses en Historial de búsquedas
        if (historialCursor.moveToFirst()) {
            var limite = 0
            do {
                val tipoApi = historialCursor.getString(historialCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_TIPO_API))
                val query = historialCursor.getString(historialCursor.getColumnIndexOrThrow(DatabaseHelper.KEY_QUERY_BUSQUEDA))
                if (tipoApi == "TVMAZE") conteoTVMaze++ else conteoOpenLibrary++
                palabrasClave.add(query)
                limite++
            } while (historialCursor.moveToNext() && limite < 5) // Analiza las últimas 5 búsquedas
        }
        historialCursor.close()

        // Motor de decisión del Algoritmo
        val preferenciaApi = if (conteoTVMaze >= conteoOpenLibrary) "TVMAZE" else "OPEN_LIBRARY"
        val palabraMasBuscada = palabrasClave.groupBy { it }.maxByOrNull { it.value.size }?.key ?: "General"

        // Retorna la sugerencia estructurada que se pintará en la interfaz offline
        return if (preferenciaApi == "TVMAZE") {
            "Basado en tu interés por '$palabraMasBuscada', te recomendamos explorar las series en tendencia de Ciencia Ficción y Drama en TVMaze."
        } else {
            "Detectamos preferencia por la lectura. Te sugerimos buscar obras académicas y de fantasía afines a '$palabraMasBuscada' en Open Library."
        }
    }
}