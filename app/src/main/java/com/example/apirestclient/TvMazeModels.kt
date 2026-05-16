package com.example.apirestclient

data class TvMazeResponse(
    val show: ShowData
)

data class ShowData(
    val id: Int,
    val name: String,
    val genres: List<String>?,
    val summary: String?
)