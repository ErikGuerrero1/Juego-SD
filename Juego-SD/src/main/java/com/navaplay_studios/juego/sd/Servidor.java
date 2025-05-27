/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.navaplay_studios.juego.sd;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * @author ferga
 */
class ConexionCliente extends Thread {

    private ObjectOutputStream salida;
    private ObjectInputStream entrada;
    private Socket cliente;
    private String nombreJugador;
    private Servidor servidor;
    private boolean activo = true;

    public ConexionCliente(Socket cliente, String nombre, Servidor servidor) throws IOException {
        this.cliente = cliente;
        this.nombreJugador = nombre;
        this.servidor = servidor;
        this.salida = new ObjectOutputStream(this.cliente.getOutputStream());
        this.entrada = new ObjectInputStream(this.cliente.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (activo) {
                Object mensaje = entrada.readObject();
                servidor.procesarMensaje(mensaje.toString(), this);
            }
        } catch (IOException | ClassNotFoundException e) {
            servidor.notificarEvento("Cliente " + nombreJugador + " desconectado");
        } finally {
            try {
                cerrarConexion();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cerrarConexion() throws IOException {
        activo = false;
        if (salida != null) {
            salida.close();
        }
        if (entrada != null) {
            entrada.close();
        }
        if (cliente != null) {
            cliente.close();
        }
    }

    public void enviarDatos(String mensaje) throws IOException {
        if (salida != null && !cliente.isClosed()) {
            salida.writeObject(mensaje);
            salida.flush();
        }
    }

    public String getNombreJugador() {
        return nombreJugador;
    }
}

public class Servidor {

    private ServerSocket servidor;
    private Socket socket1, socket2;
    private ConexionCliente cliente1, cliente2;
    private Consumer<String> callbackMensaje;
    private String jugada1 = null, jugada2 = null;
    private boolean juegoActivo = false;
    private boolean servidorActivo = false;
    private boolean esperandoJugadas = false; // Nueva bandera para controlar el estado

    // Variables para el sistema de 3 rondas
    private int rondaActual = 0;
    private int victoriasJugador1 = 0;
    private int victoriasJugador2 = 0;
    private static final int RONDAS_TOTALES = 3;

    public Servidor(Consumer<String> callbackMensaje) {
        this.callbackMensaje = callbackMensaje;
    }

    public void iniciarServidor() {
        new Thread(this::ejecutarServidor).start();
    }

    private void ejecutarServidor() {
        try {
            servidor = new ServerSocket(3000);
            servidorActivo = true;
            notificarEvento("Servidor iniciado en puerto 3000");
            notificarEvento("Esperando conexiones...");

            // Esperar primer cliente
            socket1 = servidor.accept();
            if (!servidorActivo) {
                return;
            }
            cliente1 = new ConexionCliente(socket1, "Jugador 1", this);
            cliente1.start();
            cliente1.enviarDatos("Conectado como Jugador 1. Esperando segundo jugador...");
            notificarEvento("Jugador 1 conectado");

            // Esperar segundo cliente
            socket2 = servidor.accept();
            if (!servidorActivo) {
                return;
            }
            cliente2 = new ConexionCliente(socket2, "Jugador 2", this);
            cliente2.start();
            cliente2.enviarDatos("Conectado como Jugador 2. ¡El juego puede comenzar!");
            cliente1.enviarDatos("Jugador 2 conectado. ¡El juego puede comenzar!");

            notificarEvento("Jugador 2 conectado");
            notificarEvento("¡Juego iniciado! Se jugarán 3 rondas");
            juegoActivo = true;

            iniciarNuevoJuego();

        } catch (IOException e) {
            if (servidorActivo) {
                notificarEvento("Error en el servidor: " + e.getMessage());
            }
        }
    }

    private void iniciarNuevoJuego() {
        rondaActual = 0;
        victoriasJugador1 = 0;
        victoriasJugador2 = 0;
        iniciarRonda();
    }

    public synchronized void procesarMensaje(String mensaje, ConexionCliente remitente) {
        if (!juegoActivo || !esperandoJugadas) {
            // Si no estamos esperando jugadas, ignorar el mensaje
            notificarEvento("Jugada ignorada de " + remitente.getNombreJugador() + " (fuera de tiempo): " + mensaje);
            return;
        }

        notificarEvento(remitente.getNombreJugador() + " jugó: " + mensaje);

        // Asignar la jugada al jugador correspondiente
        if (remitente == cliente1 && jugada1 == null) {
            jugada1 = mensaje;
        } else if (remitente == cliente2 && jugada2 == null) {
            jugada2 = mensaje;
        } else {
            // Jugada duplicada o de jugador incorrecto
            notificarEvento("Jugada duplicada ignorada de " + remitente.getNombreJugador());
            return;
        }

        // Verificar si ambos jugadores han jugado
        if (jugada1 != null && jugada2 != null) {
            esperandoJugadas = false; // Detener la recepción de más jugadas
            evaluarRonda();
        }
    }

    private void evaluarRonda() {
        rondaActual++;
        String resultado = determinarGanador(jugada1, jugada2);

        // Formatear el resultado según el requerimiento
        String jugadaFormateada1 = formatearJugada(jugada1);
        String jugadaFormateada2 = formatearJugada(jugada2);
        String mensajeRonda = "";

        if (resultado.equals("¡EMPATE!")) {
            mensajeRonda =jugadaFormateada1 + " vs " + jugadaFormateada2 + " - EMPATE";
        } else if (resultado.equals("¡GANA JUGADOR 1!")) {
            victoriasJugador1++;
            mensajeRonda = jugadaFormateada1 + "-" + jugadaFormateada2 + " " + getReglaMensaje(jugada1, jugada2) + " gana el jugador1";
        } else if (resultado.equals("¡GANA JUGADOR 2!")) {
            victoriasJugador2++;
            mensajeRonda = jugadaFormateada1 + "-" + jugadaFormateada2 + " " + getReglaMensaje(jugada2, jugada1) + " gana el jugador2";
        }

        try {
            // Enviar resultado de la ronda a ambos clientes
            cliente1.enviarDatos("RESULTADO: " + mensajeRonda);
            cliente2.enviarDatos("RESULTADO: " + mensajeRonda);

            notificarEvento(mensajeRonda);
            notificarEvento("Marcador: Jugador1(" + victoriasJugador1 + ") - Jugador2(" + victoriasJugador2 + ")");

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Verificar si el juego ha terminado (3 rondas completadas)
        if (rondaActual >= RONDAS_TOTALES) {
            determinarGanadorFinal();
        } else {
            // Continuar con la siguiente ronda
            prepararSiguienteRonda();
        }
    }

    private void determinarGanadorFinal() {
        String ganadorFinal;
        if (victoriasJugador1 > victoriasJugador2) {
            ganadorFinal = "¡GANADOR DEL JUEGO: JUGADOR 1! (" + victoriasJugador1 + "-" + victoriasJugador2 + ")";
        } else if (victoriasJugador2 > victoriasJugador1) {
            ganadorFinal = "¡GANADOR DEL JUEGO: JUGADOR 2! (" + victoriasJugador2 + "-" + victoriasJugador1 + ")";
        } else {
            ganadorFinal = "¡EL JUEGO TERMINA EN EMPATE! (" + victoriasJugador1 + "-" + victoriasJugador2 + ")";
        }

        try {
            cliente1.enviarDatos("JUEGO_TERMINADO: " + ganadorFinal);
            cliente2.enviarDatos("JUEGO_TERMINADO: " + ganadorFinal);
            notificarEvento(ganadorFinal);
            notificarEvento("¿Desean jugar otra partida? Reiniciando en 5 segundos...");
            cliente1.enviarDatos("¿Desean jugar otra partida? Reiniciando en 5 segundos...");
            cliente2.enviarDatos("¿Desean jugar otra partida? Reiniciando en 5 segundos...");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Reiniciar el juego después de 5 segundos
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (juegoActivo) {
                    iniciarNuevoJuego();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void prepararSiguienteRonda() {
        // Enviar mensaje para nueva ronda después de 2 segundos
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                if (juegoActivo && rondaActual < RONDAS_TOTALES) {
                    iniciarRonda();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String getReglaMensaje(String jugadaGanadora, String jugadaPerdedora) {
        switch (jugadaGanadora) {
            case "R":
                return " Roca rompe Tijeras";
            case "P":
                return " Papel cubre Roca";
            case "T":
                return " Tijeras cortan Papel";
            default:
                return "Jugada inválida";
        }
    }

    private String formatearJugada(String jugada) {
        switch (jugada) {
            case "R":
                return "R";
            case "P":
                return "P";
            case "T":
                return "T";
            default:
                return jugada;
        }
    }

    private String determinarGanador(String jugada1, String jugada2) {
        if (jugada1.equals(jugada2)) {
            return "¡EMPATE!";
        }

        switch (jugada1) {
            case "R":
                return jugada2.equals("T") ? "¡GANA JUGADOR 1!" : "¡GANA JUGADOR 2!";
            case "P":
                return jugada2.equals("R") ? "¡GANA JUGADOR 1!" : "¡GANA JUGADOR 2!";
            case "T":
                return jugada2.equals("P") ? "¡GANA JUGADOR 1!" : "¡GANA JUGADOR 2!";
            default:
                return "Jugada inválida";
        }
    }

    private void iniciarRonda() {
        // Reiniciar las jugadas y habilitar la recepción
        jugada1 = null;
        jugada2 = null;
        esperandoJugadas = true; // Habilitar la recepción de jugadas
        
        try {
            String mensajeRonda = "NUEVA_RONDA: Ronda " + (rondaActual + 1) + " de " + RONDAS_TOTALES + " - ¡Haz tu jugada!";
            cliente1.enviarDatos(mensajeRonda);
            cliente2.enviarDatos(mensajeRonda);
            notificarEvento("Iniciando ronda " + (rondaActual + 1) + " de " + RONDAS_TOTALES);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void notificarEvento(String mensaje) {
        if (callbackMensaje != null) {
            callbackMensaje.accept(mensaje);
        }
    }

    public void cerrarServidor() {
        servidorActivo = false;
        juegoActivo = false;
        esperandoJugadas = false;
        try {
            if (cliente1 != null) {
                cliente1.cerrarConexion();
            }
            if (cliente2 != null) {
                cliente2.cerrarConexion();
            }
            if (socket1 != null) {
                socket1.close();
            }
            if (socket2 != null) {
                socket2.close();
            }
            if (servidor != null) {
                servidor.close();
            }
            notificarEvento("Servidor cerrado");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean estaActivo() {
        return servidorActivo;
    }
}