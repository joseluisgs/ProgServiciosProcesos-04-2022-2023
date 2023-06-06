# EJERCICIO

En servidor tenemos un usuario llamado "pepe" con contraseña "pepe1234" y rol admin almacenada con Bcrypt.
Por supuesto la conexión es segura con TSL/AES, por lo que necesitamos claves, certificados y llaveros para el cliente y
el servidor

El usuario se conecta al servidor con un Request de tipo Login y le pasa el usuario y el password, es decir, "pepe" y "
pepe1234", como ya está cifrado por TSL no hay problemas por la contraseña

El servidor recibe el paquete, toma la contraseña y usando Bcrypt compara la contraseña "pepe1234" con lo que tiene el
objeto user.

Si hay error, manda Response del tipo Error al usuario con el texto, nombre de usuario o contraseña no válida

Si es correcto, genera un JWT token con los datos del usuario: username y rol, es decir pepe y admin y de tiempo de
expiración 60 segundos y se lo manda en un Response al Cliente

El cliente recibe el token y lo almacena

Le hace una petición Request con la hora y le añade el token
si tiene permiso de admin y el token es válido le mandará la hora
