/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.example;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

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
            // De donde sacamos los datos
            // De donde saco los datos
            String fichero = System.getProperty("user.dir")+File.separator+"cert17"+File.separator+"AlmacenSSL17.jks";
            comprobarCertificadoExiste(fichero);

            // Para depurar y ver el dialogo y handshake
            System.setProperty("javax.net.debug", "ssl, keymanager, handshake");

            System.setProperty("javax.net.ssl.keyStore", fichero);
            System.setProperty("javax.net.ssl.keyStorePassword","1234567");
            // Nos anunciamos como servidorControl de tipo SSL
            this.serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            this.servidorControl = (SSLServerSocket) serverFactory.createServerSocket(this.PUERTO);
            System.out.println("Servidor->Listo. Esperando cliente...");
        } catch (IOException ex) {
            System.err.println("Servidor->ERROR: apertura de puerto " + ex.getMessage());
            System.exit(-1);
        }
    }

    private void comprobarCertificadoExiste(String fichero) {
        if (!Files.exists(Path.of(fichero))) {
            System.err.println("No se encuentra el fichero de certificado del servidor");
            System.exit(0);
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