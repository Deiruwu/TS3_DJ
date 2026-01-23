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

    public boolean queue(QueuedTrack queuedTrack) {
        synchronized (songQueue) {
            songQueue.add(queuedTrack);
            if (currentTrack == null && !playing) {
                next();
                return true;
            }
            return false;
        }
    }

    public boolean queueNext(QueuedTrack queuedTrack) {
        synchronized (songQueue) {
            songQueue.addFirst(queuedTrack);
            if (currentTrack == null && !playing) {
                next();
                return true;
            }
            return false;
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
                return lines;
            }
            for(QueuedTrack qt : songQueue) {
                lines.add(qt.toString());
            }
            return lines;
        }
    }


    public boolean removeFromQueue(int index) {
        return removeFromQueue(index, index);
    }

    public boolean removeFromQueue(int from, int to) {
        synchronized (songQueue) {
            if (from < 0 || to < 0) return false;
            if (from >= songQueue.size()) return false;
            if (from > to) return false;

            if (to >= songQueue.size()) {
                to = songQueue.size() - 1;
            }

            songQueue.subList(from, to + 1).clear();
            return true;
        }
    }

    public List<QueuedTrack> getQueue() {
        return new ArrayList<>(songQueue);
    }

    public void clear() {
        synchronized (songQueue) {
            songQueue.clear();
        }
    }

    public void skipTo(int index) {
        synchronized (songQueue) {
            if (index < 0 || index >= songQueue.size()) {
                return;
            }

            if (index > 0) {
                songQueue.subList(0, index).clear();
            }

            next();
        }
    }

    public boolean isReady() { return !packetQueue.isEmpty() || playing; }
    public byte[] provide() { byte[] d = packetQueue.poll(); return d != null ? d : new byte[0]; }
    public CodecType getCodec() { return CodecType.OPUS_MUSIC; }
    public void setVolume(int v) { this.volume = v / 100.0f; }
    public void shuffle() { synchronized(songQueue){ Collections.shuffle(songQueue); } }
}