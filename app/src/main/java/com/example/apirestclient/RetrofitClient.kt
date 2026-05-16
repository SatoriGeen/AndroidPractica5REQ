package com.example.apirestclient

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // IMPORTANTE: 10.0.2.2 es la IP especial para que el emulador de Android se conecte al localhost de tu PC
    // Cambia el 8080 si tu contenedor Docker/Spring Boot usa otro puerto
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val instance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}