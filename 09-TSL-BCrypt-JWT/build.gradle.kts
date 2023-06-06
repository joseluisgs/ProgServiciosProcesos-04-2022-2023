import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    application
    // para serializar Json y otros
    kotlin("plugin.serialization") version "1.7.20"
}

group = "es.joseluisgs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Para hacer el logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.5")

    // Serializa Json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // Corrutinas no es necesario
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // BCrypt
    // implementation("de.nycode:bcrypt:2.2.0")
    implementation("org.mindrot:jbcrypt:0.4")

    // JWT token
    implementation("com.auth0:java-jwt:4.2.1")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}