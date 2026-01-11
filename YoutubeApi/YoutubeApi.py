import socket
import json
import re
import pprint
import os
import time
from dotenv import load_dotenv
from ytmusicapi import YTMusic
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials

load_dotenv()

SPOTIFY_CLIENT_ID = os.getenv('SPOTIFY_CLIENT_ID')
SPOTIFY_CLIENT_SECRET = os.getenv('SPOTIFY_CLIENT_SECRET')


def get_spotify_service():
    if not SPOTIFY_CLIENT_ID or not SPOTIFY_CLIENT_SECRET:
        return None
    auth_manager = SpotifyClientCredentials(client_id=SPOTIFY_CLIENT_ID, client_secret=SPOTIFY_CLIENT_SECRET)
    return spotipy.Spotify(auth_manager=auth_manager)


def extract_id(url_or_text):
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

            unique_id = None
            search_query = clean_query

            if "open.spotify.com" in clean_query and sp:
                for _ in range(3):
                    try:
                        track_info = sp.track(clean_query)
                        artist = track_info['artists'][0]['name']
                        title = track_info['name']
                        search_query = f"{artist} - {title} Audio"
                        print(f"[Spotify API] Traducido a: '{search_query}'")
                        break
                    except Exception as e:
                        time.sleep(1)

            elif "youtube" in clean_query or "youtu.be" in clean_query:
                unique_id = extract_id(clean_query)

            res = {}

            if unique_id:
                try:
                    track = ytm.get_song(unique_id)
                    vid = track.get('videoDetails') or track.get('details')

                    thumbnails = vid.get('thumbnail', {}).get('thumbnails') or track.get('thumbnails') or []
                    thumb_url = thumbnails[-1]['url'] if thumbnails else ""

                    res = {
                        "status": "ok",
                        "id": vid['videoId'],
                        "title": vid['title'],
                        "artist": vid['author'],
                        "duration": vid['lengthSeconds'],
                        "album": "YouTube Video",
                        "thumbnail": thumb_url
                    }
                except:
                    unique_id = None

            if not unique_id:
                results = ytm.search(search_query, filter="songs", limit=1)

                if results:
                    track = results[0]
                    res = {
                        "status": "ok",
                        "id": track['videoId'],
                        "title": track['title'],
                        "artist": track['artists'][0]['name'],
                        "duration": track.get('duration', '??:??'),
                        "album": track.get('album', {}).get('name', "Single"),
                        "thumbnail": track['thumbnails'][-1]['url'] if 'thumbnails' in track else ""
                    }
                else:
                    res = {"status": "error", "message": "No results found"}

            pprint.pprint(res)
            conn.sendall((json.dumps(res) + "\n").encode('utf-8'))

        except Exception as e:
            error_res = {"status": "error", "message": str(e)}
            conn.sendall((json.dumps(error_res) + "\n").encode('utf-8'))
        finally:
            conn.close()


if __name__ == "__main__":
    start_yt_service()