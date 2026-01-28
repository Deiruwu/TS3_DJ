import socket
import json
from services.MetadataService import MetadataService
from services.AnalysisService import AnalysisService
from utils.FlagsResolver import FlagsResolver


class MetadataHub:
    """
    Hub central que orquesta servicios de metadata y analisis.
    Recibe peticiones JSON via socket y delega a servicios correspondientes.
    """

    def __init__(self, host='127.0.0.1', port=5005):
        self.host = host
        self.port = port
        self.metadata_service = MetadataService()
        self.analysis_service = None

    def start(self):
        """
        Inicia el servidor socket.
        """
        server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        server.bind((self.host, self.port))
        server.listen(5)

        print(f"[MetadataHub] Escuchando en {self.host}:{self.port}")

        while True:
            conn, addr = server.accept()
            try:
                self._handle_request(conn)
            except Exception as e:
                print(f"[MetadataHub] Error: {e}")
            finally:
                conn.close()

    def _handle_request(self, conn):
        """
        Procesa request entrante y delega a servicio apropiado.
        """
        raw_data = conn.recv(4096).decode('utf-8').strip()

        if not raw_data:
            return

        try:
            request = json.loads(raw_data)
        except json.JSONDecodeError:
            response = {"status": "error", "message": "Invalid JSON"}
            conn.sendall((json.dumps(response) + "\n").encode('utf-8'))
            return

        action = request.get('action')

        if not action:
            response = {"status": "error", "message": "Missing 'action' field"}
            conn.sendall((json.dumps(response) + "\n").encode('utf-8'))
            return

        print(f"[MetadataHub] Action: {action}")

        if action == "metadata":
            response = self._handle_metadata(request)

        elif action == "analyze":
            response = self._handle_analyze(request)

        else:
            response = {"status": "error", "message": f"Unknown action: {action}"}

        conn.sendall((json.dumps(response) + "\n").encode('utf-8'))

    def _handle_metadata(self, request):
        """
        Maneja peticiones de metadata.
        """
        query = request.get('query')
        flags = request.get('flags', [])

        if not query:
            return {"status": "error", "message": "Missing 'query' field"}

        use_filter = FlagsResolver.should_use_filter(flags)

        metadata = self.metadata_service.get_metadata(query, use_filter)

        if metadata:
            return {"status": "ok", "data": metadata}
        else:
            return {"status": "error", "message": "No results found"}

    def _handle_analyze(self, request):
        """
        Maneja peticiones de analisis de audio.
        """
        file_path = request.get('path')

        if not file_path:
            return {"status": "error", "message": "Missing 'path' field"}

        if self.analysis_service is None:
            self.analysis_service = AnalysisService()

        result = self.analysis_service.analyze(file_path)

        if result:
            return {"status": "ok", "data": result}
        else:
            return {"status": "error", "message": "Analysis failed"}


if __name__ == "__main__":
    hub = MetadataHub()
    hub.start()