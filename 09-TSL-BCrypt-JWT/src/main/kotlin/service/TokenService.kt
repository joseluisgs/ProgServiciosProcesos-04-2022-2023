package service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import models.User
import mu.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger {}

// 1 min

object TokenService {
    fun createToken(user: User, tokeSecret: String, tokenExpiration: Long): String {
        logger.debug { "Creando token" }
        val algorithm: Algorithm = Algorithm.HMAC256(tokeSecret)
        // Ahora creamos el token, no todos los campos son obligatorios. Te comento algunos con *
        return JWT.create()
            //.withIssuer("2DAM") // Quien lo emite *
            //.withSubject("Programacion de Servicios y Procesos") // Para que lo emite *
            .withClaim("userid", user.id) // Datos que queremos guardar * (al menos algunos)
            .withClaim("username", user.username) // Datos que queremos guardar
            .withClaim("rol", user.role) // Datos que queremos guardar
            .withIssuedAt(Date()) // Fecha de emision *
            .withExpiresAt(Date(System.currentTimeMillis() + tokenExpiration)) // Fecha de expiracion *
            //.withJWTId(UUID.randomUUID().toString()) // Identificador unico del token
            //.withNotBefore(Date(System.currentTimeMillis() + 1000L)) // Fecha de cuando se puede usar
            .sign(algorithm) // Firmamos el token
    }

    fun verifyToken(token: String, tokeSecret: String, user: User): Boolean {
        logger.debug { "Verificando token" }
        val algorithm: Algorithm = Algorithm.HMAC256(tokeSecret)
        val verifier = JWT.require(algorithm)
            .build() // Creamos el verificador
        return try {
            val decodedJWT = verifier.verify(token)
            logger.debug { "Token verificado" }
            // Comprobamos que el token es del usuario
            decodedJWT.getClaim("userid").asInt() == user.id &&
                    decodedJWT.getClaim("username").asString() == user.username &&
                    decodedJWT.getClaim("rol").asString() == user.role

        } catch (e: Exception) {
            logger.error { "Error al verificar el token: ${e.message}" }
            false
        }
    }
}