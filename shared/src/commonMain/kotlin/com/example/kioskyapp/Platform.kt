package com.example.kioskyapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform