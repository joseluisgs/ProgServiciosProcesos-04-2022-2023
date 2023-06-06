/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servidorseguro;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author link
 */
public class ControlCliente extends Thread {

    private Socket cliente = null;
    DataInputStream controlEntrada = null;
    DataOutputStream controlSalida = null;
    private int contador = 0;
    private boolean salir = false;
    String ID;
    private static final int MAX = 20;

    public ControlCliente(Socket cliente) {
        this.cliente = cliente;
        this.contador = 0;
        this.salir = false;
        this.ID = cliente.getInetAddress()+":"+cliente.getPort();
    }

    @Override
    public void run() {
        // Trabajamos con ella
        if (salir == false) {
            crearFlujosES();
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
            controlSalida.writeBoolean(this.salir);
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al enviar ID de Cliente " + ex.getMessage());
        }
    }

    private void recibir() {
        System.out.println("ServidorGC->Recepción de mensajes");
        try {
            String dato = this.controlEntrada.readUTF();
            System.out.println("ServidorGC->Mensaje recibido de ["+this.ID+"]: " + dato);
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al recibir mensaje " + ex.getMessage());
        }
    }

    private void enviar() {
        System.out.println("ServidorGC->Enviado mensaje");
        try {
            String dato = "Mensaje de reespuesta num: " + this.contador;
            this.controlSalida.writeUTF(dato);
            System.out.println("ServidorGC->Mensaje enviado a ["+this.ID+"]: " + dato);
        } catch (IOException ex) {
            System.err.println("ServidorGC->ERROR: al enviar mensaje " + ex.getMessage());
        }
    }

}
