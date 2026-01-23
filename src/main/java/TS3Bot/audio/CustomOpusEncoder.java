package TS3Bot.audio;

import io.github.jaredmdobson.concentus.OpusApplication;
import io.github.jaredmdobson.concentus.OpusEncoder;
import io.github.jaredmdobson.concentus.OpusSignal;

import java.util.Arrays;

public class CustomOpusEncoder {
    private final OpusEncoder encoder;
    private final int frameSize; // Cuántos samples caben en 20ms
    private final byte[] buffer; // Buffer temporal

    public CustomOpusEncoder() {
        try {
            // 48000Hz, 2 Canales (Stereo)
            this.encoder = new OpusEncoder(48000, 2, OpusApplication.OPUS_APPLICATION_AUDIO);
            this.encoder.setBitrate(96000); // 96kbps
            this.encoder.setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
            this.encoder.setComplexity(5); // Balance entre calidad y CPU para la Pi

            // TS3 suele usar frames de 20ms.
            // 48000 muestras/segundo * 0.020 segundos = 960 muestras por frame
            this.frameSize = 960;

            // Buffer de salida (tamaño máximo teórico de un paquete opus)
            this.buffer = new byte[4096];

        } catch (Exception e) {
            throw new RuntimeException("No se pudo iniciar Opus Encoder", e);
        }
    }

    public byte[] encode(short[] pcmAudio) {
        try {
            // Encodea el audio PCM (short) a Opus (byte)
            int len = encoder.encode(pcmAudio, 0, frameSize, buffer, 0, buffer.length);

            // Devuelve solo los bytes útiles
            return Arrays.copyOf(buffer, len);
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }

    public int getFrameSize() {
        return frameSize;
    }
}