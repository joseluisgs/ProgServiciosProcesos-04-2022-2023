package client

import common.Request
import common.Response
import es.joseluisgs.encordadosmongodbreactivespringdatakotlin.extensions.toLocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Login
import mu.KotlinLogging
import utils.PropertiesReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.math.BigInteger
import java.nio.file.Path
import java.security.cert.X509Certificate
import java.time.LocalDateTime
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.io.path.exists
import kotlin.system.exitProcess


private const val PUERTO = 6666
private const val SERVER = "localhost" // InetAddress.getLocalHost().getHostAddress()

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

fun main() {
    println("🔵 Iniciando Cliente")

    // Donde está mi clave--> Mejor con fichero de propiedades
    val myConfig = readConfigFile()

    logger.debug { "Cargando fichero de propiedades" }
    // System.setProperty("javax.net.debug", "ssl, keymanager, handshake") // Debug
    System.setProperty("javax.net.ssl.trustStore", myConfig["keyFile"]!!) // llavero cliente
    System.setProperty("javax.net.ssl.trustStorePassword", myConfig["keyPassword"]!!) // clave

    try {

        val clientFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
        val socket = clientFactory.createSocket(SERVER, PUERTO) as SSLSocket

        // Opcionalmente podemos forzar el tipo de protocolo-> Poner el mismo que el cliente
        logger.debug { "Protocolos soportados: ${socket.supportedProtocols.contentToString()}" }
        socket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
        socket.enabledProtocols = arrayOf("TLSv1.3")

        println("✅ Cliente conectado a $SERVER:$PUERTO")

        infoSession(socket)

        // Creamos los flujos de entrada y salida y lo hacemos como texto, podrían ser binarios
        val entrada = DataInputStream(socket.inputStream)
        val salida = DataOutputStream(socket.outputStream)

        // Hacemos el login
        sendLoginRequest(salida)

        // Recibimos la respuesta
        val token = readTokenResponse(entrada)

        // Paramos, para forzar el error del token
        // Thread.sleep(15000)

        // Hacemos la petición de la hora
        sendTimeRequest(salida, token)

        // Recibimos la respuesta
        val time = readTimeResponse(entrada)
        println("🕒 Fecha del servidor: $time")
        println("\uD83C\uDDEA\uD83C\uDDF8 ${LocalDateTime.parse(time).toLocalDateTime()}")

        // Cerramos los flujos y el socket
        // si notas que hay una excepción, no lo es, es el mensaje de debug del ssl, comentalo!!!
        entrada.close()
        salida.close()
        socket.close()

        println("🔵 Cliente finalizado")
        exitProcess(0)

    } catch (e: Exception) {
        System.err.println("Error: ${e.message}")
    }


}

fun readTimeResponse(entrada: DataInputStream): String {
    // Recibimos la hora
    val jsonResponse = entrada.readUTF()
    logger.debug { "Recibido: $jsonResponse" }
    val response = json.decodeFromString<Response<String>>(jsonResponse)
    // Vemos el tipo
    when (response.type) {
        Response.Type.TIME -> {
            logger.debug { "Recibida hora del servidor" }
            return response.content!!
        }

        Response.Type.ERROR -> {
            println("❌ Error: ${response.content}")
            exitProcess(1)
        }

        else -> {
            println("❌ Error: Tipo de respuesta no esperado")
            exitProcess(1)
        }
    }

}

fun sendTimeRequest(salida: DataOutputStream, token: String) {
    logger.debug { "Enviando petición de hora" }
    val requestTime = Request<String>(token = token, type = Request.Type.TIME)
    // pasamos a json
    val jsonRequest = json.encodeToString(requestTime)
    logger.debug { "Enviando: $jsonRequest" }
    salida.writeUTF(jsonRequest) // Enviamos el JSON como texto
}

fun readTokenResponse(entrada: DataInputStream): String {
    logger.debug { "Esperando token y respuesta del servidor" }
    // Recibimos la respuesta
    var jsonResponse = entrada.readUTF()
    logger.debug { "Recibido: $jsonResponse" }
    val response = json.decodeFromString<Response<String>>(jsonResponse)
    // Vemos el tipo
    when (response.type) {
        Response.Type.TOKEN -> {
            println("✅ Token recibido: ${response.content}")
            return response.content!!
        }

        Response.Type.ERROR -> {
            println("❌ Error: ${response.content}")
            exitProcess(1)
        }

        else -> {
            println("❌ Error: Tipo de respuesta no esperado")
            exitProcess(1)
        }
    }
}

private fun sendLoginRequest(salida: DataOutputStream) {
    //val requestLogin = Request(content = Login("johndoe", "Hola"), type = Request.Type.LOGIN)
    val requestLogin = Request(content = Login("pepe", "pepe1234"), type = Request.Type.LOGIN)
    // pasamos a json
    val jsonRequest = json.encodeToString(requestLogin)
    logger.debug { "Enviando: $jsonRequest" }
    salida.writeUTF(jsonRequest) // Enviamos el JSON como texto
}

private fun readConfigFile(): Map<String, String> {
    try {
        logger.debug { "Leyendo el fichero de configuracion" }
        val properties = PropertiesReader("client.properties")

        val keyFile = properties.getProperty("keyFile")
        val keyPassword = properties.getProperty("keyPassword")

        // Comprobamos que no estén vacías
        check(keyFile.isNotEmpty() && keyPassword.isNotEmpty()) { "Hay errores al procesar el fichero de propiedades o una de ellas está vacía" }

        // Comrpbamos el fichero de la clave
        check(Path.of(keyFile).exists()) { "No se encuentra el fichero de la clave" }

        return mapOf("keyFile" to keyFile, "keyPassword" to keyPassword)
    } catch (e: FileNotFoundException) {
        logger.error { "Error en clave: ${e.localizedMessage}" }
        exitProcess(1)
    }

}

private fun infoSession(socket: SSLSocket) {
    logger.debug { "Información de la sesión" }
    println("🔐 Información de la sesión")
    try {
        val sesion: SSLSession = socket.session
        println("Servidor: " + sesion.peerHost)
        println("Cifrado: " + sesion.cipherSuite)
        println("Protocolo: " + sesion.protocol)
        println("Identificador:" + BigInteger(sesion.id))
        println("Creación de la sesión: " + sesion.creationTime)
        val certificado: X509Certificate = sesion.peerCertificates[0] as X509Certificate
        println("Propietario : " + certificado.subjectX500Principal)
        println("Algoritmo: " + certificado.sigAlgName)
        println("Tipo: " + certificado.type)
        println("Número Serie: " + certificado.serialNumber)
        // expiración del certificado
        println("Válido hasta: " + certificado.notAfter)
    } catch (ex: SSLPeerUnverifiedException) {
        logger.error { "Error en la sesión: ${ex.localizedMessage}" }
    }
}