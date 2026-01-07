package TS3Bot.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class JsonHelper {

    // Constructor privado para evitar que alguien haga "new JsonHelper()"
    // ya que es una clase de utilidad estática.
    private JsonHelper() {}

    /**
     * Busca un valor dentro de un objeto JSON siguiendo una ruta separada por puntos.
     * Ejemplo: get(json, "bot.identity.file")
     */
    public static JsonElement get(JsonObject object, String path) {
        JsonElement currentElement = object;

        // Dividimos la ruta "bot.identity.file" en ["bot", "identity", "file"]
        for (String node : path.split("\\.")) {
            // Verificamos si el elemento actual es un objeto y si tiene la llave que buscamos
            if (!currentElement.isJsonObject() || !currentElement.getAsJsonObject().has(node)) {
                throw new IllegalArgumentException("No se encontró la ruta: " + path);
            }
            // Bajamos un nivel en el árbol
            currentElement = currentElement.getAsJsonObject().get(node);
        }

        return currentElement;
    }

    /**
     * Intenta buscar el valor en la configuración específica (del servidor).
     * Si no existe, lo busca en la configuración por defecto (default).
     */
    public static JsonElement get(JsonObject defaultConf, JsonObject serverConf, String path) {
        try {
            // 1. Intentamos buscar en la configuración específica del servidor
            return get(serverConf, path);
        } catch (IllegalArgumentException ex) {
            // 2. Si falla (no existe), buscamos en la configuración global por defecto
            return get(defaultConf, path);
        }
    }

    /**
     * Un atajo útil para obtener Strings directamente y evitar .getAsString() fuera
     */
    public static String getString(JsonObject defaultConf, JsonObject serverConf, String path) {
        return get(defaultConf, serverConf, path).getAsString();
    }

    /**
     * Un atajo útil para obtener Enteros (int) directamente
     */
    public static int getInt(JsonObject defaultConf, JsonObject serverConf, String path) {
        return get(defaultConf, serverConf, path).getAsInt();
    }
}