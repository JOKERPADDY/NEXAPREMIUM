package com.nexapremium.data.api

/** Normalizes card detail paths to the title slug expected by /v1/titles/{path}. */
fun normalizeTitlePath(value: String): String {
    var path = value.trim().substringBefore('?')
    path = path.removePrefix("https://themoviebox.xyz/")
        .removePrefix("http://themoviebox.xyz/")
        .trimStart('/')
        .removePrefix("v1/titles/")
    path = path.removePrefix("movies/").removePrefix("series/")
    return path.trim('/')
}
