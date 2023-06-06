# Programación de Servicios y Procesos - 04 Técnicas de Programación Segura

Tema 04. Técnicas de Programación segura. Curso 2022/2023.

![imagen](https://raw.githubusercontent.com/joseluisgs/ProgServiciosProcesos-00-2022-2023/master/images/servicios.png)


## Contenidos
1. Introducción a la seguridad en la comunicaciones
2. Cifrado Unidireccional
3. Cifrado Simétrico
4. Cifrado Asimétrico
5. Firma Digital
6. Certificado Digital
7. Cifrado de Sesión
8. Comunicaciones Seguras
9. SSL/TSL
10. Secure Sockets
11. JWT

## Referencias
- [Cifrado de datos](https://latam.kaspersky.com/resource-center/definitions/encryption)
- [Hash en JVM](https://www.baeldung.com/sha-256-hashing-java)
- [BCrypt](https://en.wikipedia.org/wiki/Bcrypt)
- [AES](https://www.baeldung.com/java-aes-encryption-decryption)
- [RSA](https://www.baeldung.com/java-rsa)
- [DSA](https://www.baeldung.com/java-digital-signature)
- [Handshake SSL/TSL](https://www.cloudflare.com/es-es/learning/ssl/what-happens-in-a-tls-handshake/)
- [SSL JVM Baeldung](https://www.baeldung.com/java-ssl)
- [HTTPS Client Certificate Authentication](https://www.baeldung.com/java-https-client-certificate-authentication)
- [Socket SSL JVM](https://chuwiki.chuidiang.org/index.php?title=Socket_SSL_con_Java)
- [SSL Example](https://github.com/AlphaGarden/SSL-Client-Server)
- [SSL Ktor Socket](https://ktor.io/docs/servers-raw-sockets.html#secure)
- [SSL Ktor SSL Config](https://ktor.io/docs/client-ssl.html#configure-ssl)
- [Ktor snippets](https://github.com/ktorio/ktor-documentation/tree/2.2.1/codeSnippets/snippets)
- [JWT](https://es.wikipedia.org/wiki/JSON_Web_Token)
- [JWT Librerías](https://jwt.io/libraries)

https://ktor.io/docs/client-ssl.html#configure-ssl

## Proteger comunicaciones cifrando comunicaciones SSL/TSL
### Crear Pares de clave pública/privada Para Servidor y crear llavero del servidor
```bash
keytool -genkeypair -alias senderKeyPair -keyalg RSA -keysize 2048 \
  -dname "CN=1DAM" -validity 365 -storetype PKCS12 \
  -keystore sender_keystore.p12 -storepass changeit
```
¿Qué estamos haciendo?
```bash
keytool -genkey -keyalg RSA -alias serverKay -keystore serverKey.jks -storepass servpass
```

keytool está en el directorio bin de donde tengamos instalado java.
Con la opción -genkey le estamos diciendo que genere un certificado.
- keyalg RSA le indicamos que lo queremos encriptado con el algorítmo RSA
- alias serverKey. El certificado se meterá en un fichero de almacén de certificados que podrá contener varios certificados. Este alias es el nombre con el que luego podremos identificar este certificado concreto dentro del almacén. Podemos poner cualquier nombre que nos de una pista de qué es ese certificado.
- keystore serverKey.jks. Este es el fichero que hará de almacén de certificados. Si no existe se crea, si ya existe se añade el certificado con el alias que se haya indicado.
- storepass servpass. El almacén está protegido con contraseña, para acceder a él necesitamos la contraseña. Si el almacén no existe, se crea usando esta contraseña, por lo que deberemos recordarla. Si ya existe, debemos proporcionar la contraseña que tuviera ese almacén.


### Exportar certificado del servidor
```bash
keytool -exportcert -alias senderKeyPair -storetype PKCS12 \
  -keystore sender_keystore.p12 -file \
  sender_certificate.cer -rfc -storepass changeit
```
¿Qué estamos haciendo?
```bash
keytool -export -keystore serverkey.jks -alias serverKey -file ServerPublicKey.cer
```
- export es para exportar el certificado
- keystore serverkey.jks indica en qué almacén está el certificado que queremos exportar
- alias serverKey es el identificador del certificado dentro del almacén. Debe ser el mismo alias que pusimos cuando lo creamos.
- file ServerPublicKey.cer es el nombre del fichero donde queremos que se guarde el certificado que vamos a extraer.


### Importar certificado del servidor en el llavero del cliente
```bash
keytool -importcert -alias receiverKeyPair -storetype PKCS12 \
  -keystore receiver_keystore.p12 -file \
  sender_certificate.cer -rfc -storepass changeit
```
¿Qué estamos haciendo?
```bash
keytool -import -alias serverKey -file ServerPublicKey.cer -keystore clientTrustedCerts.jks -keypass clientpass -storepass clientpass
```
- import para indicar que queremos meter un certificado existente en un almacén.
- alias serverKey es el identificador que queremos dar al servidor dentro del almacén de certificados de confianza del cliente. Hemos puesto otra vez serverKey, pero al ser un almacén distinto de el del servidor, podríamos poner otro nombre.
- file ServerPublicKey.cer El certificado que queremos importar
- keystore clientTrustedCerts.jks el almacén de certificados de confianza del cliente, se creará si no existe.
- keypass clientpass Clave para proteger el certificado dentro del almacén. Debe ser la misma que la del almacén.
- storepass clientpass Clave para el almacén, si el almacén existe debe ser la que diéramos en el momento de crearlo. Si no existe, el almacén se creará protegido con esta clave. Esta clave debe coincidir además con la que pongamos en la opción -keypass porque luego el código java lo exigirá así.


### Con OpenSSL
```bash
# Step 1. Create a private key and public certificate for client & server by openssl tool.
openssl req -newkey rsa:2048 -nodes -keyout client-key.pem -x509 -days 365 -out client-certificate.pem  
openssl req -newkey rsa:2048 -nodes -keyout server-key.pem -x509 -days 365 -out server-certificate.pem

# Step 2. Combine the private key and public certificate into PCKS12(P12) format for client and server respectively.
openssl pkcs12 -inkey client-key.pem -in client-certificate.pem -export -out client-certificate.p12
openssl pkcs12 -inkey server-key.pem -in server-certificate.pem -export -out server-certificate.p12

# Step 3. Place client-certificate.p12 and server-certificate.p12 into keystore and trustStore location.
```
![image](https://raw.githubusercontent.com/AlphaGarden/SSL-Client-Server/master/img/client-server.jpg)
## Recursos
- Twitter: https://twitter.com/JoseLuisGS_
- GitHub: https://github.com/joseluisgs
- Web: https://joseluisgs.dev
- Discord del módulo: https://discord.gg/TKEE7dVxnQ
- Aula DAMnificad@s: https://discord.gg/XT8G5rRySU

## Autor

Codificado con :sparkling_heart: por [José Luis González Sánchez](https://twitter.com/JoseLuisGS_)

[![Twitter](https://img.shields.io/twitter/follow/JoseLuisGS_?style=social)](https://twitter.com/JoseLuisGS_)
[![GitHub](https://img.shields.io/github/followers/joseluisgs?style=social)](https://github.com/joseluisgs)

### Contacto
<p>
  Cualquier cosa que necesites házmelo saber por si puedo ayudarte 💬.
</p>
<p>
 <a href="https://joseluisgs.dev" target="_blank">
        <img src="https://joseluisgs.github.io/img/favicon.png" 
    height="30">
    </a>  &nbsp;&nbsp;
    <a href="https://github.com/joseluisgs" target="_blank">
        <img src="https://distreau.com/github.svg" 
    height="30">
    </a> &nbsp;&nbsp;
        <a href="https://twitter.com/JoseLuisGS_" target="_blank">
        <img src="https://i.imgur.com/U4Uiaef.png" 
    height="30">
    </a> &nbsp;&nbsp;
    <a href="https://www.linkedin.com/in/joseluisgonsan" target="_blank">
        <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/LinkedIn_logo_initials.png/768px-LinkedIn_logo_initials.png" 
    height="30">
    </a>  &nbsp;&nbsp;
    <a href="https://discordapp.com/users/joseluisgs#3560" target="_blank">
        <img src="https://logodownload.org/wp-content/uploads/2017/11/discord-logo-4-1.png" 
    height="30">
    </a> &nbsp;&nbsp;
    <a href="https://g.dev/joseluisgs" target="_blank">
        <img loading="lazy" src="https://googlediscovery.com/wp-content/uploads/google-developers.png" 
    height="30">
    </a>    
</p>

### ¿Un café?
<p><a href="https://www.buymeacoffee.com/joseluisgs"> <img align="left" src="https://cdn.buymeacoffee.com/buttons/v2/default-blue.png" height="50" alt="joseluisgs" /></a></p><br><br><br>

## Licencia de uso

Este repositorio y todo su contenido está licenciado bajo licencia **Creative Commons**, si desea saber más, vea la [LICENSE](https://joseluisgs.github.io/docs/license/). Por favor si compartes, usas o modificas este proyecto cita a su autor, y usa las mismas condiciones para su uso docente, formativo o educativo y no comercial.

<a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/"><img alt="Licencia de Creative Commons" style="border-width:0" src="https://i.creativecommons.org/l/by-nc-sa/4.0/88x31.png" /></a><br /><span xmlns:dct="http://purl.org/dc/terms/" property="dct:title">JoseLuisGS</span> by <a xmlns:cc="http://creativecommons.org/ns#" href="https://joseluisgs.github.io/" property="cc:attributionName" rel="cc:attributionURL">José Luis González Sánchez</a> is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-nc-sa/4.0/">Creative Commons Reconocimiento-NoComercial-CompartirIgual 4.0 Internacional License</a>.<br />Creado a partir de la obra en <a xmlns:dct="http://purl.org/dc/terms/" href="https://github.com/joseluisgs" rel="dct:source">https://github.com/joseluisgs</a>.