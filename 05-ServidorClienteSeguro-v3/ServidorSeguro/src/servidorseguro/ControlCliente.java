/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;

/**
 *
 * @author link
 */
public class ControlCliente extends Thread {

    private Socket cliente = null;
    DataInputStream controlEntrada = null;
    DataOutputStream controlSalida = null;
    private int contador = 1;
    private boolean salir = false;
    String ID;
    private static final int MAX = 20;
    private Key sessionKey = null;
    private PrivateKey privateKey = null;
    private byte[] sesionCifrada = null;
    private PublicKey publicKey;

    public ControlCliente(Socket cliente) {
        this.cliente = cliente;
        this.contador = 1;
        this.salir = false;
        this.ID = cliente.getInetAddress()+":"+cliente.getPort();
        
    }

    @Override
    public void run() {
        // Trabajamos con ella
        if (salir == false) {
            crearFlujosES();
            // Datos de la sesion
            sesion();
            // Tratamos la conexion
            tratarConexion();
            cerrarFlujosES();
        } else {
            this.interrupt(); // Me interrumpo y no trabajo
        }

    }

    private void tratarConexion() {
        // Escuchamos hasta aburrirnos, es decir, hasta que salgamos
        while (!salir) {
            //Recibimos un mensaje
            recibir();
            // Devolvemos una respuesta
            enviar();
            // Aumentamos el contador
                this.contador++;
                 // Le indicamos si sale
            if(!salir){
                salir();
               
            }
        }
    }

    private void crearFlujosES() {
        try {
            controlEntrada = new DataInputStream(cliente.getInputStream());
            controlSalida = new DataOutputStream(cliente.getOutputStream());
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: crear flujos de entrada y salida " + ex.getMessage());
        }
    }

    private void cerrarFlujosES() {
        try {
            controlEntrada.close();
            controlSalida.close();
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: cerrar flujos de entrada y salida " + ex.getMessage());
        }
    }

    private void salir() {
        if (this.contador >= this.MAX) {
            this.salir = true;
        } else { // No es necssario pero es un ejemplo didáctico y quiero que quede claro
            this.salir = false;
        }
        // Envamos la respuesta
        try {
            System.out.println("ServidorGC->Enviar si salir");
            String salida = String.valueOf(this.salir);
            controlSalida.writeUTF(this.cifrar(salida));
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al enviar ID de Cliente " + ex.getMessage());
        }
    }

    private void recibir() {
        System.out.println("ServidorGC->Recepción de mensajes");
        try {
            String dato = this.descifrar(this.controlEntrada.readUTF());
            System.out.println("ServidorGC->Mensaje recibido de ["+this.ID+"]: " + dato);
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al recibir mensaje " + ex.getMessage());
        }
    }

    private void enviar() {
        System.out.println("ServidorGC->Enviado mensaje");
        try {
            String dato = "Mensaje de reespuesta num: " + this.contador;
            this.controlSalida.writeUTF(this.cifrar(dato));
            System.out.println("ServidorGC->Mensaje enviado a ["+this.ID+"]: " + dato);
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al enviar mensaje " + ex.getMessage());
        }
    }

    private void sesion() {
        // cargamos la clave pública
        clavePublica();
        // Enviamos la clave pública
        enviarClave();
        // Leemos la clave privada
        clavePrivada();
        // recibimos la clave de sesion
        recibirClave();
        //desciframos la clave
        descifrarClave();
    }

    private void clavePrivada() {
        FileInputStream fis = null;
        String fichero = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"claves-rsa-private.dat";
        try {
            fis = new FileInputStream(fichero);
            int numBtyes = fis.available();
            byte[] bytes = new byte[numBtyes];
            fis.read(bytes);
            fis.close();
            
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
            this.privateKey = keyFactory.generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.err.println("ServidorGC->ERROR: al cargar la clave privada " + ex.getMessage());
        }
    }

    private void recibirClave() {
        System.out.println("ServidorGC->Recibiendo clave de sesión");
        try {
            // leemos la longitid
            int l =  this.controlEntrada.readInt();
            byte[] clave = new byte[l];
            this.controlEntrada.read(clave);
            System.out.println("ServidorGC->Clave de sesión recibida de ["+this.ID+"]: "+ clave.toString());
            this.sesionCifrada = clave;
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al recibir mensaje " + ex.getMessage());
        }
    }

    private void descifrarClave() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(128);
            this.sessionKey = kg.generateKey();
            Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            c.init(Cipher.UNWRAP_MODE, privateKey);
            this.sessionKey = c.unwrap(this.sesionCifrada, "AES", Cipher.SECRET_KEY);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            System.err.println("ServidorGC->ERROR: al descodificar clave de sesion " + ex.getMessage());
        }
    }
    
    private String cifrar(String mensaje){
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, this.sessionKey);
            byte[] encriptado = c.doFinal(mensaje.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encriptado);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | UnsupportedEncodingException | IllegalBlockSizeException | BadPaddingException ex) {
            System.err.println("ServidorGC->ERROR: cifrar mensaje " + ex.getMessage());
        }
        return null;
    }
    
    private String descifrar(String mensaje){
        try {
            Cipher c = Cipher.getInstance("AES/ECB/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, this.sessionKey);
            byte[] encriptado = Base64.getDecoder().decode(mensaje);
            byte[] desencriptado = c.doFinal(encriptado);
            // Texto obtenido, igual al original.
            return new String(desencriptado);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidKeyException ex) {
             System.err.println("ServidorGC->ERROR: descifrar mensaje " + ex.getMessage());
        }
        return null;
    }

    private void clavePublica() {
        FileInputStream fis = null;
        String fichero = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"claves-rsa-public.dat";
        try {
            fis = new FileInputStream(fichero);
            int numBtyes = fis.available();
            byte[] bytes = new byte[numBtyes];
            fis.read(bytes);
            fis.close();
            
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            KeySpec keySpec = new X509EncodedKeySpec(bytes);
            this.publicKey = keyFactory.generatePublic(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException ex) {
            System.err.println("ServidorGC->ERROR: al cargar la clave publica " + ex.getMessage());
        }
    }

    private void enviarClave() {
        // la pasamos cifrada pero como Code64 para que vaya como un string
        byte[] clave = this.publicKey.getEncoded();
        System.out.println("ServidorGC->Enviado clave de publica");
        try {
            // Mandamos longitud y clave
            this.controlSalida.writeInt(clave.length);
            this.controlSalida.write(clave);
            System.out.println("ServidorGC->Clave publica " + clave.toString());
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al enviar clave " + ex.getMessage());
        }
    }
       

}
