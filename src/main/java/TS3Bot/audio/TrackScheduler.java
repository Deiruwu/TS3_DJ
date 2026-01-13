package TS3Bot.audio;

import TS3Bot.model.QueuedTrack;
import TS3Bot.model.Track;
import com.github.manevolent.ts3j.audio.Microphone;
import com.github.manevolent.ts3j.command.CommandException;
import com.github.manevolent.ts3j.enums.CodecType;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class TrackScheduler implements Microphone {

    public interface TrackStartListener {
        void onTrackStart(String userUid, String userName, String trackUuid);
    }

    private final CustomOpusEncoder encoder;
    private final ConcurrentLinkedQueue<byte[]> packetQueue;
    private final LinkedList<QueuedTrack> songQueue = new LinkedList<>();

    private volatile QueuedTrack currentTrack = null;
    private Thread audioThread;
    private volatile boolean playing = false;
    private volatile boolean stopSignal = false;
    private volatile float volume = 1.0f;

    private TrackStartListener trackStartListener;

    private static final int MAX_BUFFER_SIZE = 150;

    public TrackScheduler() {
        this.encoder = new CustomOpusEncoder();
        this.packetQueue = new ConcurrentLinkedQueue<>();
    }

    public void setTrackStartListener(TrackStartListener listener) {
        this.trackStartListener = listener;
    }

    public void queue(QueuedTrack queuedTrack) {
        synchronized (songQueue) {
            songQueue.add(queuedTrack);
            if (currentTrack == null && !playing) {
                next();
            }
        }
    }

    public void queueNext(QueuedTrack queuedTrack) {
        synchronized (songQueue) {
            songQueue.addFirst(queuedTrack);
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

    private void startStreaming(QueuedTrack queuedTrack) {
        stopSignal = false;
        playing = true;

        Track track = queuedTrack.getTrack();

        if (trackStartListener != null) {
            trackStartListener.onTrackStart(
                    queuedTrack.getRequestedByUid(),
                    queuedTrack.getRequestedByName(),
                    track.getUuid()
            );
        }

        audioThread = new Thread(() -> {
            Process ffmpeg = null;
            try {
                System.out.println("Reproduciendo: " + track);

                ProcessBuilder pb = new ProcessBuilder(
                        "ffmpeg",
                        "-i", track.getPath(),
                        "-f", "s16le",
                        "-ac", "2",
                        "-ar", "48000",
                        "-af", "loudnorm=I=-16:TP=-1.5:LRA=11",
                        "-loglevel", "quiet",
                        "pipe:1"
                );

                ffmpeg = pb.start();
                InputStream stream = ffmpeg.getInputStream();

                byte[] buffer = new byte[3840];
                int bytesRead;

                while (playing && !stopSignal && (bytesRead = stream.read(buffer)) != -1) {
                    short[] pcm = bytesToShorts(buffer);

                    if (volume != 1.0f) {
                        for (int i = 0; i < pcm.length; i++) {
                            int val = (int) (pcm[i] * volume);
                            if (val > 32767) val = 32767; else if (val < -32768) val = -32768;
                            pcm[i] = (short) val;
                        }
                    }

                    packetQueue.add(encoder.encode(pcm));

                    while (packetQueue.size() >= MAX_BUFFER_SIZE && playing) {
                        Thread.sleep(5);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error streaming: " + e.getMessage());
            } finally {
                if (ffmpeg != null) ffmpeg.destroyForcibly();
                if (!stopSignal && playing) {
                    next();
                }
            }
        });
        audioThread.start();
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

    public String getCurrentSongName() {
        return currentTrack != null ? currentTrack.getTrack().toString() : "Sin música aún";
    }

    public QueuedTrack getCurrentTrack() {
        return currentTrack;
    }

    public List<String> getQueueList() {
        synchronized(songQueue) {
            List<String> lines = new ArrayList<>();
            if (songQueue.isEmpty()) {
                lines.add("Cola vacía.");
                return lines;
            }
            int i = 1;
            for(QueuedTrack qt : songQueue) {
                String line = qt.getTrack().getTitle() + "[color=purple] by [/color] " + qt.getTrack().getArtist();
                lines.add(line);
                if(i > 10) {
                    lines.add("... y " + (songQueue.size() - 10) + " más.");
                    break;
                }
            }
            return lines;
        }
    }

    public boolean removeFromQueue(int index) {
        synchronized(songQueue) {
            if (index >= 0 && index < songQueue.size()) {
                songQueue.remove(index);
                return true;
            }
            return false;
        }
    }

    public boolean isReady() { return !packetQueue.isEmpty() || playing; }
    public byte[] provide() { byte[] d = packetQueue.poll(); return d != null ? d : new byte[0]; }
    public CodecType getCodec() { return CodecType.OPUS_MUSIC; }
    public void setVolume(int v) { this.volume = v / 100.0f; }
    public void shuffle() { synchronized(songQueue){ Collections.shuffle(songQueue); } }
}