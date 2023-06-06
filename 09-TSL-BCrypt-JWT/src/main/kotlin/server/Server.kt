package server

import common.Request
import common.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Login
import models.User
import mu.KotlinLogging
import org.mindrot.jbcrypt.BCrypt
import repositories.UserRepository
import service.TokenService
import utils.PropertiesReader
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileNotFoundException
import java.net.Socket
import java.nio.file.Path
import java.time.LocalDateTime
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLServerSocketFactory
import kotlin.io.path.exists
import kotlin.system.exitProcess

private const val PUERTO = 6666

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }


fun main() = runBlocking {
    var numConnections = 0

    println("🔵 Iniciando Servidor")

    // Donde está mi clave--> Mejor con fichero de propiedades
    val myConfig = readConfigFile()

    logger.debug { "Configurando TSL" }
    // System.setProperty("javax.net.debug", "ssl, keymanager, handshake") // Depuramos
    System.setProperty("javax.net.ssl.keyStore", myConfig["keyFile"]!!) // Llavero
    System.setProperty("javax.net.ssl.keyStorePassword", myConfig["keyPassword"]!!) // Clave de acceso

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
        launch(Dispatchers.IO) {
            procesarCliente(socket, numConnections, myConfig)
        }
        // thread { procesarCliente(socket, numConnections, myConfig) }
    }

}


private fun readConfigFile(): Map<String, String> {
    try {
        logger.debug { "Leyendo el fichero de propiedades" }
        val properties = PropertiesReader("server.properties")

        val keyFile = properties.getProperty("keyFile")
        val keyPassword = properties.getProperty("keyPassword")
        val tokenSecret = properties.getProperty("tokenSecret")
        val tokenExpiration = properties.getProperty("tokenExpiration")

        // Comprobamos que no estén vacías
        check(keyFile.isNotEmpty() && keyPassword.isNotEmpty()) { "Hay errores al procesar el fichero de propiedades o una de ellas está vacía" }

        // Comrpbamos el fichero de la clave
        check(Path.of(keyFile).exists()) { "No se encuentra el fichero de la clave" }

        return mapOf(
            "keyFile" to keyFile,
            "keyPassword" to keyPassword,
            "tokenSecret" to tokenSecret,
            "tokenExpiration" to tokenExpiration
        )
    } catch (e: FileNotFoundException) {
        logger.error { "Error en clave: ${e.localizedMessage}" }
        exitProcess(1)
    }

}

private fun procesarCliente(socket: Socket, numConnections: Int, config: Map<String, String>) {
    logger.debug { "Procesando cliente $numConnections" }

    // Creamos los flujos de entrada y salida y lo hacemos como texto, podrían ser binarios
    val entrada = DataInputStream(socket.inputStream)
    val salida = DataOutputStream(socket.outputStream)

    // Login
    val user = receiveLoginRequest(entrada)
    if (user == null) {
        sendErrorResponse(salida, "Error: Usuario o contraseña incorrectos")
    } else {
        sendTokenResponse(salida, user, config["tokenSecret"]!!, config["tokenExpiration"]!!.toLong())
        // Si no es admin no puede hacer nada
        if (user.role != "admin")
            sendErrorResponse(salida, "Error: No tienes permisos para acceder a esta operación")
        else {
            val timeOk = receiveTimeRequest(entrada, user, config["tokenSecret"]!!)
            if (!timeOk) {
                sendErrorResponse(salida, "Error: Token incorrecto. No autorizado")
            } else {
                sendTimeResponse(salida)
            }
        }
    }

    // Cerramos
    salida.close()
    entrada.close()
    socket.close()
}

fun sendTimeResponse(salida: DataOutputStream) {
    val timeResponse = Response(LocalDateTime.now().toString(), Response.Type.TIME)
    val jsonResponse = json.encodeToString(timeResponse)
    logger.debug { "Enviando: $jsonResponse" }
    salida.writeUTF(jsonResponse)
}

fun receiveTimeRequest(entrada: DataInputStream, user: User, tokenSecret: String): Boolean {
    logger.debug { "Esperando mensaje time" }
    val jsonRequest = entrada.readUTF()
    val requestTime = json.decodeFromString<Request<String>>(jsonRequest)
    logger.debug { "Recibido: $requestTime" }

    // Procesamos el mensaje
    val token = requestTime.token!!
    // Verificamos el token
    return TokenService.verifyToken(token, tokenSecret, user)
}

fun sendTokenResponse(salida: DataOutputStream, user: User, tokenSecret: String, tokenExpiration: Long = 3600L) {
    val token = TokenService.createToken(user, tokenSecret, tokenExpiration)
    // Creamos la respuesta
    val tokenResponse = Response(token, Response.Type.TOKEN)
    val jsonResponse = json.encodeToString(tokenResponse)
    logger.debug { "Enviando: $jsonResponse" }
    salida.writeUTF(jsonResponse)
}

fun sendErrorResponse(salida: DataOutputStream, message: String) {
    // Creamos la respuesta
    val errorResponse = Response(message, Response.Type.ERROR)
    val jsonResponse = json.encodeToString(errorResponse)
    logger.debug { "Enviando: $jsonResponse" }
    salida.writeUTF(jsonResponse)
}

private fun receiveLoginRequest(entrada: DataInputStream): User? {
    logger.debug { "Esperando mensaje Login" }
    val jsonRequest = entrada.readUTF()
    val requestLogin = json.decodeFromString<Request<Login>>(jsonRequest)
    logger.debug { "Recibido: $requestLogin" }

    // Procesamos el mensaje
    val login = requestLogin.content as Login
    // Existe el usuario?
    val user = UserRepository.findUserByUsername(login.username)
    // Si existe comprobamos la contraseña, si le ponemos admin lo filtramos por rol aquí, pero voy a dejarlo
    // Para el token
    return if (user != null && BCrypt.checkpw(login.password, user.password) /*&& user.role == "admin"*/) {
        logger.debug { "Usuario válido" }
        user
    } else {
        logger.debug { "Usuario no válido" }
        null
    }
}
