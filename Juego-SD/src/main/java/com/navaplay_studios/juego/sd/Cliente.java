/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.navaplay_studios.juego.sd;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * @author ferga
 */
public class Cliente extends Thread {

    private Socket socket;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private String direccionServidor;
    private Consumer<String> callbackMensaje;
    private Consumer<Boolean> callbackEstadoBotones;
    private boolean conectado = false;

    public Cliente(String direccion, Consumer<String> callbackMensaje, Consumer<Boolean> callbackBotones) {
        this.direccionServidor = direccion;
        this.callbackMensaje = callbackMensaje;
        this.callbackEstadoBotones = callbackBotones;
    }

    public boolean conectarServidor() {
        try {
            notificarMensaje("Intentando conectar con " + direccionServidor + ":3000...");
            socket = new Socket(direccionServidor, 3000);
            salida = new ObjectOutputStream(socket.getOutputStream());
            entrada = new ObjectInputStream(socket.getInputStream());
            conectado = true;
            notificarMensaje("¡Conectado al servidor!");
            this.start();
            return true;
        } catch (IOException e) {
            notificarMensaje("Error de conexión: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void run() {
        try {
            while (conectado) {
                Object mensajeRecibido = entrada.readObject();
                String mensaje = mensajeRecibido.toString();

                if (mensaje.startsWith("RESULTADO:")) {
                    notificarMensaje(mensaje.substring(10)); // Remover "RESULTADO:"
                } else if (mensaje.startsWith("NUEVA_RONDA:")) {
                    notificarMensaje(mensaje.substring(12)); // Remover "NUEVA_RONDA:"
                    habilitarBotones(true);
                } else if (mensaje.startsWith("JUEGO_TERMINADO:")) {
                    notificarMensaje("=== JUEGO TERMINADO ===");
                    notificarMensaje(mensaje.substring(16)); // Remover "JUEGO_TERMINADO:"
                    notificarMensaje("========================");
                    habilitarBotones(false); // Deshabilitar botones temporalmente
                } else {
                    notificarMensaje("Servidor: " + mensaje);
                    // Si es el mensaje de conexión de jugador 2, habilitar botones
                    if (mensaje.contains("¡El juego puede comenzar!")) {
                        habilitarBotones(true);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (conectado) {
                notificarMensaje("Conexión perdida con el servidor");
            }
        } finally {
            cerrarConexion();
        }
    }

    public void enviarJugada(String jugada) {
        if (conectado && salida != null) {
            try {
                salida.writeObject(jugada);
                salida.flush();
                notificarMensaje("Tu jugada: " + jugada);
                habilitarBotones(false); // Deshabilitar botones hasta nueva ronda
            } catch (IOException e) {
                notificarMensaje("Error al enviar jugada: " + e.getMessage());
            }
        } else {
            notificarMensaje("No conectado al servidor");
        }
    }

    public void cerrarConexion() {
        conectado = false;
        try {
            if (salida != null) {
                salida.close();
            }
            if (entrada != null) {
                entrada.close();
            }
            if (socket != null) {
                socket.close();
            }
            notificarMensaje("Desconectado del servidor");
        } catch (IOException e) {
            notificarMensaje("Error al cerrar conexión: " + e.getMessage());
        }
    }

    // Métodos para comunicarse con la GUI
    private void notificarMensaje(String mensaje) {
        if (callbackMensaje != null) {
            callbackMensaje.accept(mensaje);
        }
    }

    private void habilitarBotones(boolean habilitar) {
        if (callbackEstadoBotones != null) {
            callbackEstadoBotones.accept(habilitar);
        }
    }

    public boolean estaConectado() {
        return conectado;
    }
}
