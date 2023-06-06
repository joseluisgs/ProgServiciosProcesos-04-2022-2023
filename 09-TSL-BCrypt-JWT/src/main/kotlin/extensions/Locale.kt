package es.joseluisgs.encordadosmongodbreactivespringdatakotlin.extensions

import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

fun LocalDate.toLocalDate(): String {
    return this.format(
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale("es", "ES"))
    )
}

fun LocalDateTime.toLocalDateTime(): String {
    return this.format(
        DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale("es", "ES"))
    )
}

fun Double.toLocalMoney(): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(this)
}

fun Double.toLocalNumber(): String {
    return NumberFormat.getNumberInstance(Locale("es", "ES")).format(this)
}