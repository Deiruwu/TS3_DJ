package TS3Bot.audio.validation;

import TS3Bot.audio.MetadataClient;
import TS3Bot.model.Track;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Validador automático que detecta campos null/0 por reflexión.
 */
public class AutoTrackValidator {

    // Define qué campos son obligatorios y cómo repararlos
    private static final Map<String, FieldRepair> REPAIRS = new HashMap<>();

    static {
        REPAIRS.put("thumbnail", (track, meta) -> track.setThumbnail(meta.getThumbnail()));
        REPAIRS.put("duration", (track, meta) -> track.setDuration(meta.getDuration()));
        REPAIRS.put("album", (track, meta) -> track.setAlbum(meta.getAlbum()));
    }

    /**
     * Valida TODOS los campos automáticamente y repara los que estén null/0.
     * @return true si se reparó algo
     */
    public boolean validateAndRepair(Track track) throws Exception {
        boolean needsRepair = false;

        // 1. DETECTAR qué está null/0 usando reflection
        for (Map.Entry<String, FieldRepair> entry : REPAIRS.entrySet()) {
            String fieldName = entry.getKey();

            if (isFieldInvalid(track, fieldName)) {
                needsRepair = true;
                System.out.println("[Validator] Campo inválido detectado: " + fieldName);
            }
        }

        // 2. Si hay algo malo, traer metadata fresca y reparar todo
        if (needsRepair) {
            Track metadata = MetadataClient.getMetadata(track.getUuid());

            for (Map.Entry<String, FieldRepair> entry : REPAIRS.entrySet()) {
                String fieldName = entry.getKey();

                if (isFieldInvalid(track, fieldName)) {
                    try {
                        entry.getValue().repair(track, metadata);
                        System.out.println("[Validator]Reparado: " + fieldName);
                    } catch (Exception e) {
                        System.err.println("[Validator]Error reparando " + fieldName + ": " + e.getMessage());
                    }
                }
            }
        }

        return needsRepair;
    }

    /**
     * Verifica si un campo está null, vacío o 0 usando reflection.
     */
    private boolean isFieldInvalid(Track track, String fieldName) {
        try {
            // Buscar el getter (getFieldName o isFieldName)
            String getterName = "get" + capitalize(fieldName);
            Method getter = Track.class.getMethod(getterName);
            Object value = getter.invoke(track);

            // Verificar según el tipo
            if (value == null) return true;
            if (value instanceof String && ((String) value).isEmpty()) return true;
            if (value instanceof Number && ((Number) value).intValue() == 0) return true;

            return false;
        } catch (Exception e) {
            System.err.println("[Validator] Error leyendo campo " + fieldName + ": " + e.getMessage());
            return false;
        }
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Interfaz funcional para definir cómo reparar cada campo.
     */
    @FunctionalInterface
    interface FieldRepair {
        void repair(Track track, Track metadata) throws Exception;
    }
}