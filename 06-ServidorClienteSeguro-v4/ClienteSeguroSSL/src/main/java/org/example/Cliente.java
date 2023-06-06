/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Instant;
import javax.net.ssl.*;
import java.math.BigInteger;
import java.security.cert.X509Certificate;

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
            // Cojo el KeyStore
            //System.setProperty("javax.net.debug", "all");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "1234567"; // Necesitamos nuestra clave para leerlo
            String certificadoCliente = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"client-certificate.p12";
            InputStream inputStreamCliente = ClassLoader.getSystemClassLoader().getResourceAsStream("certificadoCliente");
            keyStore.load(inputStreamCliente, password.toCharArray());

            // TrustManagerFactory ()
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            String password2 = "1234567"; // Necesitamos nuestra clave para añadir el servidor
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
            String certificadoServidor = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"server-certificate.p12";
            InputStream inputStreamServidor = ClassLoader.getSystemClassLoader().getResourceAsStream("certificadoServidor");
            trustStore.load(inputStreamServidor, password2.toCharArray());
            trustManagerFactory.init(trustStore);
            X509TrustManager x509TrustManager = null;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    x509TrustManager = (X509TrustManager) trustManager;
                    break;
                }
            }

            if (x509TrustManager == null) throw new NullPointerException();

            // KeyManagerFactory ()
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            keyManagerFactory.init(keyStore, password.toCharArray());
            X509KeyManager x509KeyManager = null;
            for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager) {
                    x509KeyManager = (X509KeyManager) keyManager;
                    break;
                }
            }
            if (x509KeyManager == null) throw new NullPointerException();

            // Creamos el contexto de SSL
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{x509KeyManager}, new TrustManager[]{x509TrustManager}, null);
            this.clientFactory = sslContext.getSocketFactory();
            this.servidor = (SSLSocket) clientFactory.createSocket(this.direccion, this.PUERTO);
            this.servidor.setEnabledProtocols(new String[]{"TLSv1.2"});

            // Streams de Entrada y Salida
            datoEntrada = new DataInputStream(servidor.getInputStream());
            datoSalida = new DataOutputStream(servidor.getOutputStream());
            System.out.println("Cliente->Conectado al servidor...");

        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: No se puede conectar");
            System.exit(-1);
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException |
                 NoSuchProviderException | KeyManagementException e) {
            e.printStackTrace();
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
        }
    }

    private void sesion() {
        try {
            SSLSession sesion = ((SSLSocket)this.servidor).getSession();
            System.out.println("Servidor: "+sesion.getPeerHost());
            System.out.println("Cifrado: "+ sesion.getCipherSuite());
            System.out.println("Protocolo: " + sesion.getProtocol ());
            //System.out.println("IDentificador:" + new BigInteger(sesion.getId()));
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

}
