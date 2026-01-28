import os
import numpy as np
import librosa
import warnings

warnings.filterwarnings('ignore')


class AnalysisService:
    """
    Servicio para analizar archivos de audio y extraer BPM y tonalidad (Camelot).
    Sin BeatNet / Torch.
    """

    def __init__(self):
        print("[AnalysisService] Inicializado (librosa-only)")
        self.camelot_map = self._build_camelot_map()

    def _build_camelot_map(self):
        return {
            'B Maj': '1B', 'F# Maj': '2B', 'C# Maj': '3B', 'G# Maj': '4B',
            'D# Maj': '5B', 'A# Maj': '6B', 'F Maj': '7B', 'C Maj': '8B',
            'G Maj': '9B', 'D Maj': '10B', 'A Maj': '11B', 'E Maj': '12B',
            'G# Min': '1A', 'D# Min': '2A', 'A# Min': '3A', 'F Min': '4A',
            'C Min': '5A', 'G Min': '6A', 'D Min': '7A', 'A Min': '8A',
            'E Min': '9A', 'B Min': '10A', 'F# Min': '11A', 'C# Min': '12A'
        }

    def analyze(self, file_path):
        if not os.path.exists(file_path):
            print(f"[AnalysisService] Archivo no existe: {file_path}")
            return None

        print(f"[AnalysisService] Analizando: {file_path}")

        try:
            bpm = self._get_bpm(file_path)
            key = self._get_key(file_path)

            print(f"[AnalysisService] Resultado: BPM={bpm}, Key={key}")

            return {
                "bpm": bpm,
                "camelotKey": key
            }

        except Exception as e:
            print(f"[AnalysisService] Error: {e}")
            return None

    def _get_bpm(self, file_path):
        """
        Extrae BPM usando librosa.
        """
        try:
            y, sr = librosa.load(file_path, mono=True)
            tempo, _ = librosa.beat.beat_track(y=y, sr=sr)
            return int(round(float(tempo)))
        except Exception as e:
            print(f"[AnalysisService] Error BPM: {e}")
            return 0

    def _get_key(self, file_path):
        """
        Extrae tonalidad usando Librosa y convierte a Camelot.
        """
        try:
            y, sr = librosa.load(file_path, offset=30, duration=60)
            y_harmonic = librosa.effects.hpss(y)[0]

            chroma = librosa.feature.chroma_cqt(
                y=y_harmonic,
                sr=sr,
                fmin=librosa.note_to_hz('C2'),
                n_octaves=4
            )

            chroma_mean = np.mean(chroma, axis=1)

            major_profile = np.array([5.0, 2.0, 3.5, 2.0, 4.5, 4.0, 2.0, 4.5, 2.0, 3.5, 1.5, 4.0])
            minor_profile = np.array([5.0, 2.0, 3.5, 4.5, 2.0, 4.0, 2.0, 4.5, 3.5, 2.0, 1.5, 4.0])
            notes = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']

            scores = {}
            for i in range(12):
                scores[f"{i}_Maj"] = np.corrcoef(chroma_mean, np.roll(major_profile, i))[0, 1]
                scores[f"{i}_Min"] = np.corrcoef(chroma_mean, np.roll(minor_profile, i))[0, 1]

            best_key_code = max(scores, key=scores.get)
            best_idx, best_mode = best_key_code.split('_')
            best_idx = int(best_idx)

            final_key = f"{notes[best_idx]} {best_mode}"
            return self._to_camelot(final_key)

        except Exception as e:
            print(f"[AnalysisService] Error Key: {e}")
            return "?"

    def _to_camelot(self, key_str):
        key_str = (key_str.replace("Db", "C#")
                   .replace("Eb", "D#")
                   .replace("Gb", "F#")
                   .replace("Ab", "G#")
                   .replace("Bb", "A#"))

        return self.camelot_map.get(key_str, "?")
