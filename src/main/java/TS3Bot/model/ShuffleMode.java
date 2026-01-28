package TS3Bot.model;

public enum ShuffleMode {
    CHAOS,       // Aleatorio puro (Collections.shuffle) - Para fiestas locas
    HARMONIC,    // Inteligente (BPM + Key) - Transiciones suaves
    RISING,      // De lento a rápido - Para subir la energía (Gym/Gaming)
    FALLING,     // De rápido a lento - Para relajarse (Chill)
    WAVE         // Sinusoidal (Rápido -> Lento -> Rápido) - Mantiene el interés
}