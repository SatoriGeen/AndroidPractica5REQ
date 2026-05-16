package com.example.apirestclient

data class OpenLibraryResponse(
    val docs: List<BookData>
)

data class BookData(
    val key: String,
    val title: String,
    val author_name: List<String>?,
    val first_publish_year: Int?
)