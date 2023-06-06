package models

import kotlinx.serialization.Serializable

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val password: String,
    val role: String
)

@Serializable
data class Login(
    val username: String,
    val password: String
)