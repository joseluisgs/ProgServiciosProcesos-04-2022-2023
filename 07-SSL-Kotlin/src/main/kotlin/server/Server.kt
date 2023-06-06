package server

import common.Request
import common.Response
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private const val PUERTO = 6666

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

fun main() {
    var numConnections = 0

    println("🔵 Iniciando Servidor")

    // Donde está mi clave--> Mejor con fichero de propiedades
    logger.debug { "Cargando fichero del llavero Servidor" }
    val fichero = System.getProperty("user.dir") + File.separator + "cert" + File.separator + "server_keystore.p12"
    if (!Files.exists(Path.of(fichero))) {
        System.err.println("No se encuentra el fichero de certificado del servidor")
        exitProcess(0)
    }

    // Mejor cargarmos el fichero de propiedades
    logger.debug { "Cargando fichero de propiedades" }
    System.setProperty("javax.net.debug", "ssl, keymanager, handshake") // Depuramos
    System.setProperty("javax.net.ssl.keyStore", fichero) // Llavero
    System.setProperty("javax.net.ssl.keyStorePassword", "1234567") // Clave de acceso

    // Nos anunciamos como servidorControl de tipo SSL
    val serverFactory = SSLServerSocketFactory.getDefault() as SSLServerSocketFactory
    val serverSocket = serverFactory.createServerSocket(PUERTO) as SSLServerSocket

    // Opcionalmente podemos forzar el tipo de protocolo-> Poner el mismo que el cliente
    logger.debug { "Protocolos soportados: ${serverSocket.supportedProtocols.contentToString()}" }
    serverSocket.enabledCipherSuites = arrayOf("TLS_AES_128_GCM_SHA256")
    serverSocket.enabledProtocols = arrayOf("TLSv1.3")

    println("✅ Servidor->Listo. Esperando cliente...")

    while (true) {
        // Nos suspendemos hasta que llegue una conexión
        logger.debug { "Esperando conexión" }
        val socket = serverSocket.accept()
        numConnections++
        println("✳ Cliente $numConnections conectado desde: ${socket.remoteSocketAddress}")

        // Desviamos a una hilo o corrutina (launch), lo que quieras
        thread {
            procesarCliente(socket, numConnections)
        }
    }

}

fun procesarCliente(socket: Socket, numConnections: Int) {
    logger.debug { "Procesando cliente $numConnections" }

    // Creamos los flujos de entrada y salida y lo hacemos como texto, podrían ser binarios
    val entrada = DataInputStream(socket.inputStream)
    val salida = DataOutputStream(socket.outputStream)

    logger.debug { "Esperando mensaje Greeting" }
    var jsonRequest = entrada.readUTF()
    var request = json.decodeFromString<Request<String>>(jsonRequest)
    logger.debug { "Recibido: $request" }
    // Creamos la respuesta
    val greetingResponse = Response("Hola cliente $numConnections", Response.Type.OK)
    var jsonResponse = json.encodeToString(greetingResponse)
    logger.debug { "Enviando: $jsonResponse" }
    salida.writeUTF(jsonResponse)

    logger.debug { "Esperando mensaje Time" }
    jsonRequest = entrada.readUTF()
    logger.debug { "Recibido: $request" }
    // Creamos la respuesta
    val timeResponse = Response(LocalDateTime.now().toString(), Response.Type.OK)
    jsonResponse = json.encodeToString(timeResponse)
    logger.debug { "Enviando: $jsonResponse" }
    salida.writeUTF(jsonResponse)

    // Cerramos
    salida.close()
    entrada.close()
    socket.close()

}
