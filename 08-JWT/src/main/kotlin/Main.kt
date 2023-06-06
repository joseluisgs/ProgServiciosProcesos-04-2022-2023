import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*


fun main(args: Array<String>) {
    println("Hola JWT")

    // Lo primero es crear el algoritmo de cifrado JWT
    val algorithm: Algorithm = Algorithm.HMAC256("MeGustanLosPepinosDeLeganesSiSonGrandesYHermosos")

    // Ahora creamos el token, no todos los campos son obligatorios. Te comento algunos con *
    val jwtToken: String = JWT.create()
        .withIssuer("2DAM") // Quien lo emite *
        .withSubject("Programacion de Servicios y Procesos") // Para que lo emite *
        .withClaim("userId", "1234") // Datos que queremos guardar * (al menos algunos)
        .withClaim("name", "Jose Luis") // Datos que queremos guardar
        .withClaim("roles", listOf("admin", "user")) // Datos que queremos guardar
        .withClaim("email", "prueba@luisvives.es") // Datos que queremos guardar
        .withIssuedAt(Date()) // Fecha de emision *
        .withExpiresAt(Date(System.currentTimeMillis() + 5000L)) // Fecha de expiracion *
        .withJWTId(UUID.randomUUID().toString()) // Identificador unico del token
        .withNotBefore(Date(System.currentTimeMillis() + 1000L)) // Fecha de cuando se puede usar
        .sign(algorithm) // Firmamos el token

    println(jwtToken)
    // Si no esperamos fallaría porque le hemos puesto un tiempo de inicio de 1 segundo
    // O si aumentamos a mas de 5 caduca el token
    Thread.sleep(2000L)

    try {

        // Ahora lo decodificamos
        val verifier = JWT.require(algorithm)
            //.withIssuer("Prueba") // Quien lo emite  y solo validamos para este tipo de emisor
            .build()
        val decodedJWT = verifier.verify(jwtToken)

        // Vamos a sacar los claims o datos
        println(decodedJWT.getClaim("userId").asString())
        println(decodedJWT.getClaim("name").asString())
        println(decodedJWT.getClaim("roles").asList(String::class.java))
        println(decodedJWT.getClaim("email").asString())

        // Otros datos que tenemos
        println(decodedJWT.issuer)
        println(decodedJWT.subject)
        println(decodedJWT.expiresAt)
        println(decodedJWT.notBefore)
        println(decodedJWT.issuedAt)
        println(decodedJWT.id)
        println(decodedJWT.header)
        println(decodedJWT.payload)
        println(decodedJWT.signature)
        println(decodedJWT.token)

    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
    }

}