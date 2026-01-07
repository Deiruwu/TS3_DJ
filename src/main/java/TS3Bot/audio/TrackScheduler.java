package TS3Bot.audio;

import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.enums.CodecType;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

public class TrackScheduler implements Microphone {
    // Clase para guardar metadatos en la cola
    public static class TrackInfo {
        public final File file;
        public final String title;
        public TrackInfo(File file, String title) {
            this.file = file;
            this.title = title;
        }
    }

    private final CustomOpusEncoder encoder;
    private final ConcurrentLinkedQueue<byte[]> packetQueue;
    private final LinkedList<TrackInfo> songQueue = new LinkedList<>();

    private volatile TrackInfo currentTrack = null;
    private Thread audioThread;
    private volatile boolean playing = false;
    private volatile boolean stopSignal = false;
    private volatile float volume = 1.0f;
    private static final int MAX_BUFFER_SIZE = 150;

    public TrackScheduler() {
        this.encoder = new CustomOpusEncoder();
        this.packetQueue = new ConcurrentLinkedQueue<>();
    }

    // Ahora pedimos el título para mostrarlo en la lista
    public void queue(File file, String title) {
        synchronized (songQueue) {
            songQueue.add(new TrackInfo(file, title));
            if (currentTrack == null) {
                next();
            }
        }
    }

    public void shuffle() {
        synchronized (songQueue) {
            if (songQueue.size() > 1) {
                Collections.shuffle(songQueue);
                System.out.println("[Queue] Cola mezclada aleatoriamente.");
            }
        }
    }

    public void next() {
        stopCurrentStream();
        synchronized (songQueue) {
            if (!songQueue.isEmpty()) {
                currentTrack = songQueue.poll();
                startAudioThread(currentTrack.file, currentTrack.title);
            } else {
                currentTrack = null;
                System.out.println("[Queue] Fin de la playlist.");
            }
        }
    }

    // Devuelve un String detallado de la cola
    public String getQueueDetails() {
        synchronized (songQueue) {
            if (songQueue.isEmpty()) return "La cola está vacía.";

            StringBuilder sb = new StringBuilder();
            sb.append("--- Próximas canciones ---\n");
            int i = 1;
            for (TrackInfo track : songQueue) {
                sb.append(i).append(". ").append(track.title).append("\n");
                i++;
                if (i > 10) { // Límite para no saturar el chat de TS
                    sb.append("... y ").append(songQueue.size() - 10).append(" más.");
                    break;
                }
            }
            return sb.toString();
        }
    }

    private void startAudioThread(File file, String title) {
        stopSignal = false;
        playing = true;

        audioThread = new Thread(() -> {
            try {
                System.out.println(">>> Reproduciendo: " + title);

                AudioInputStream rawStream = AudioSystem.getAudioInputStream(file);
                AudioFormat targetFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,48000,16,2,4,48000,false);
                AudioInputStream ais = AudioSystem.getAudioInputStream(targetFormat, rawStream);

                byte[] buffer = new byte[3840];

                while (playing && !stopSignal) {
                    if (packetQueue.size() >= MAX_BUFFER_SIZE) {
                        try { Thread.sleep(10); } catch (InterruptedException e) { break; }
                        continue;
                    }

                    int bytesRead = ais.read(buffer);
                    if (bytesRead == -1) break;

                    short[] pcm = bytesToShorts(buffer);

                    if (volume != 1.0f) {
                        for (int i = 0; i < pcm.length; i++) {
                            int val = (int) (pcm[i] * volume);
                            if (val > 32767) val = 32767;
                            else if (val < -32768) val = -32768;
                            pcm[i] = (short) val;
                        }
                    }
                    packetQueue.add(encoder.encode(pcm));
                }

                ais.close();
                rawStream.close();

                if (!stopSignal && playing) {
                    Thread.sleep(1000);
                    next();
                }

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                next();
            }
        });
        audioThread.start();
    }

    // ... (Mantén bytesToShorts, stopCurrentStream, shutdown igual) ...

    public void setVolume(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 150) percent = 150;
        this.volume = percent / 100.0f;
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

    private void stopCurrentStream() {
        stopSignal = true;
        playing = false;
        try {
            if (audioThread != null && audioThread.isAlive()) {
                audioThread.join(2000);
            }
        } catch (InterruptedException ignored) {}
        packetQueue.clear();
    }

    public void shutdown() {
        stopSignal = true;
        playing = false;
        packetQueue.clear();
        synchronized (songQueue) { songQueue.clear(); }
        currentTrack = null;
        packetQueue.add(new byte[0]);
    }

    public String getCurrentSongName() {
        return currentTrack != null ? currentTrack.title : "Nada";
    }

    public int getQueueSize() {
        synchronized (songQueue) { return songQueue.size(); }
    }

    @Override public boolean isReady() { return !packetQueue.isEmpty() || playing; }
    @Override public byte[] provide() { byte[] data = packetQueue.poll(); return data != null ? data : new byte[0]; }
    @Override public CodecType getCodec() { return CodecType.OPUS_MUSIC; }
}