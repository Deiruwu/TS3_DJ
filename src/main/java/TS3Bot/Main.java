package TS3Bot;

import TS3Bot.utils.LogUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;

public class Main {

    public static void main(String[] args) {

        LogUtils.install();
        System.out.println("Iniciando Bot...");

        try {
            // 1. Definir dónde está el archivo
            File archivoConfig = new File("config.json");

            // Verificación básica
            if (!archivoConfig.exists()) {
                System.err.println("ERROR: No encuentro el archivo config.json en la raíz del proyecto.");
                System.err.println("Ruta buscada: " + archivoConfig.getAbsolutePath());
                return;
            }

            System.out.println("Cargando configuración...");

            // 2. Usar GSON para leer el archivo y convertirlo en un objeto manipulable
            JsonObject configCompleta = JsonParser.parseReader(new FileReader(archivoConfig)).getAsJsonObject();

            // 3. INICIAR EL BOT
            // CAMBIO MÍNIMO: En lugar de separar aquí el JSON, se lo pasamos entero al Bot junto con el archivo.
            // Esto es obligatorio para que el Bot tenga permiso de escritura para guardar el VolumeCommand.
            TeamSpeakBot ts = new TeamSpeakBot(configCompleta, archivoConfig);

            // 5. Encendemos
            ts.start();

        } catch (Exception e) {
            System.err.println("Ocurrió un error al iniciar:");
            e.printStackTrace();
        }
    }
}