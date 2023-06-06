/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSession;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.File;

/**
 *
 * @author link
 */
public class Cliente {

    private final int PUERTO = 6666;
    //private InetAddress direccion;
    private String direccion;
    private SSLSocket servidor;
    private SSLSocketFactory clientFactory; 
    private boolean salir = false;
    DataInputStream datoEntrada = null;
    DataOutputStream datoSalida = null;
    private static final int ESPERAR = 1000;

    public void iniciar() {
        // Antes de nada compruebo la direccion
        comprobar();
        // Nos conectamos
        conectar();
        //información de la sesión
        sesion();
        // Procesamos
        procesar();
        //Cerramos
        cerrar();
    }

    private void comprobar() {
        try {
            // Consigo la dirección (ojo está puesta la de mi ma´uina virtual)
            direccion = InetAddress.getLocalHost().getHostAddress(); //"192.168.169.2"; //InetAddress. //InetAddress.getLocalHost(); // dirección local (localhost)
        } catch (UnknownHostException ex) {
            System.err.println("Cliente->ERROR: No encuetra dirección del servidor");
            System.exit(-1);
        }
    }

    private void conectar() {
        try {
            // De donde saco los datos
            String fichero = System.getProperty("user.dir")+File.separator+"cert17"+File.separator+"UsuarioAlmacenSSL17.jks";
            comprobarCertificadoExiste(fichero);
            // Para depurar y ver el dialogo y handshake
            System.setProperty("javax.net.debug", "ssl, keymanager, handshake");

            System.setProperty("javax.net.ssl.trustStore", fichero);
            System.setProperty("javax.net.ssl.trustStorePassword","0987654");
            // Me conecto
            this.clientFactory = (SSLSocketFactory)SSLSocketFactory.getDefault();
            this.servidor = (SSLSocket) clientFactory.createSocket(this.direccion, this.PUERTO);
            datoEntrada = new DataInputStream(servidor.getInputStream());
            datoSalida = new DataOutputStream(servidor.getOutputStream());
            System.out.println("Cliente->Conectado al servidor...");
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: No se puede conectar");
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    private void cerrar() {
        try {
            // Me desconecto
            servidor.close();
            datoEntrada.close();
            datoSalida.close();
            System.out.println("Cliente->Desconectado");
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: No se puede conectar");
            System.exit(-1);
        }
    }

    private void procesar() {
        // Ciclamos hasta que salgamos
        // Escuchamos hasta aburrirnos, es decir, hasta que salgamos
        while (!salir) {
            //Envaimos el mensaje
            enviar();
            // recibimos la respuesta
            recibir();
            // vemos si salimos
            salir();
            // esperamos
            esperar();
        }

    }

    private void enviar() {
        System.out.println("Cliente->Enviado mensaje");
        try {
            String dato = "Mensaje: " + Instant.now().getEpochSecond();
            this.datoSalida.writeUTF(dato);
            System.out.println("Cliente->Mensaje enviado a Servidor: " + dato);
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: al enviar mensaje " + ex.getMessage());
        }
    }

    private void recibir() {
        try {
            System.out.println("Cliente->Recepción de mensajes");
            String dato = this.datoEntrada.readUTF();
            System.out.println("Cliente->Mensaje recibido: " + dato);
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: al recibir mensaje " + ex.getMessage());
        }
    }

    private void salir() {
        try {
            System.out.println("Cliente->¿Salir?");
            this.salir = this.datoEntrada.readBoolean();
            System.out.println("Cliente->Salir: " + this.salir);
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: al recibir salir " + ex.getMessage());
        }
    }

    private void esperar() {
        try {
            Thread.sleep(this.ESPERAR);
        } catch (InterruptedException ex) {
            System.err.println("Cliente->ERROR: al esperar " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void sesion() {
        try {
            SSLSession sesion = this.servidor.getSession();
            System.out.println("Servidor: "+sesion.getPeerHost());
            System.out.println("Cifrado: "+ sesion.getCipherSuite());
            System.out.println("Protocolo: " + sesion.getProtocol ());
            System.out.println("IDentificador:" + new BigInteger(sesion.getId()));
            System.out.println("Creación de la sesión: " +sesion.getCreationTime());
            X509Certificate certificado =(X509Certificate) sesion.getPeerCertificates()[0];
            System.out.println ("Propietario : "+certificado.getSubjectDN());
            System.out.println("Algoritmo: " +certificado.getSigAlgName());
            System.out.println("Tipo: "+certificado.getType());
            System.out.println ("Emisor: "+certificado.getIssuerDN());
            System.out.println("Número Serie: "+certificado.getSerialNumber());
        } catch (SSLPeerUnverifiedException ex) {
            System.err.println("Cliente->ERROR: al leer información del certificado " + ex.getMessage());
        }

    }

    private void comprobarCertificadoExiste(String fichero) {
        if (!Files.exists(Path.of(fichero))) {
            System.err.println("No se encuentra el fichero de certificado del servidor");
            System.exit(0);
        }
    }

}
