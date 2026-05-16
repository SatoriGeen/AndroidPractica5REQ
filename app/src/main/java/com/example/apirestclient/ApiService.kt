package com.example.apirestclient

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @retrofit2.http.POST("api/auth/login")
    fun iniciarSesion(
        @retrofit2.http.Body camposLogin: Map<String, String>
    ): Call<String>

    // Buscador de Series (TVMaze)
    @GET("https://api.tvmaze.com/search/shows")
    fun buscarSeries(
        @Query("q") nombreSerie: String
    ): Call<List<TvMazeResponse>>

    // Llama de forma dinámica
    @GET("https://openlibrary.org/search.json")
    fun buscarLibros(
        @Query("q") queryLibro: String
    ): Call<OpenLibraryResponse>
}