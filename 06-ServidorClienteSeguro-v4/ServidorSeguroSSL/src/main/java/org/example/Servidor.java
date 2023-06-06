/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.cert.CertificateException;
import javax.net.ssl.*;

/**
 *
 * @author link
 */
public class Servidor {

    private final int PUERTO = 6666;
    private SSLServerSocketFactory serverFactory; 
    private SSLServerSocket servidorControl;
    private SSLSocket cliente = null;
    private boolean salir = false;
    

    // Patron Singleton -> Unsa sola instancia
    private static Servidor servidor;
    private Servidor() {
       
    }
    
    public static Servidor iniciarServidor() {
        if (servidor == null) {
            servidor = new Servidor();
            servidor.iniciarControl();
        }
        return servidor;
    }

    private void iniciarControl() {
        // Prparamos conexion
        prepararConexion();
        // Trabajamos con ella
        tratarConexion();
        // Cerramos la conexion
        cerrarConexion();
    }

    private void prepararConexion() {
        try {
            // Obtengo el KeyStore
            // System.setProperty("javax.net.debug", "all");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "1234567"; // Necesitamos nuestra clave para leerlo
            String certificadoServidor = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"server-certificate.p12";
            InputStream inputStreamServidor = ClassLoader.getSystemClassLoader().getResourceAsStream(certificadoServidor);
            keyStore.load(inputStreamServidor, password.toCharArray());

            // TrustManagerFactory, necesitamos nuestra clave para añadir el cliente
            String password2 = "1234567";
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");

            // cargamos el certificado del cliente
            String certificadoCliente = System.getProperty("user.dir")+File.separator+"cert"+File.separator+"client-certificate.p12";
            InputStream inputStreamCliente = ClassLoader.getSystemClassLoader().getResourceAsStream(certificadoCliente);
            trustStore.load(inputStreamCliente, password2.toCharArray());
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

            // Creamos el contexto SSL
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{x509KeyManager}, new TrustManager[]{x509TrustManager}, null);

            this.serverFactory = sslContext.getServerSocketFactory();
            this.servidorControl = (SSLServerSocket)  serverFactory.createServerSocket(this.PUERTO);
            // indicamos que necesita autenticacion por parte del cliente, por eso su certificado
            this.servidorControl.setNeedClientAuth(true);
            this.servidorControl.setEnabledProtocols(new String[]{"TLSv1.2"});
            System.out.println("Servidor->Listo. Esperando cliente...");
        } catch (IOException ex) {
            System.err.println("Servidor->ERROR: apertura de puerto " + ex.getMessage());
            System.exit(-1);
        } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException | NoSuchProviderException |
                 UnrecoverableKeyException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void cerrarConexion() {
        try {
            // Cerramos el cliente y el servidorControl
            cliente.close();
            servidorControl.close();
            System.out.println("Servidor->Cerrando la conexión");
            System.exit(0);
        } catch (IOException ex) {
            System.err.println("Servidor->ERROR: Cerrar Conexiones" + ex.getMessage());
        }
    }

    private void tratarConexion() {
        // Escuchamos hasta aburrirnos, es decir, hasta que salgamos
        while (!salir) {
            //Aceptamos la conexion
            aceptarConexion();
            // Procesamos el cliente
            procesarCliente();
        }
    }

    private void procesarCliente() {
        System.out.println("Servidor->Iniciando sistema de control");
        ControlCliente gc = new ControlCliente(cliente);
        gc.start();
    }

    private void aceptarConexion() {
        // Aceptamos la petición
        try {
            cliente = (SSLSocket)servidorControl.accept();
            System.out.println("Servidor->Llega el cliente: " + cliente.getInetAddress() +":"+cliente.getPort());
        } catch (IOException ex) {
            System.err.println("Servidor->ERROR: aceptar conexiones " + ex.getMessage());
        }
    }

    public void destruir() {
        try {
            super.finalize();
        } catch (Throwable ex) {
            System.err.println("Servidor->Error al destruir");
        }
    }

}