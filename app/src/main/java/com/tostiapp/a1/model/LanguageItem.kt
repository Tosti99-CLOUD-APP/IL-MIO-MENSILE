package com.tostiapp.a1.model

data class LanguageItem(
    val name: String,
    val code: String,
    var isDownloaded: Boolean = false,
    var isDownloading: Boolean = false
)
