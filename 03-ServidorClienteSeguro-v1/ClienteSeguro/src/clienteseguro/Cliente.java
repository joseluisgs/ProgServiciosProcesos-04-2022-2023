/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package clienteseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author link
 */
public class Cliente {

    private final int PUERTO = 6666;
    //private InetAddress direccion;
    private String direccion;
    private Socket servidor;
    private boolean salir = false;
    DataInputStream datoEntrada = null;
    DataOutputStream datoSalida = null;
    private static final int ESPERAR = 1000;

    public void iniciar() {
        // Antes de nada compruebo la direccion
        comprobar();
        // Nos conectamos
        conectar();
        // Procesamos
        procesar();
        //Cerramos
        cerrar();
    }

    private void comprobar() {
       try {
            // Consigo la dirección
            direccion = InetAddress.getLocalHost().getHostAddress();
       } catch (UnknownHostException ex) {
            System.err.println("Cliente->ERROR: No encuetra dirección del servidor");
            System.exit(-1);
       }
    }

    private void conectar() {
        try {
            // Me conecto
            servidor = new Socket(direccion, PUERTO);
            datoEntrada = new DataInputStream(servidor.getInputStream());
            datoSalida = new DataOutputStream(servidor.getOutputStream());
            System.out.println("Cliente->Conectado al servidor...");
        } catch (IOException ex) {
            System.err.println("Cliente->ERROR: No se puede conectar");
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
        }
    }

}
