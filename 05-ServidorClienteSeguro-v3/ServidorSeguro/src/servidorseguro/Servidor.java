/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorseguro;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author link
 */
public class Servidor {

    private final int PUERTO = 6666;
    private ServerSocket servidorControl = null;
    Socket cliente = null;
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
            // Nos anunciamos como servidorControl
            servidorControl = new ServerSocket(this.PUERTO);
            System.out.println("Servidor->Listo. Esperando cliente...");
        } catch (IOException ex) {
            System.err.println("Servidor->ERROR: apertura de puerto " + ex.getMessage());
            System.exit(-1);
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
            cliente = servidorControl.accept();
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