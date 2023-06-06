package utils

import java.io.FileNotFoundException
import java.util.*

class PropertiesReader(private val fileName: String) {
    private val properties = Properties()

    init {
        val file = this::class.java.classLoader.getResourceAsStream(fileName)
        if (file != null) {
            properties.load(file)
        } else {
            throw FileNotFoundException("No se encuentra el fichero $fileName")
        }
    }

    fun getProperty(key: String): String {
        val value = properties.getProperty(key)
        if (value != null) {
            return value
        } else {
            throw FileNotFoundException("No se encuentra la propiedad $key en el fichero $fileName")
        }
    }
}
