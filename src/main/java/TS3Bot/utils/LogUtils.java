package TS3Bot.utils;

import java.io.PrintStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogUtils {

    public static void install() {
        // Guardamos la referencia a la consola original para no perderla
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // Formato de hora: 14:30:05
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        // SECUESTRO DE SYSTEM.OUT (Texto normal / blanco)
        System.setOut(new PrintStream(originalOut) {
            @Override
            public void println(String x) {
                String time = LocalTime.now().format(formatter);
                // Imprimimos: [14:30:05] Mensaje
                super.println("[" + time + "] " + x);
            }

            @Override
            public void println(Object x) {
                println(String.valueOf(x));
            }

            // OJO: No sobreescribimos print() normal para no romper
            // la barra de progreso de descarga (\r)
        });

        // SECUESTRO DE SYSTEM.ERR (Texto de error / rojo)
        System.setErr(new PrintStream(originalErr) {
            @Override
            public void println(String x) {
                String time = LocalTime.now().format(formatter);
                super.println("[" + time + "] " + x);
            }

            @Override
            public void println(Object x) {
                println(String.valueOf(x));
            }
        });

        System.out.println("[LogUtlils] Sistema de Logs con Timestamp activado.");
    }
}