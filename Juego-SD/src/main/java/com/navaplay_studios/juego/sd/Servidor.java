/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.navaplay_studios.juego.sd;

import java.io.*;
import java.net.*;

/**
 *
 * @author ferga
 */
class ConexionCliente extends Thread {

    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private Socket cliente;

    public ConexionCliente(Socket cliente) throws IOException {
        this.cliente = cliente;
        this.salida = new ObjectOutputStream(this.cliente.getOutputStream());
        this.entrada = new ObjectInputStream(this.cliente.getInputStream());
    }

    public void cerrarConexion() throws IOException {
        salida.close();
        entrada.close();
        cliente.close();
    }

    public void enviarDatos(String mensaje) throws IOException {
        salida.writeObject("SERVIDOR: " + mensaje);
    }
}

public class Servidor {

    private ServerSocket servidor;
    private Socket socket1, socket2;
    private ConexionCliente cliente1, cliente2;

    public void ejecutarServidor() {
        try {
            servidor = new ServerSocket(11000);
            while (true) {
                try {
                    esperarConexion(socket1);
                    cliente1 = new ConexionCliente(socket1);
                    cliente1.enviarDatos("Esperando segundo jugador...");
                    esperarConexion(socket2);
                    cliente2 = new ConexionCliente(socket2);
                    procesarConexiones();
                } catch (EOFException excepcionEOF) {
                    System.err.println("El servidor terminó la conexión.");
                } finally {
                    cerrarConexiones();
                }
            }
        } catch (IOException excepcionES) {
            excepcionES.printStackTrace();
        }
    }

    private void esperarConexion(Socket c) throws IOException {
        c = servidor.accept();
    }

    private void procesarConexiones() {

    }

    private void cerrarConexiones() {
        try {
            cliente1.cerrarConexion();
            cliente2.cerrarConexion();
            socket1.close();
            socket2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
