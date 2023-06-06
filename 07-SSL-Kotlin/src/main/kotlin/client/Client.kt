package client

import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import kotlin.system.exitProcess

private const val PUERTO = 6666
private const val SERVER = "localhost" // InetAddress.getLocalHost().getHostAddress()

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

fun main() {
    println("🔵 Iniciando Cliente")

    // Donde está mi clave--> Mejor con fichero de propiedades
    logger.debug { "Cargando fichero del llavero Cliente" }
    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "client_keystore.p12"
    if (!Files.exists(Path.of(fichero))) {
        System.err.println("No se encuentra el fichero de certificado del servidor")
        exitProcess(0)
    }

    // Mejor cargarmos el fichero de propiedades
    logger.debug { "Cargando fichero de propiedades" }
    System.setProperty("javax.net.debug", "ssl, keymanager, handshake") // Debug
    System.setProperty("javax.net.ssl.trustStore", fichero) // llavero cliente
    System.setProperty("javax.net.ssl.trustStorePassword", "1234567") // clave

    val clientFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
    val socket = clientFactory.createSocket(SERVER, PUERTO) as SSLSocket

    // Opcionalmente podemos forzar el tipo de protocolo-> Poner el mismo que el cliente
    logger.debug { "Protocolos soportados: ${socket.supportedProtocols.contentToString()}" }
    socket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    socket.enabledProtocols = arrayOf("TLSv1.3")

    println("✅ Cliente conectado a $SERVER:$PUERTO")

    // Creamos los flujos de entrada y salida y lo hacemos como texto, podrían ser binarios
    val entrada = DataInputStream(socket.inputStream)
    val salida = DataOutputStream(socket.outputStream)

    // Enviamos un echo
    logger.debug { "Pidiendo un echo" }
    val requestGreeting = Request<String>(null, Request.Type.GREETINGS)
    // pasamos a json
    var jsonRequest = json.encodeToString(requestGreeting)
    logger.debug { "Enviando: $jsonRequest" }
    // salida.write(jsonRequest.toByteArray()) // Esto sería en binario, no es necesqrio enn JSON
    salida.writeUTF(jsonRequest) // Enviamos el JSON como texto

    // Recibimos el echo
    var jsonResponse = entrada.readUTF()
    logger.debug { "Recibido: $jsonResponse" }
    val response = json.decodeFromString<Response<String>>(jsonResponse)
    println("\uD83D\uDCE9 Recibido: ${response.content}")

    // Enviamos la hora
    logger.debug { "Pidiendo la hora" }
    val requestTime = Request<String>(null, Request.Type.TIME)
    // pasamos a json
    jsonRequest = json.encodeToString(requestTime)
    logger.debug { "Enviando: $jsonRequest" }
    salida.writeUTF(jsonRequest) // Enviamos el JSON como texto

    // Recibimos la hora
    jsonResponse = entrada.readUTF()
    logger.debug { "Recibido: $jsonResponse" }
    val responseTime = json.decodeFromString<Response<String>>(jsonResponse)
    println("\uD83D\uDCE9 Recibido: ${responseTime.content}")

    // Cerramos los flujos y el socket
    // si notas que hay una excepción, no lo es, es el mensaje de debug!!!
    entrada.close()
    salida.close()
    socket.close()

    println("🔵 Cliente finalizado")
    exitProcess(0)
}