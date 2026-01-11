package TS3Bot.audio;

import TS3Bot.model.Track;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;

import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TrackScheduler implements Microphone {
    private final CustomOpusEncoder encoder;
    private final ConcurrentLinkedQueue<byte[]> packetQueue;

    // Ahora la cola es de objetos Track
    private final LinkedList<Track> songQueue = new LinkedList<>();

    private volatile Track currentTrack = null;
    private Thread audioThread;
    private volatile boolean playing = false;
    private volatile boolean stopSignal = false;
    private volatile float volume = 1.0f;

    private static final int MAX_BUFFER_SIZE = 150; // ~3 segundos de buffer

    public TrackScheduler() {
        this.encoder = new CustomOpusEncoder();
        this.packetQueue = new ConcurrentLinkedQueue<>();
    }

    public void queue(Track track) {
        synchronized (songQueue) {
            songQueue.add(track);
            if (currentTrack == null && !playing) {
                next();
            }
        }
    }

    public void next() {
        stopCurrentStream();
        synchronized (songQueue) {
            if (!songQueue.isEmpty()) {
                currentTrack = songQueue.poll();
                startStreaming(currentTrack);
            } else {
                currentTrack = null;
            }
        }
    }

    private void startStreaming(Track track) {
        stopSignal = false;
        playing = true;

        audioThread = new Thread(() -> {
            Process ffmpeg = null;
            try {
                System.out.println("Reproduciendo: " + track);

                // COMANDO FFMPEG PARA DECODIFICAR EN VIVO
                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", track.getPath(),     // Archivo comprimido
                        "-f", "s16le",             // Salida: PCM Raw 16-bit
                        "-ac", "2",                // Stereo
                        "-ar", "48000",            // 48kHz
                        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11", // Normalización en vivo
                        "-loglevel", "quiet",      // Silencio en consola
                        "pipe:1"                   // Salida a StdOut (Java)
                );

                ffmpeg = pb.start();
                InputStream stream = ffmpeg.getInputStream();

                byte[] buffer = new byte[3840]; // 20ms de audio
                int bytesRead;

                while (playing && !stopSignal && (bytesRead = stream.read(buffer)) != -1) {
                    short[] pcm = bytesToShorts(buffer);

                    // Aplicar volumen digitalmente
                    if (volume != 1.0f) {
                        for (int i = 0; i < pcm.length; i++) {
                            int val = (int) (pcm[i] * volume);
                            if (val > 32767) val = 32767; else if (val < -32768) val = -32768;
                            pcm[i] = (short) val;
                        }
                    }

                    packetQueue.add(encoder.encode(pcm));

                    // Control de flujo: Si tenemos mucho audio listo, esperamos para no saturar RAM
                    while (packetQueue.size() >= MAX_BUFFER_SIZE && playing) {
                        Thread.sleep(5);
                    }
                }

            } catch (Exception e) {
                System.err.println("Error streaming: " + e.getMessage());
            } finally {
                if (ffmpeg != null) ffmpeg.destroyForcibly(); // Matamos ffmpeg al terminar
                if (!stopSignal && playing) {
                    next(); // Pasamos a la siguiente automáticamente
                }
            }
        });
        audioThread.start();
    }

    // --- UTILS ---
    private short[] bytesToShorts(byte[] buffer) {
        short[] shorts = new short[buffer.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            int byte1 = buffer[i * 2] & 0xFF;
            int byte2 = buffer[i * 2 + 1] & 0xFF;
            shorts[i] = (short) ((byte2 << 8) | byte1);
        }
        return shorts;
    }

    public void stopCurrentStream() {
        stopSignal = true;
        playing = false;
        try { if (audioThread != null) audioThread.join(2000); } catch(Exception ignored){}
        packetQueue.clear();
    }

    public void shutdown() {
        stopCurrentStream();
        synchronized(songQueue) { songQueue.clear(); }
        currentTrack = null;
    }

    public String getCurrentSongName() { return currentTrack != null ? currentTrack.getTitle() : "Nada"; }

    public String getQueueDetails() {
        synchronized(songQueue) {
            if (songQueue.isEmpty()) return "Cola vacía.";
            StringBuilder sb = new StringBuilder();
            int i = 1;
            for(Track t : songQueue) {
                sb.append(i++).append(". ").append(t.getTitle()).append("\n");
                if(i > 10) { sb.append("... y más."); break; }
            }
            return sb.toString();
        }
    }

    // Microphone Interface
    public boolean isReady() { return !packetQueue.isEmpty() || playing; }
    public byte[] provide() { byte[] d = packetQueue.poll(); return d != null ? d : new byte[0]; }
    public CodecType getCodec() { return CodecType.OPUS_MUSIC; }
    public void setVolume(int v) { this.volume = v / 100.0f; }
    public void shuffle() { synchronized(songQueue){ Collections.shuffle(songQueue); } }
}