package repositories

import models.User
import org.mindrot.jbcrypt.BCrypt

object UserRepository {
    val users = listOf(
        User(
            id = 1,
            name = "John Doe",
            username = "johndoe",
            password = BCrypt.hashpw("Hola", BCrypt.gensalt(12)),
            role = "user"
        ),
        User(
            id = 2,
            name = "Pepe Perez",
            username = "pepe",
            password = BCrypt.hashpw("pepe1234", BCrypt.gensalt(12)),
            role = "admin"
        ),
    )

    fun findUserByUsername(username: String): User? {
        return users.find { it.username == username }
    }

    fun findUserById(id: Int): User? {
        return users.find { it.id == id }
    }

}