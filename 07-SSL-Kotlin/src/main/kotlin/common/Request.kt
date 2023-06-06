package common

import kotlinx.serialization.Serializable

@Serializable
data class Request<T>(
    val content: T?, // Contenido de la petición
    val type: Type, // Tipo de petición
) {
    enum class Type {
        GREETINGS, TIME
    }
}
