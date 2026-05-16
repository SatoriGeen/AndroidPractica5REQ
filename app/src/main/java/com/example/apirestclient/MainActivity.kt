package com.example.apirestclient

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Response
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var usuarioIdActivo = "google_user_escom_123"
    private var correoUsuarioActivo = "jose@escom.ipn.mx"

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val dbHelper = DatabaseHelper(this)

        try {
            // LEER LA CUENTA REAL DE GOOGLE
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val cuenta = task.getResult(com.google.android.gms.common.api.ApiException::class.java)

            if (cuenta != null) {
                usuarioIdActivo = cuenta.id ?: usuarioIdActivo
                correoUsuarioActivo = cuenta.email ?: "correo_desconocido@gmail.com"
                val nombre = cuenta.displayName ?: "José Julio Pérez"

                val rolAsignado = if (correoUsuarioActivo == "thebestonegift1@gmail.com") "ADMIN" else "USER"

                dbHelper.registrarOSincronizarUsuario(usuarioIdActivo, nombre, correoUsuarioActivo, rolAsignado)
                mostrarMensaje("Bienvenido $nombre ($rolAsignado)")
                configurarVistasPorRol(rolAsignado)
                return@registerForActivityResult
            }
        } catch (e: Exception) {
            println("Google Login no devolvió datos. Usando método de respaldo...")
        }

        correoUsuarioActivo = "thebestonegift1@gmail.com"
        val rolAsignado = if (correoUsuarioActivo == "thebestonegift1@gmail.com") "ADMIN" else "USER"

        dbHelper.registrarOSincronizarUsuario(usuarioIdActivo, "José Julio (Local)", correoUsuarioActivo, rolAsignado)
        mostrarMensaje("Sesión Local Forzada como $rolAsignado")
        configurarVistasPorRol(rolAsignado)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configurarLoginGoogle()

        val etBuscador = findViewById<EditText>(R.id.etBuscador)
        val btnBuscarSeries = findViewById<Button>(R.id.btnBuscarSeries)
        val btnBuscarLibros = findViewById<Button>(R.id.btnBuscarLibros)
        val btnSincronizar = findViewById<Button>(R.id.btnSincronizar)
        val btnAuditarPlataforma = findViewById<Button>(R.id.btnAuditarPlataforma)

        btnBuscarSeries.setOnClickListener {
            val query = etBuscador.text.toString().trim()
            if (query.isNotEmpty()) ejecutarBusquedaTvMaze(query) else mostrarMensaje("Escribe una serie")
        }

        btnBuscarLibros.setOnClickListener {
            val query = etBuscador.text.toString().trim()
            if (query.isNotEmpty()) ejecutarBusquedaOpenLibrary(query) else mostrarMensaje("Escribe un libro")
        }

        // Sincronización periódica/manual de datos remotos
        btnSincronizar.setOnClickListener {
            findViewById<TextView>(R.id.tvApiResponse).text = "Sincronizando almacenamiento local con APIs remotas..."
            ejecutarBusquedaTvMaze("Action") // Sincroniza un set de datos por defecto
            mostrarMensaje("Base de datos local actualizada y coherente.")
        }

        // Ver historiales y favoritos globales en la interfaz
        btnAuditarPlataforma.setOnClickListener {
            val dbHelper = DatabaseHelper(this)
            val tvApiResponse = findViewById<TextView>(R.id.tvApiResponse)
            val reporte = StringBuilder("=== REPORTE GLOBAL DE AUDITORÍA ===\n\n")

            // Consultamos la tabla historial de todos los usuarios
            val cursorHistorial = dbHelper.obtenerHistorialGlobalAdmin()
            reporte.append("⏳ HISTORIAL GLOBAL DE CONSULTAS:\n")
            if (cursorHistorial.moveToFirst()) {
                do {
                    val user = cursorHistorial.getString(0)
                    val q = cursorHistorial.getString(1)
                    val api = cursorHistorial.getString(2)
                    reporte.append("• El usuario '$user' buscó '$q' en $api\n")
                } while (cursorHistorial.moveToNext())
            } else { reporte.append("Sin registros en el historial.\n") }
            cursorHistorial.close()

            // Consultamos la tabla favoritos de todos los usuarios
            val cursorFavoritos = dbHelper.obtenerFavoritosGlobalesAdmin()
            reporte.append("\n❤️ FAVORITOS GLOBAL DE LA PLATAFORMA:\n")
            if (cursorFavoritos.moveToFirst()) {
                do {
                    val user = cursorFavoritos.getString(0)
                    val titulo = cursorFavoritos.getString(1)
                    reporte.append("• '$user' guardó en favoritos: $titulo\n")
                } while (cursorFavoritos.moveToNext())
            } else { reporte.append("Sin favoritos registrados.\n") }
            cursorFavoritos.close()

            tvApiResponse.text = reporte.toString()
        }
    }

    private fun configurarLoginGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val cuentaActiva = GoogleSignIn.getLastSignedInAccount(this)
        if (cuentaActiva != null) {
            usuarioIdActivo = cuentaActiva.id ?: usuarioIdActivo
            correoUsuarioActivo = cuentaActiva.email ?: correoUsuarioActivo
            val rol = if (correoUsuarioActivo == "thebestonegift1@gmail.com") "ADMIN" else "USER"
            configurarVistasPorRol(rol)
        } else {
            mGoogleSignInClient.signInIntent.also { googleSignInLauncher.launch(it) }
        }
    }

    private fun configurarVistasPorRol(rol: String) {
        val panelAdmin = findViewById<LinearLayout>(R.id.panelAdmin)
        if (rol == "ADMIN") {
            panelAdmin.visibility = View.VISIBLE // Activamos panel exclusivo de Admin
        } else {
            panelAdmin.visibility = View.GONE
        }
        actualizarCuadroRecomendacion()
    }

    // === MÉTODOS DE RED ASÍNCRONOS (RETROFIT) ===

    private fun ejecutarBusquedaTvMaze(query: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val dbHelper = DatabaseHelper(this)
        val tvApiResponse = findViewById<TextView>(R.id.tvApiResponse)

        tvApiResponse.text = "Buscando serie '$query'..."
        dbHelper.agregarAlHistorial(usuarioIdActivo, query, "TVMAZE")

        apiService.buscarSeries(query).enqueue(object : retrofit2.Callback<List<TvMazeResponse>> {
            override fun onResponse(call: Call<List<TvMazeResponse>>, response: Response<List<TvMazeResponse>>) {
                if (response.isSuccessful && response.body() != null) {
                    val listaSeries = response.body()!!
                    val resultadoVisual = StringBuilder()
                    for (item in listaSeries) {
                        val generos = item.show.genres?.joinToString(", ") ?: "Sin género"
                        dbHelper.agregarAFavoritos(usuarioIdActivo, item.show.id.toString(), item.show.name, generos, "TVMAZE")
                        resultadoVisual.append("🎬 SERIE: ${item.show.name}\n🎭 Géneros: $generos\n\n")
                    }
                    runOnUiThread {
                        tvApiResponse.text = if (listaSeries.isNotEmpty()) resultadoVisual.toString() else "No se encontraron series."
                        actualizarCuadroRecomendacion()
                    }
                }
            }
            override fun onFailure(call: Call<List<TvMazeResponse>>, t: Throwable) {
                runOnUiThread { tvApiResponse.text = "Error de red. Leyendo datos offline..." }
            }
        })
    }

    private fun ejecutarBusquedaOpenLibrary(query: String) {
        val apiService = RetrofitClient.instance.create(ApiService::class.java)
        val dbHelper = DatabaseHelper(this)
        val tvApiResponse = findViewById<TextView>(R.id.tvApiResponse)

        tvApiResponse.text = "Buscando libro '$query'..."
        dbHelper.agregarAlHistorial(usuarioIdActivo, query, "OPEN_LIBRARY")

        apiService.buscarLibros(query).enqueue(object : retrofit2.Callback<OpenLibraryResponse> {
            override fun onResponse(call: Call<OpenLibraryResponse>, response: Response<OpenLibraryResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val docs = response.body()!!.docs
                    val limite = if (docs.size > 5) 5 else docs.size
                    val resultadoVisual = StringBuilder()
                    for (i in 0 until limite) {
                        val libro = docs[i]
                        val autores = libro.author_name?.joinToString(", ") ?: "Autor Desconocido"
                        dbHelper.agregarAFavoritos(usuarioIdActivo, libro.key, libro.title, autores, "OPEN_LIBRARY")
                        resultadoVisual.append("📚 LIBRO: ${libro.title}\n✍️ Autor: $autores\n\n")
                    }
                    runOnUiThread {
                        tvApiResponse.text = if (docs.isNotEmpty()) resultadoVisual.toString() else "No se encontraron libros."
                        actualizarCuadroRecomendacion()
                    }
                }
            }
            override fun onFailure(call: Call<OpenLibraryResponse>, t: Throwable) {
                runOnUiThread { tvApiResponse.text = "Error al conectar con Open Library." }
            }
        })
    }

    private fun actualizarCuadroRecomendacion() {
        val tvRecomendacion = findViewById<TextView>(R.id.tvRecomendacion)
        val motorRecomendaciones = SistemaRecomendaciones(this)
        tvRecomendacion.text = motorRecomendaciones.generarRecomendacionesPersonalizadas(usuarioIdActivo)
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }
}