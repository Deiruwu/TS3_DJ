TeamSpeak 3 Music Bot

Este proyecto es una implementación propia de un bot de música para TeamSpeak 3 utilizando ts3j de Manevolent. intenté correr algunos bots pero todos me daban error para correrlos en una Raspberri Pi 4, así que desarrollé esta solución híbrida.

El núcleo está escrito en Java (para manejar la conexión Query y el streaming de audio en Opus), mientras que la resolución de metadatos y enlaces se delega a un microservicio en Python (usando yt-dlp y APIs de Spotify). Ambos se comunican vía Sockets locales.
Requisitos del Sistema

El bot está pensado para correr en entornos Linux (desarrollado y probado en Debian/Raspberry Pi OS y Arch Linux).

Necesitas tener instalado:

    Java 21 (JDK).

    Maven (para compilar el proyecto Java).

    Python 3.11+ (con soporte para entornos virtuales).

    FFmpeg (indispensable para la extracción y conversión de audio).

    yt-dlp (Instalado manualmente, ver nota abajo).

    tmux (para la gestión de procesos en segundo plano).

Instalación de dependencias (Debian/Ubuntu)

sudo apt update
sudo apt install maven python3-full python3-pip git ffmpeg tmux -y

Nota importante sobre yt-dlp

No uses la versión de apt. YouTube actualiza sus protocolos constantemente y la versión de los repositorios suele quedar obsoleta, provocando errores de descarga. Instala el binario oficial directamente:

sudo wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp
sudo chmod a+rx /usr/local/bin/yt-dlp

Se recomienda configurar un cronjob para ejecutar sudo yt-dlp -U periódicamente.
Instalación del Proyecto

    Clona el repositorio:
    Bash

git clone <URL_DEL_REPO>
cd TS3_Music_Bot

Prepara el entorno de Python: Las distribuciones modernas de Linux bloquean pip install global. Usaremos un entorno virtual local.
Bash

    cd YoutubeApi
    python3 -m venv .venv
    ./.venv/bin/pip install -r requirements.txt
    cd ..

Configuración

Antes de iniciar, es necesario renombrar los archivos de ejemplo y colocar tus credenciales.
1. Configuración del Bot (Java)

Renombra el archivo config.json.example a config.json en la raíz del proyecto.

mv config.json.example config.json

Edita config.json con los datos de tu servidor TeamSpeak:

{
  "server_ip": "127.0.0.1",
  "login_name": "serveradmin",
  "login_password": "TU_PASSWORD",
  "bot_nickname": "MusicBot"
}

2. Configuración de APIs (Python)

Para que el soporte de Spotify funcione, necesitas credenciales de desarrollador. Crear un archivo llamado  .env dentro de la carpeta YoutubeApi:


Edita el archivo .env:
SPOTIFY_CLIENT_ID=tu_client_id
SPOTIFY_CLIENT_SECRET=tu_client_secret

3. Identidad (Opcional)

Si tienes un archivo de identidad de TeamSpeak exportado (identity_pi.ini), colócalo en la raíz del proyecto para que el bot mantenga sus permisos de administrador al reconectarse.
Ejecución
