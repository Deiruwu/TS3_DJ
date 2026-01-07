package TS3Bot.audio;

import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FilePlayer implements Microphone {
    private final CustomOpusEncoder encoder;
    private final ConcurrentLinkedQueue<byte[]> packetQueue;
    private boolean playing = false;

    // NUEVO: Variable de volumen (Volatile para que cambie en tiempo real)
    private volatile float volume = 1.0f; // 1.0 = 100%

    public FilePlayer() {
        this.encoder = new CustomOpusEncoder();
        this.packetQueue = new ConcurrentLinkedQueue<>();
    }

    // NUEVO: Metodo para cambiar el volumen (0 a 100)
    public void setVolume(int percent) {
        // Aseguramos que esté entre 0 y 150 (para dar un poco de ganancia extra si hace falta)
        if (percent < 0) percent = 0;
        if (percent > 150) percent = 150;
        this.volume = percent / 100.0f;
    }

    public void playFile(File file) {
        new Thread(() -> {
            try {
                System.out.println(">>> Reproduciendo: " + file.getName());
                AudioInputStream ais = AudioSystem.getAudioInputStream(file);

                // 960 muestras * 2 canales * 2 bytes = 3840 bytes
                int frameBytes = 960 * 2 * 2;
                byte[] buffer = new byte[frameBytes];

                playing = true;

                while (playing && ais.read(buffer) != -1) {
                    short[] pcm = bytesToShorts(buffer);

                    // NUEVO: Aplicar volumen
                    if (volume != 1.0f) {
                        for (int i = 0; i < pcm.length; i++) {
                            // Multiplicamos por el factor de volumen
                            pcm[i] = (short) (pcm[i] * volume);
                        }
                    }

                    byte[] opusPacket = encoder.encode(pcm);
                    packetQueue.add(opusPacket);
                    Thread.sleep(19);
                }

                playing = false;
                ais.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private short[] bytesToShorts(byte[] buffer) {
        short[] shorts = new short[buffer.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            int byte1 = buffer[i * 2] & 0xFF;
            int byte2 = buffer[i * 2 + 1] & 0xFF;
            shorts[i] = (short) ((byte2 << 8) | byte1);
        }
        return shorts;
    }

    @Override
    public boolean isReady() { return playing && !packetQueue.isEmpty(); }

    @Override
    public byte[] provide() {
        byte[] data = packetQueue.poll();
        return data != null ? data : new byte[0];
    }

    public CodecType getCodec() { return CodecType.OPUS_MUSIC; }

    public void stop() { this.playing = false; packetQueue.clear(); }
}