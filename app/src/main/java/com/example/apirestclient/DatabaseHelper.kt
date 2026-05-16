package com.example.apirestclient

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "EscomApp_Nativa.db"
        private const val DATABASE_VERSION = 1

        // Nombres de Tablas
        const val TABLE_USUARIOS = "usuarios"
        const val TABLE_HISTORIAL = "historial"
        const val TABLE_FAVORITOS = "favoritos"

        // Columnas Comunes
        const val KEY_ID = "id"
        const val KEY_USUARIO_ID = "usuario_id"

        // Tabla Usuarios
        const val KEY_NOMBRE = "nombre"
        const val KEY_CORREO = "correo"
        const val KEY_ROL = "rol" // "USER" o "ADMIN"

        // Tabla Historial
        const val KEY_QUERY_BUSQUEDA = "query_busqueda"
        const val KEY_TIPO_API = "tipo_api" // "TVMAZE"
        const val KEY_TIMESTAMP = "timestamp"

        // Tabla Favoritos
        const val KEY_ELEMENTO_ID = "elemento_id"
        const val KEY_TITULO = "titulo"
        const val KEY_SUBTITULO_O_GENERO = "sub_o_genero"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Tabla de Usuarios
        val createUsuariosTable = ("CREATE TABLE " + TABLE_USUARIOS + "("
                + KEY_ID + " TEXT PRIMARY KEY," // Guardará el ID
                + KEY_NOMBRE + " TEXT,"
                + KEY_CORREO + " TEXT,"
                + KEY_ROL + " TEXT" + ")")

        // Tabla de Historial
        val createHistorialTable = ("CREATE TABLE " + TABLE_HISTORIAL + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USUARIO_ID + " TEXT,"
                + KEY_QUERY_BUSQUEDA + " TEXT,"
                + KEY_TIPO_API + " TEXT,"
                + KEY_TIMESTAMP + " LONG,"
                + "FOREIGN KEY(" + KEY_USUARIO_ID + ") REFERENCES " + TABLE_USUARIOS + "(" + KEY_ID + "))")

        // Tabla de Favoritos
        val createFavoritosTable = ("CREATE TABLE " + TABLE_FAVORITOS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_USUARIO_ID + " TEXT,"
                + KEY_ELEMENTO_ID + " TEXT,"
                + KEY_TITULO + " TEXT,"
                + KEY_SUBTITULO_O_GENERO + " TEXT,"
                + KEY_TIPO_API + " TEXT,"
                + "FOREIGN KEY(" + KEY_USUARIO_ID + ") REFERENCES " + TABLE_USUARIOS + "(" + KEY_ID + "))")

        db?.execSQL(createUsuariosTable)
        db?.execSQL(createHistorialTable)
        db?.execSQL(createFavoritosTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITOS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORIAL")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USUARIOS")
        onCreate(db)
    }

    // === OPERACIONES DE ESCRITURA (LOGICA DEL SISTEMA) ===

    fun registrarOSincronizarUsuario(id: String, nombre: String, correo: String, rol: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ID, id)
            put(KEY_NOMBRE, nombre)
            put(KEY_CORREO, correo)
            put(KEY_ROL, rol)
        }
        db.insertWithOnConflict(TABLE_USUARIOS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    fun agregarAlHistorial(usuarioId: String, query: String, tipoApi: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USUARIO_ID, usuarioId)
            put(KEY_QUERY_BUSQUEDA, query)
            put(KEY_TIPO_API, tipoApi)
            put(KEY_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_HISTORIAL, null, values)
        db.close()
    }

    fun agregarAFavoritos(usuarioId: String, elementoId: String, titulo: String, extraInfo: String, tipoApi: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USUARIO_ID, usuarioId)
            put(KEY_ELEMENTO_ID, elementoId)
            put(KEY_TITULO, titulo)
            put(KEY_SUBTITULO_O_GENERO, extraInfo)
            put(KEY_TIPO_API, tipoApi)
        }
        db.insert(TABLE_FAVORITOS, null, values)
        db.close()
    }

    // === OPERACIONES DE LECTURA (REQUISITOS USUARIO / ADMIN) ===

    // Usuario normal ve su propio historial
    fun obtenerHistorialUsuario(usuarioId: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_HISTORIAL WHERE $KEY_USUARIO_ID = ? ORDER BY $KEY_TIMESTAMP DESC", arrayOf(usuarioId))
    }

    // Usuario normal ve sus propios favoritos
    fun obtenerFavoritosUsuario(usuarioId: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_FAVORITOS WHERE $KEY_USUARIO_ID = ?", arrayOf(usuarioId))
    }

    // Visualizar historiales de todos los usuarios
    fun obtenerHistorialGlobalAdmin(): Cursor {
        val db = this.readableDatabase
        val query = """
            SELECT u.$KEY_NOMBRE, h.$KEY_QUERY_BUSQUEDA, h.$KEY_TIPO_API, h.$KEY_TIMESTAMP 
            FROM $TABLE_HISTORIAL h 
            INNER JOIN $TABLE_USUARIOS u ON h.$KEY_USUARIO_ID = u.$KEY_ID
            ORDER BY h.$KEY_TIMESTAMP DESC
        """.trimIndent()
        return db.rawQuery(query, null)
    }

    // Visualizar favoritos de todos los usuarios
    fun obtenerFavoritosGlobalesAdmin(): Cursor {
        val db = this.readableDatabase
        val query = """
            SELECT u.$KEY_NOMBRE, f.$KEY_TITULO, f.$KEY_TIPO_API 
            FROM $TABLE_FAVORITOS f 
            INNER JOIN $TABLE_USUARIOS u ON f.$KEY_USUARIO_ID = u.$KEY_ID
        """.trimIndent()
        return db.rawQuery(query, null)
    }
}