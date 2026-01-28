package TS3Bot.utils;

import TS3Bot.model.Track;

public class HarmonicUtils {

    /**
     * Calcula una puntuación de compatibilidad entre dos tracks (0 a 100).
     * Más alto = Mejor mezcla.
     */
    public static int getCompatibilityScore(Track current, Track candidate) {
        if (!current.isAnalyzed() || !candidate.isAnalyzed()) return 0;

        int score = 0;

        // 1. PUNTUACIÓN POR BPM (Máx 40 puntos)
        double bpmDiff = Math.abs(current.getBpm() - candidate.getBpm());
        double bpmPercent = bpmDiff / current.getBpm();

        if (bpmPercent <= 0.05) score += 40;      // Diferencia menor al 5% (Excelente)
        else if (bpmPercent <= 0.10) score += 20; // Diferencia menor al 10% (Aceptable)
        else if (bpmPercent <= 0.15) score += 5;  // Diferencia menor al 15% (Meh)
        // Si es más del 15%, 0 puntos.

        // 2. PUNTUACIÓN POR KEY (Máx 60 puntos)
        score += getKeyCompatibility(current.getCamelotKey(), candidate.getCamelotKey());

        return score;
    }

    private static int getKeyCompatibility(String key1, String key2) {
        if (key1 == null || key2 == null) return 0;
        if (key1.equals(key2)) return 60; // Misma nota: Perfecto

        try {
            // Parsear "11B" -> Numero: 11, Letra: B
            Key k1 = parseKey(key1);
            Key k2 = parseKey(key2);

            // Regla 1: Mismo número, diferente letra (Ej: 11B -> 11A)
            if (k1.number == k2.number && k1.letter != k2.letter) return 50;

            // Regla 2: Misma letra, número adyacente (Ej: 11B -> 10B o 12B)
            // (Manejo del ciclo 12 -> 1 y 1 -> 12)
            boolean isAdjacent = Math.abs(k1.number - k2.number) == 1 ||
                    Math.abs(k1.number - k2.number) == 11;

            if (k1.letter == k2.letter && isAdjacent) return 40;

            // Regla 3: Energy Boost (Diagonal) - Ej: 8A -> 9B (Opcional, da subidón)
            if (k1.letter != k2.letter && isAdjacent) return 20;

        } catch (Exception e) {
            return 0; // Si el formato es raro (ej: "Gm"), ignoramos
        }

        return 0; // No compatible
    }

    private static Key parseKey(String key) {
        // Asume formato "11B", "4A", etc.
        char letter = key.charAt(key.length() - 1); // Último caracter (A o B)
        int number = Integer.parseInt(key.substring(0, key.length() - 1)); // El resto es número
        return new Key(number, letter);
    }

    private static class Key {
        int number;
        char letter;
        Key(int n, char l) { number = n; letter = l; }
    }
}