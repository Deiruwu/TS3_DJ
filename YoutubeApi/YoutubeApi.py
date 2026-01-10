import socket
import json
import re
import pprint
import os
from dotenv import load_dotenv
from ytmusicapi import YTMusic
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

load_dotenv()

# --- CONFIGURACIÓN SPOTIFY ---
SPOTIFY_CLIENT_ID = os.getenv('SPOTIFY_CLIENT_ID')
SPOTIFY_CLIENT_SECRET = os.getenv('SPOTIFY_CLIENT_SECRET')


def get_spotify_service():
    """Inicializa la conexión con Spotify si hay credenciales."""
    if not SPOTIFY_CLIENT_ID or not SPOTIFY_CLIENT_SECRET:
        print("[Error] Faltan las credenciales de Spotify.")
        return None

    auth_manager = SpotifyClientCredentials(client_id=SPOTIFY_CLIENT_ID, client_secret=SPOTIFY_CLIENT_SECRET)
    return spotipy.Spotify(auth_manager=auth_manager)


def extract_id(url_or_text):
    """Extrae ID de YouTube si es link."""
    patterns = [r'(?:v=|\/)([0-9A-Za-z_-]{11}).*']
    for pattern in patterns:
        match = re.search(pattern, url_or_text)
        if match: return match.group(1)
    return None


def start_yt_service():
    ytm = YTMusic()
    sp = get_spotify_service()

    ip = '127.0.0.1'
    puerto = 5005

    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

    server.bind((ip, puerto))
    server.listen(5)

    print("[Server] Escuchando peticiones del puerto:", puerto)

    while True:
        conn, addr = server.accept()
        try:
            raw_data = conn.recv(4096).decode('utf-8').strip()
            if not raw_data:
                conn.close()
                continue

            clean_query = re.sub(r'\[/??URL\]', '', raw_data, flags=re.IGNORECASE).strip()
            print(f"\n[Socket] Recibido: '{clean_query}'")

            # --- DEFINICIÓN DE VARIABLES GENÉRICAS ---
            # Aquí inicializamos 'unique_id' en None para evitar el crash anterior.
            unique_id = None
            search_query = clean_query

            # --- LÓGICA DE DETECCIÓN DE FUENTE ---

            # CASO 1: Spotify
            if "open.spotify.com" in clean_query and sp:
                print("[Spotify API] Link de Spotify detectado.")
                try:
                    track_info = sp.track(clean_query)
                    artist = track_info['artists'][0]['name']
                    title = track_info['name']
                    # Sobrescribimos la búsqueda de texto con los datos reales
                    search_query = f"{artist} - {title} Audio"
                    print(f"[Spotify API] Traducido a: '{search_query}'")
                except Exception as e:
                    print(f"[Spotify API] Error leyendo track: {e}")

            # CASO 2: YouTube Link Directo
            elif "youtube" in clean_query or "youtu.be" in clean_query:
                unique_id = extract_id(clean_query)

            # --- FASE DE OBTENCIÓN DE METADATOS (FETCH) ---
            res = {}

            # Intento 1: Si tenemos un ID único (unique_id), intentamos obtenerlo directo
            if unique_id:
                print(f"[YTMusic] ID detectado ({unique_id}). Intentando obtención directa...")
                try:
                    track = ytm.get_song(unique_id)
                    vid = track['details']  # Asegúrate que tu versión de ytmusicapi devuelve 'details'

                    res = {
                        "status": "ok",
                        "id": vid['videoId'],
                        "title": vid['title'],
                        "artist": vid['author'],
                        "duration": vid['lengthSeconds'],
                        "album": "YouTube Video",
                        # try/except interno por si la estructura de thumbnails varía
                        "thumbnail": vid['thumbnail']['thumbnails'][-1]['url']
                    }
                except Exception as e:
                    print(f"[YTMusic] Falló la búsqueda por ID ({e}). Cambiando a búsqueda por texto...")
                    unique_id = None  # IMPORTANTE: Forzamos el fallback

            # Intento 2: Búsqueda por texto (Search)
            # Entra aquí si no había ID al principio O si el ID falló arriba.
            if not unique_id:
                print(f"[YTMusic] Buscando: '{search_query}'")
                results = ytm.search(search_query, filter="songs", limit=1)

                if results:
                    track = results[0]
                    res = {
                        "status": "ok",
                        "id": track['videoId'],
                        "title": track['title'],
                        "artist": track['artists'][0]['name'],
                        "duration": track.get('duration', '??:??'),
                        "album": track['album']['name'] if 'album' in track and track['album'] else "Single",
                        "thumbnail": track['thumbnails'][-1]['url'] if 'thumbnails' in track else ""
                    }
                else:
                    res = {"status": "error", "message": "No results found"}

            # --- RESPUESTA ---
            print("\n\t [Metadatos a enviar]")
            pprint.pprint(res)
            print("\n")

            conn.sendall((json.dumps(res) + "\n").encode('utf-8'))

        except Exception as e:
            print(f"[Error General] {str(e)}")
            error_res = {"status": "error", "message": str(e)}
            conn.sendall((json.dumps(error_res) + "\n").encode('utf-8'))
        finally:
            conn.close()


if __name__ == "__main__":
    start_yt_service()