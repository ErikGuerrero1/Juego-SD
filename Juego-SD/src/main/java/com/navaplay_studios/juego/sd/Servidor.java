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
public class Servidor {

    private ServerSocket servidor;
    private ObjectOutputStream salida1, salida2;
    private ObjectInputStream entrada1, entrada2;
    private Socket cliente1, cliente2;

    public Servidor() {
    }

    public void ejecutarServidor() {
        try {
            servidor = new ServerSocket(11000);
            while (true) {
//                try {
//                    // esperarConexion();
//                    // obtenerFlujos();
//                    // procesarConexion
//                } catch (EOFException excepcionEOF) {
//                    System.err.println("El servidor terminó la conexión.");
//                } finally {
//                    // cerrarConexion();
//                }
            }
        } catch (IOException excepcionES) {
            excepcionES.printStackTrace();
        }
    }

    private void esperarConexion(int numCliente) throws IOException {
        if (numCliente == 1) {
            cliente1 = servidor.accept();
        } else if (numCliente == 2) {
            cliente2 = servidor.accept();
        }
    }

    private void obtenerFlujos(int numCliente) throws IOException {
        if (numCliente == 1) {
            salida1 = new ObjectOutputStream(cliente1.getOutputStream());
            entrada1 = new ObjectInputStream(cliente1.getInputStream());
        } else if (numCliente == 2) {
            salida2 = new ObjectOutputStream(cliente2.getOutputStream());
            entrada2 = new ObjectInputStream(cliente2.getInputStream());
        }
    }
}
