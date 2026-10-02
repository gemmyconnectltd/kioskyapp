package com.example.kioskyapp.logic

class LoginValidator {
    fun validate(username: String, password: String): Boolean {
        return username.isNotEmpty() && password.length >= 4
    }
}