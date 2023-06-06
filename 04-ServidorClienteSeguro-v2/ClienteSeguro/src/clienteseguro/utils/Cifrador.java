/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clienteseguro.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author link
 */
public class Cifrador {

    private static Cifrador cif = null;

    /**
     * Devuelve una instancia única del cifrador
     *
     * @return
     */
    public static Cifrador nuevoCifrador() {
        if (cif == null) {
            cif = new Cifrador();
        }
        return cif;
    }

    private Cifrador() {

    }

    /**
     * Codifica a cadena en SHA 256
     *
     * @param cadena
     * @return
     */
    public String SHA256(String cadena) {
        MessageDigest md = null;
        byte[] hash = null;
        // Llamamos a la función de hash de java
        try {
            md = MessageDigest.getInstance("SHA-256");
            hash = md.digest(cadena.getBytes("UTF-8"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return convertToHex(hash);
    }

    /**
     * Converts the given byte[] to a hex string.
     *
     * @param raw the byte[] to convert
     * @return the string the given byte[] represents
     */
    private String convertToHex(byte[] raw) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < raw.length; i++) {
            sb.append(Integer.toString((raw[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Cifra un mensaje con AES 128 bits
     *
     * @param mensaje mensaje a cofrar
     * @param pass contraseña para cifrar
     * @return mensaje cifrado y codificado ademas en base64
     */
    public String cifrarAES(String mensaje, String pass) {
        try {
            // Generamos una clave de 128 bits adecuada para AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            // Alternativamente, una clave que queramos que tenga al menos 16 bytes
            // y nos quedamos con los bytes 0 a 15 = 128 = 16 Bytes x 8 bits
            key = new SecretKeySpec(pass.getBytes("UTF-8"), 0, 16, "AES");
            // Se obtiene un cifrador AES
            Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Se inicializa para encriptacion y se encripta el texto,
            // que debemos pasar como bytes.
            aes.init(Cipher.ENCRYPT_MODE, key);
            byte[] encriptado = aes.doFinal(mensaje.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encriptado);

        } catch (Exception ex) {
            System.err.println("Error al codificar con AES: " + ex.getMessage());
        }
        return null;

    }

    /**
     * Descodifica una cadena cifrada en AES 128
     *
     * @param mensaje mensaje cifrado
     * @param pass contraseña para descifrar
     * @return mensaje descifrado
     */
    public String descifrarAES(String mensaje, String pass) {
        try {

            // Generamos una clave de 128 bits adecuada para AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            // Alternativamente, una clave que queramos que tenga al menos 16 bytes
            // y nos quedamos con los bytes 0 a 15 = 128 = 16 Bytes x 8 bits
            key = new SecretKeySpec(pass.getBytes(), 0, 16, "AES");
            // Se obtiene un cifrador AES
            Cipher aes = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Se iniciliza el cifrador para desencriptar, con la
            // misma clave y se desencripta
            aes.init(Cipher.DECRYPT_MODE, key);
            byte[] encriptado = Base64.getDecoder().decode(mensaje);
            byte[] desencriptado = aes.doFinal(encriptado);
            // Texto obtenido, igual al original.
            return new String(desencriptado);
        } catch (Exception ex) {
            System.err.println("Error al descodificar con AES: " + ex.getLocalizedMessage());
        }

        return null;
    }

    /**
     * Salva la clave AES codificada en un fichero
     *
     * @param pass contraseña a lamcenar
     * @param fichero fichero donde se almacenará la clave
     */
    public void salvarClaveAES(String pass, String fichero) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();
            // Alternativamente, una clave que queramos que tenga al menos 16 bytes
            // y nos quedamos con los bytes 0 a 15 = 128 = 16 Bytes x 8 bits
            key = new SecretKeySpec(pass.getBytes("UTF-8"), 0, 16, "AES");
            String sal = Base64.getEncoder().encodeToString(key.getEncoded());
            PrintWriter ficheroSalida = new PrintWriter(
                    new FileWriter(fichero));
            ficheroSalida.println(sal);
            ficheroSalida.close();
        } catch (Exception ex) {
            System.err.println("Error al salvar clave AES: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Recupera las claves AES almacenaras en fichero
     *
     * @param fichero donde está almacenada la clave
     * @return
     */
    public String cargarClaveAES(String fichero) {
        try {
            BufferedReader ficheroEntrada = new BufferedReader(
                    new FileReader(fichero));

            String linea = null;
            String sal = "";
            while ((linea = ficheroEntrada.readLine()) != null) {
                sal += linea;
            }
            ficheroEntrada.close();
            byte[] clave = Base64.getDecoder().decode(sal);
            return new String(clave);
        } catch (Exception ex) {
            System.err.println("Error al importar clave AES: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Crea una clave AES el propio sistema y la almacena en fichero
     *
     * @param fichero
     */
    public void crearClaveAES(String fichero) {
        try {
            // Generamos una clave de 128 bits adecuada para AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = keyGenerator.generateKey();

            // Se salva y recupera de fichero la clave publica
            String sal = Base64.getEncoder().encodeToString(key.getEncoded());
            PrintWriter ficheroSalida = new PrintWriter(
                    new FileWriter(fichero + "-aes.dat"));
            ficheroSalida.println(sal);
            ficheroSalida.close();
        } catch (Exception ex) {
            System.err.println("Error al crear clave AES: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Leemos la clave pública de un fichero
     *
     * @param fichero fichero de clave pública
     * @return clave pública
     * @throws Exception
     */
    public PublicKey cargarClavePublicaRSA(String fichero) throws Exception {
        FileInputStream fis = new FileInputStream(fichero);
        int numBtyes = fis.available();
        byte[] bytes = new byte[numBtyes];
        fis.read(bytes);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec keySpec = new X509EncodedKeySpec(bytes);
        PublicKey keyFromBytes = keyFactory.generatePublic(keySpec);
        return keyFromBytes;
    }

    /**
     * Carga la Clave Privada desde fichero
     *
     * @param fichero fichero de clave privada
     * @return clave privada
     * @throws Exception
     */
    public PrivateKey cargarClavePrivadaRSA(String fichero) throws Exception {
        FileInputStream fis = new FileInputStream(fichero);
        int numBtyes = fis.available();
        byte[] bytes = new byte[numBtyes];
        fis.read(bytes);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        KeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        PrivateKey keyFromBytes = keyFactory.generatePrivate(keySpec);
        return keyFromBytes;
    }

    /**
     * Salva el par de claves (privada/pública) en un fichero
     *
     * @param key clave
     * @param fichero fichero de claves
     * @throws Exception
     */
    public void salvarClavesRSA(Key key, String fichero) throws Exception {
        byte[] publicKeyBytes = key.getEncoded();
        FileOutputStream fos = new FileOutputStream(fichero);
        fos.write(publicKeyBytes);
        fos.close();
    }

    /**
     * Crea las claves de cifrado RSA y las almacena en un fichero
     *
     * @param fichero
     */
    public void crearClavesRSA(String fichero) {
        try {
            // Generar el par de claves
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // Se salva y recupera de fichero la clave publica
            this.salvarClavesRSA(publicKey, fichero + "-rsa-public.dat");
            this.salvarClavesRSA(privateKey, fichero + "-rsa-private.dat");
        } catch (Exception ex) {
            System.err.println("Error al crear clave RSA: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Cifra un texto mediante RSA
     *
     * @param mensaje mensaje a cifrar
     * @param publicKeyFile Fichero de la clave pública
     * @return cadena cifrada
     */
    public String cifrarRSA(String mensaje, String publicKeyFile) {
        try {
            PublicKey publicKey = this.cargarClavePublicaRSA(publicKeyFile);
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encriptado = rsa.doFinal(mensaje.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encriptado);
        } catch (Exception ex) {
            System.err.println("Error al cifrar con RSA: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Descifra una cadena mediante RSA
     *
     * @param mensaje mensaje a cifrar
     * @param privateKeyFile fichero de clave provada
     * @return mensje cifrado
     */
    public String descifrarRSA(String mensaje, String privateKeyFile) {
        try {
            PrivateKey privateKey = this.cargarClavePrivadaRSA(privateKeyFile);
            Cipher rsa = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encriptado = Base64.getDecoder().decode(mensaje);
            byte[] desencriptado = rsa.doFinal(encriptado);
            return new String(desencriptado);
        } catch (Exception ex) {
            System.err.println("Error al descifrar con RSA: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Crea una clave de firma DSA y la almacena en fichero
     *
     * @param fichero fichero de claves
     */
    public void crearClavesDSA(String fichero) {
        try {
            // Generar el par de claves
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
            // Lo inicializamos (antes no lo he puestopara que seapor defecto)
            SecureRandom numero = SecureRandom.getInstance("SHA1PRNG");
            keyPairGenerator.initialize(1024, numero);
            // obtenemos el par de claves
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();

            // Se salva y recupera de fichero la clave publica
            this.salvarClavesDSA(publicKey, fichero + "-dsa-public.dat");
            this.salvarClavesDSA(privateKey, fichero + "-dsa-private.dat");
        } catch (Exception ex) {
            System.err.println("Error al crear clave DSA: " + ex.getLocalizedMessage());
        }
    }

    /**
     * Salva una clave Privada o pública DSA en fichero
     *
     * @param key clave
     * @param fichero fichero
     * @throws Exception
     */
    public void salvarClavesDSA(Key key, String fichero) throws Exception {
        byte[] publicKeyBytes = key.getEncoded();
        FileOutputStream fos = new FileOutputStream(fichero);
        fos.write(publicKeyBytes);
        fos.close();
    }

    /**
     * Leemos la clave pública de un fichero
     *
     * @param fichero fichero de clave pública
     * @return clave pública
     * @throws Exception
     */
    public PublicKey cargarClavePublicaDSA(String fichero) throws Exception {
        FileInputStream fis = new FileInputStream(fichero);
        int numBtyes = fis.available();
        byte[] bytes = new byte[numBtyes];
        fis.read(bytes);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        KeySpec keySpec = new X509EncodedKeySpec(bytes);
        PublicKey keyFromBytes = keyFactory.generatePublic(keySpec);
        return keyFromBytes;
    }

    /**
     * Carga la Clave Privada desde fichero
     *
     * @param fichero fichero de clave privada
     * @return clave privada
     * @throws Exception
     */
    public PrivateKey cargarClavePrivadaDSA(String fichero) throws Exception {
        FileInputStream fis = new FileInputStream(fichero);
        int numBtyes = fis.available();
        byte[] bytes = new byte[numBtyes];
        fis.read(bytes);
        fis.close();

        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        KeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
        PrivateKey keyFromBytes = keyFactory.generatePrivate(keySpec);
        return keyFromBytes;
    }

    /**
     * Firma un texto mediante DSA
     *
     * @param mensaje mensaje a firmar
     * @param publicKeyFile Fichero de la clave privada
     * @return cadena cifrada
     */
    public String firmarDSA(String mensaje, String privateKeyFile) {
        try {
            PrivateKey privateKey = this.cargarClavePrivadaDSA(privateKeyFile);
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initSign(privateKey);
            dsa.update(mensaje.getBytes("UTF-8"));
            byte[] firmado = dsa.sign(); // obtenemos la firma
            return Base64.getEncoder().encodeToString(firmado);
        } catch (Exception ex) {
            System.err.println("Error al firmar con DSA: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Comprueba la firma a una cadena
     *
     * @param original cadena original
     * @param firmado cadena firmada
     * @param publicKeyFile fichero de clave pública
     * @return
     */
    public boolean verificarDSA(String original, String firmado, String publicKeyFile) {
        try {
            PublicKey publicKey = this.cargarClavePublicaDSA(publicKeyFile);
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initVerify(publicKey);
            dsa.update(original.getBytes("UTF-8"));
            byte[] firma = Base64.getDecoder().decode(firmado);
            boolean check = dsa.verify(firma);
            return check;
        } catch (Exception ex) {
            System.err.println("Error al comprobar la firma DSA: " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Cifra con una sesión RSA/AES
     *
     * @param mensaje mensaje a difrar
     * @param publicKeyFile clave pública
     * @param sesionFile fichero de sesion
     * @return mensaje cifrado
     */
    public String cifrarConSesion(String mensaje, String publicKeyFile, String sesionFile) {
        try {
            //Se carga la clave pública
            PublicKey publicKey = this.cargarClavePublicaRSA(publicKeyFile);
            // Creamos la clave AES 
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            Key key = kg.generateKey();
            //se encripta la clave secreta con la clave pública
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.WRAP_MODE, publicKey);
            byte sesioncifrada[] = c.wrap(key);
            // salvamos la calve de sesion.
            this.salvarClaveSesion(sesioncifrada, sesionFile);
            // Ciframos el texto como si fuese AES
            c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encriptado = c.doFinal(mensaje.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encriptado);
        } catch (Exception ex) {
            System.err.println("Error al cifrar con sesion: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Descifra con una sesión RSA/AES
     *
     * @param mensaje mensaje a descifrar
     * @param privateKeyFile clave privada
     * @param sesionFile fichero de sesión
     * @return mensaje descifrado
     */
    public String descifrarConSesion(String mensaje, String privateKeyFile, String sesionFile) {
        try {
            //Se carga la clave pública
            PrivateKey privateKey = this.cargarClavePrivadaRSA(privateKeyFile);
            // leemos el fichero de sesion cifrado
            byte[] sesioncifrada = this.cargarClaveSesion(sesionFile);
            //se desencripta  la clave secreta con la clave privada
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.UNWRAP_MODE, privateKey);
            Key sesion = c.unwrap(sesioncifrada, "AES", Cipher.SECRET_KEY);
            // Desciframos AES
            c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Se iniciliza el cifrador para desencriptar, con la
            // misma clave y se desencripta
            c.init(Cipher.DECRYPT_MODE, sesion);
            byte[] encriptado = Base64.getDecoder().decode(mensaje);
            byte[] desencriptado = c.doFinal(encriptado);
            // Texto obtenido, igual al original.
            return new String(desencriptado);
        } catch (Exception ex) {
            System.err.println("Error al descifrar con sesion: " + ex.getLocalizedMessage());
        }
        return null;
    }

    /**
     * Salva la clave de sesión
     *
     * @param clave clave
     * @param fichero dichero donde se guarda
     * @throws Exception
     */
    public void salvarClaveSesion(byte[] clave, String fichero) throws Exception {
        FileOutputStream fos = new FileOutputStream(fichero);
        fos.write(clave);
        fos.close();
    }

    /**
     * Recupera la clave sesión
     *
     * @param fichero fichero de clave
     * @return array de clave
     * @throws Exception
     */
    public byte[] cargarClaveSesion(String fichero) throws Exception {
        FileInputStream fis = new FileInputStream(fichero);
        int numBtyes = fis.available();
        byte[] bytes = new byte[numBtyes];
        fis.read(bytes);
        fis.close();
        return bytes;
    }

    /**
     * Firma un fichero
     *
     * @param fichero fichero a firmar
     * @param clavePrivada fichero con la clave privada
     * @param firma fichero con la firma
     */
    public void firmarFicheroDSA(File fichero, File clavePrivada, File firma) {
        try {
            // Cargamos la clave
            PrivateKey privateKey = this.cargarClavePrivadaDSA(clavePrivada.getAbsolutePath());
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initSign(privateKey);
            // Cargamos el fichero
            FileInputStream fin = new FileInputStream(fichero);
            BufferedInputStream bis = new BufferedInputStream(fin);
            byte[] buffer = new byte[bis.available()];
            int len;
            // recorremos mientras firmamos
            while ((len = bis.read(buffer)) >= 0) {
                dsa.update(buffer, 0, len);
            }
            bis.close();
            // Generamos la firma
            byte[] fir = dsa.sign();
            // Guarda la firma en el fichero 
            FileOutputStream fos = new FileOutputStream(firma);
            fos.write(fir);
            fos.close();
        } catch (Exception ex) {
            System.err.println("Error al firmar fichero: " + ex.getLocalizedMessage());
        }

    }

    /**
     * Verifica la firma en un fichero por DSA
     *
     * @param fichero fichero a verificar
     * @param clavePublica clave pública
     * @param firma fichero de firma
     * @return verdadero o falso si coinciden
     */
    public boolean verificarFicheroDSA(File fichero, File clavePublica, File firma) {
        try {
            // Cargamos la firma
            PublicKey publicKey = this.cargarClavePublicaDSA(clavePublica.getAbsolutePath());
            // Cargamos el fichero de la firma
            FileInputStream firm = new FileInputStream(firma);
            byte[] fir = new byte[firm.available()];
            firm.read(fir);
            firm.close();
            //iniciamos el cifrador
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initVerify(publicKey);
            //Lectura del fichero con los datos a verificar
            //Se suministra al objeto Signature los datos a verificar 
            FileInputStream fin = new FileInputStream(fichero);
            BufferedInputStream bis = new BufferedInputStream(fin);
            byte[] buffer = new byte[bis.available()];
            int len;
            while ((len = bis.read(buffer)) >= 0) {
                dsa.update(buffer, 0, len);
            }
            bis.close();
            //Verificamos la firma de los datos leídos 
            boolean verifica = dsa.verify(fir);
            return verifica;

        } catch (Exception ex) {
            System.err.println("Error al verificar fichero: " + ex.getLocalizedMessage());
        }
        return false;
    }

    /**
     * Firma un fichero con AES
     *
     * @param fichero fichero a cifrar
     * @param destino fichero destino
     * @param clave clave a usar
     */
    public void cifrarFicheroAES(File fichero, File destino, String clave) {
        try {
            // Generamos una clave de 128 bits adecuada para AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = new SecretKeySpec(clave.getBytes("UTF-8"), 0, 16, "AES");
            // Se obtiene un cifrador AES
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, key);
            // Fichero a cifrar
            FileInputStream filein = new FileInputStream(fichero);
            // Objeto CipherOutputStream donde se almacena el fichero cifrado 
            CipherOutputStream out = new CipherOutputStream(new FileOutputStream(destino), c);
            int tambloque = c.getBlockSize();   //tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //leemos los bloques de bytes del fichero original
            // Lo vamos escribiendo en el buffer del cifrado
            int i = filein.read(bytes);
            while (i != -1) {
                out.write(bytes, 0, i);
                i = filein.read(bytes);
            }
            out.flush();
            out.close();
            filein.close();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException ex) {
            System.err.println("Error al cifrar fichero " + ex.getLocalizedMessage());
        }
    }

    /**
     * Descifraun fichero usando AES
     *
     * @param fichero fichero a cifrar
     * @param destino fichero destino
     * @param clave clave
     */
    public void descifrarFicheroAES(File fichero, File destino, String clave) {
        try {
            // Generamos una clave de 128 bits adecuada para AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            Key key = new SecretKeySpec(clave.getBytes("UTF-8"), 0, 16, "AES");
            // Se obtiene un cifrador AES
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key);
            //Objeto CipherinputStream cuyo contenido se va a descifrar
            CipherInputStream in = new CipherInputStream(new FileInputStream(fichero), c);
            int tambloque = c.getBlockSize();//tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //fichero descifrado que se creará
            FileOutputStream fileout = new FileOutputStream(destino);
            //Leemos los bloques del fichero cifrado y los desciframos en el destino
            int i = in.read(bytes);
            while (i != -1) {
                fileout.write(bytes, 0, i);
                i = in.read(bytes);
            }
            fileout.close();
            in.close();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException ex) {
            System.err.println("Error al descifrar fichero " + ex.getLocalizedMessage());
        }
    }

    /**
     * Ciframos un fichero con RSA
     *
     * @param fichero fichero a cifrar
     * @param destino fichero destino (cifrado)
     * @param clavePublica fichero de clave pública
     */
    public void cifrarFicheroRSA(File fichero, File destino, File clavePublica) {
        try {
            // Cargamos la clave pública
            PublicKey key = this.cargarClavePublicaRSA(clavePublica.getAbsolutePath());
            //Inicializamos el cifrador
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.ENCRYPT_MODE, key);
            // Fichero a cifrar
            FileInputStream filein = new FileInputStream(fichero);
            // Objeto CipherOutputStream donde se almacena el fichero cifrado 
            CipherOutputStream out = new CipherOutputStream(new FileOutputStream(destino), c);
            int tambloque = c.getBlockSize();   //tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //leemos los bloques de bytes del fichero original
            // Lo vamos escribiendo en el buffer del cifrado
            int i = filein.read(bytes);
            while (i != -1) {
                out.write(bytes, 0, i);
                i = filein.read(bytes);
            }
            out.flush();
            out.close();
            filein.close();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException ex) {
            System.err.println("Error al cifrar fichero " + ex.getLocalizedMessage());
        } catch (Exception ex) {
            System.err.println("Error al cifrar fichero " + ex.getLocalizedMessage());
        }
    }

    /**
     * Descifraun fichero usando AES
     *
     * @param fichero fichero a cifrar
     * @param destino fichero destino
     * @param clave clave
     */
    public void descifrarFicheroRSA(File fichero, File destino, File clavePrivada) {
        try {
            // Cargamos la clave pública
            PrivateKey key = this.cargarClavePrivadaRSA(clavePrivada.getAbsolutePath());
            //Inicializamos el cifrador
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.DECRYPT_MODE, key);
            //Objeto CipherinputStream cuyo contenido se va a descifrar
            CipherInputStream in = new CipherInputStream(new FileInputStream(fichero), c);
            int tambloque = c.getBlockSize();//tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //fichero descifrado que se creará
            FileOutputStream fileout = new FileOutputStream(destino);
            //Leemos los bloques del fichero cifrado y los desciframos en el destino
            int i = in.read(bytes);
            while (i != -1) {
                fileout.write(bytes, 0, i);
                i = in.read(bytes);
            }
            fileout.close();
            in.close();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException ex) {
            System.err.println("Error al descifrar fichero " + ex.getLocalizedMessage());
        } catch (Exception ex) {
            System.err.println("Error al descifrar fichero " + ex.getLocalizedMessage());
        }
    }

    /**
     * Ciframos un fichero con RSA/AES y lo empaquetamos.
     *
     * @param fichero
     * @param destino
     * @param clavePublica
     */
    public void cifrarFicheroCIF(File fichero, File destino, File clavePublica) {
        try {
            // Cargamos la clave pública
            PublicKey publicKey = this.cargarClavePublicaRSA(clavePublica.getAbsolutePath());

            // Creamos la clave AES 
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            Key key = kg.generateKey();

            //se encripta la clave secreta con la clave pública
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.WRAP_MODE, publicKey);
            byte sesioncifrada[] = c.wrap(key);

            // salvamos la clave sesion en el disco
            File sesion = new File("file.key");
            FileOutputStream fos = new FileOutputStream(sesion);
            fos.write(sesioncifrada);
            fos.close();

            //ciframos el fichero con AES Y no refatorizo para que os epapeis el codigo de memoria
            // Se obtiene un cifrador AES
            c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, key);
            // Fichero a cifrar
            FileInputStream filein = new FileInputStream(fichero);
            // Objeto CipherOutputStream donde se almacena el fichero cifrado 
            File temp = new File("file.cmp");
            CipherOutputStream out = new CipherOutputStream(new FileOutputStream(temp), c);
            int tambloque = c.getBlockSize();   //tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //leemos los bloques de bytes del fichero original
            // Lo vamos escribiendo en el buffer del cifrado
            int i = filein.read(bytes);
            while (i != -1) {
                out.write(bytes, 0, i);
                i = filein.read(bytes);
            }
            out.flush();
            out.close();
            filein.close();

            // Creamos el fichero cif (que es un zip)
            List<String> srcFiles = Arrays.asList(sesion.getAbsolutePath(), temp.getAbsolutePath());
            fos = new FileOutputStream(destino);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            for (String srcFile : srcFiles) {
                File fileToZip = new File(srcFile);
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zipOut.putNextEntry(zipEntry);

                bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }
            zipOut.close();
            fos.close();

            // Borramos el fichero de cla clave, y el fichero cifrado una vez hecho
            sesion.delete();
            temp.delete();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException ex) {
            System.err.println("Error al cifrar fichero " + ex.getLocalizedMessage());
        } catch (Exception ex) {
            System.err.println("Error al cifrar fichero " + ex.getLocalizedMessage());
        }
    }

    /**
     * Descifra un fichero RSA/AES desempaquetándolo
     *
     * @param fichero fichero.cif de origen
     * @param destino fichero a guardar
     * @param clavePrivada clave privada
     */
    public void descifrarFicheroCIF(File fichero, File destino, File clavePrivada) {
        try {
            // Descomprimimos los ficheros
            byte[] buffer = new byte[1024];
            ZipInputStream zis = new ZipInputStream(new FileInputStream(fichero));
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                FileOutputStream fos = new FileOutputStream(zipEntry.getName());
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

            //Se carga la clave rivada
            PrivateKey privateKey = this.cargarClavePrivadaRSA(clavePrivada.getAbsolutePath());

            // leemos el fichero de sesion cifrado
            File sesion = new File("file.key");
            FileInputStream fis = new FileInputStream(sesion);
            int numBtyes = fis.available();
            byte[] sesioncifrada = new byte[numBtyes];
            fis.read(sesioncifrada);
            fis.close();

            //se desencripta  la clave secreta con la clave de sesión
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.UNWRAP_MODE, privateKey);
            Key key = c.unwrap(sesioncifrada, "AES", Cipher.SECRET_KEY);

            // Se obtiene un cifrador AES
            c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, key);

            //Objeto CipherinputStream cuyo contenido se va a descifrar
            File temp = new File("file.cmp");
            CipherInputStream in = new CipherInputStream(new FileInputStream(temp), c);
            int tambloque = c.getBlockSize();//tamaño de bloque 
            byte[] bytes = new byte[tambloque]; //bloque de bytes
            //fichero descifrado que se creará
            FileOutputStream fileout = new FileOutputStream(destino);
            //Leemos los bloques del fichero cifrado y los desciframos en el destino
            int i = in.read(bytes);
            while (i != -1) {
                fileout.write(bytes, 0, i);
                i = in.read(bytes);
            }
            fileout.close();
            in.close();

            // Borramos el fichero de cla clave, y el fichero cifrado una vez hecho
            sesion.delete();
            temp.delete();
        } catch (Exception ex) {
            Logger.getLogger(Cifrador.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
