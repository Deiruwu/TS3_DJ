import time
from ytmusicapi import YTMusic
import spotipy
from spotipy.oauth2 import SpotifyClientCredentials
from utils.TimeFormatter import TimeFormatter
import re
import os
from dotenv import load_dotenv

load_dotenv()


class MetadataService:
    def __init__(self):
        self.ytm = YTMusic()
        self.sp = self._init_spotify()

    def _init_spotify(self):
        client_id = os.getenv('SPOTIFY_CLIENT_ID')
        client_secret = os.getenv('SPOTIFY_CLIENT_SECRET')

        if not client_id or not client_secret:
            print(f"[metadataService] No se pudieron encontrar las credenciales de Spotify")
            return None

        auth_manager = SpotifyClientCredentials(
            client_id=client_id,
            client_secret=client_secret
        )
        return spotipy.Spotify(auth_manager=auth_manager)

    def get_metadata(self, query, use_filter=True):
        print(f"[MetadataService] Query: '{query}' (filter={use_filter})")

        search_query = query
        video_id = None

        # Lógica de Spotify intacta como pediste
        if "open.spotify.com" in query and self.sp:
            search_query = self._resolve_spotify(query)
            if not search_query:
                return None

        # ID directo (11 caracteres)
        elif re.match(r'^[a-zA-Z0-9_-]{11}$', query.strip()):
            video_id = query.strip()

        if video_id:
            return self._get_by_id(video_id)
        else:
            return self._search(search_query, use_filter)

    def _resolve_spotify(self, spotify_url):
        for attempt in range(3):
            try:
                track_info = self.sp.track(spotify_url)
                artist = track_info['artists'][0]['name']
                title = track_info['name']
                query = f"{artist} - {title} Audio"
                print(f"[Spotify] Traducido a: '{query}'")
                return query
            except Exception as e:
                print(f"[Spotify] Intento {attempt + 1}/3 fallo: {e}")
                if attempt < 2:
                    time.sleep(1)
        return None

    def _get_by_id(self, video_id):
        try:
            track = self.ytm.get_song(video_id)
            vid = track.get('videoDetails') or track.get('details')

            thumbnails = vid.get('thumbnail', {}).get('thumbnails') or track.get('thumbnails') or []
            thumb_url = thumbnails[-1]['url'] if thumbnails else ""

            duration_seconds = TimeFormatter.to_seconds(vid['lengthSeconds'])

            return {
                "id": vid['videoId'],
                "title": vid['title'],
                "artist": vid['author'],
                "duration": duration_seconds,
                "album": track.get('album', {}).get('name', "Single"),
                "thumbnail": thumb_url
            }
        except Exception as e:
            print(f"[MetadataService] Error obteniendo por ID: {e}")
            return None

    def _search(self, query, use_filter):
        try:
            if use_filter:
                print("[MetadataService] Buscando con filtro 'songs'")
                results = self.ytm.search(query, filter="songs", limit=1)
            else:
                print("[MetadataService] Buscando sin filtro")
                results = self.ytm.search(query, limit=1)

            if not results:
                return None

            track = results[0]

            duration_str = track.get('duration', '0:00')
            duration_seconds = TimeFormatter.to_seconds(duration_str)

            return {
                "id": track['videoId'],
                "title": track['title'],
                "artist": track['artists'][0]['name'],
                "duration": duration_seconds,
                "album": track.get('album', {}).get('name', "Single"),
                "thumbnail": track['thumbnails'][-1]['url'] if 'thumbnails' in track else ""
            }

        except Exception as e:
            print(f"[MetadataService] Error en busqueda: {e}")
            return None