# TeamSpeak 3 Music Bot (Hybrid Java/Python)

> **Implementación propia de un bot de música para TeamSpeak 3 basada en [TS3J](https://github.com/Manevolent/ts3j) de Manevolent pensando para ser ejecutado en una raspberry u otros dispositivos**

La implementación de un bot de música presentada por Manevolent es muy básica (como lo menciona). Esta es una implementación personal y más completa: el **Core en Java** maneja la conexión y el streaming de audio (Opus), mientras que un **microservicio en Python** resuelve metadatos y enlaces complejos (Spotify/YouTube) comunicándose vía Sockets locales.

---

Este bot de música no presenta actualmente caracteristicas como manejo de white list, blacklist, admins o algo similar; la razón es porque se ha priorizado la experiencia del usuario, además de estar pensando más para un servidor de amigos qué un servidor gigante (aunque igualmente es usable en este)

Este bot está pensando para correr en segundo plano 24/7 y tener algo de espacio en el disco, pues todas las canciones son descargadas y guardas en la carpeta **cache** además de  genera una base de datos **en sqlite** con las canciones solicitadas por cada usuario y crear una playlist personal con las canciones solicitadas individualmente.
## Características
* **Sistema de guardado:** Dadas las limitantes de descarga de **yt-dlp** y posibles problemas con el internet, se optó por registrar las canciones en una base de datos así como su archivo **.m4a**, aquí también se almacenan sus metadatos y su ruta.

* **Sistema de playlist:** Gracias a la base de datos, podemos implementar un sistema de playlist, donde cada qué un usuario pide una canción **!p <cancion/url>** se registra la canción, creando una playlist llamada **Música de [nombre del usuario]** y guardando un registro de tus canciones escuchadas qué puede ser utilizada después para reproducir una playlist completa o inclusive crear tu propia playlist personalizada 

* **Sistema de creación de playlist mejorado:** Se utilizó set theory para crear 3 comandos nuevos, los qué permiten añadir a la cola de reproducción la unión, intersección y symdiff de 2 o más playlist.

* **Soporte Multi-plataforma:** YouTube (video/playlists) y Spotify (canciones/playlists).

* **Integración con Discord:** Dado qué este bot nació gracias a la necesidad de remplazar discord (y sus bots de música) se incorporó la opción de mandar mensajes a un servidor de discord para mostrar qué usuarios están conectados en el servidor de TeamSpeak




## Requisitos del Sistema

El bot está diseñado para entornos **Linux** (Debian, Raspberry Pi OS, Arch Linux) proximamente se incorporará un docker para qué pueda ser ejecutado en windows.

* **Java 21 (JDK)**
* **Maven** (Para compilar)
* **Python 3.11+** (Con módulo venv)
* **FFmpeg** (Indispensable para conversión de audio)
* **yt-dlp** (Versión más reciente recomendada)
* **tmux** (Opcional, para procesos en background)

## Instalación

### 1. Dependencias del Sistema (Debian/Ubuntu/Raspberry OS)

```bash
sudo apt update
sudo apt install maven python3-full python3-pip git ffmpeg tmux -y

```

> **Nota sobre yt-dlp:** Los repositorios de apt suelen tener versiones desactualizadas. Se recomienda instalarlo/actualizarlo manualmente:
> `sudo wget https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O /usr/local/bin/yt-dlp && sudo chmod a+rx /usr/local/bin/yt-dlp`


### 2. Configurar Entorno Python

Debido a las políticas de `PEP 668` en Linux modernos, usaremos un entorno virtual:

```bash
cd YoutubeApi
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
deactivate
cd ..

```

## Configuración

### 1. Configuración del Bot (Java)

Utiliza el script de configuración (`setup.sh`) para preparar el bot

```bash
# Dar permisos de ejecución
chmod +x setup.sh

# Ejecutar
./setup.sh
```

### 2. Credenciales de Spotify (Python)

Para poder poner un link de spotify y qué el bot pueda reproducir la canción, necesitarás ingresar las credenciales. El script anterior debería ser suficiente para qué funcione, sin embargo si no lo configuraste o quieres cambiarlo vea la carpeta de YoutubeApi y crear un archivo .venv

```bash
# Dentro de /YoutubeApi/.env
SPOTIFY_CLIENT_ID=tu_client_id_aqui
SPOTIFY_CLIENT_SECRET=tu_client_secret_aqui
```

### 3. Identidad (Importante)

Al iniciar el bot por primera vez creará un archivo (`identity.ini`). Deberás darle permisos de administrador al bot y no borrar el archivo, para qué este pueda conservar sus permisos
## Ejecución

Puedes usar el script de inicio automático que gestiona tanto el socket de Python como el proceso de Java:

```bash
# Dar permisos de ejecución
chmod +x start.sh

# Ejecutar
./start.sh
```

## Comandos
Aquí tienes la documentación completa de tus comandos basada en la estructura de archivos que me mostraste. He organizado las tablas por **categorías** para que se vea mucho más ordenado en tu README, en lugar de una lista gigante.

### Control de Reproducción (Playback)

Comandos para manejar la música que suena actualmente y la cola.

| Comando | Archivo | Descripción                                        | Ejemplo                     |
| --- | --- |----------------------------------------------------|-----------------------------|
| `!play <canción>` | `PlayCommand` | Reproduce una URL o busca por nombre.              | `!play El tesoro`           |
| `!playnext <canción>` | `PlayNextCommand` | Pone una canción al inicio de la cola (siguiente). | `!playnext La noche eterna` |
| `!skip` | `SkipCommand` | Salta la canción actual.                           | `!skip`                     |
| `!skipto <índice>` | `SkipToCommand` | Salta directamente a una posición de la cola.      | `!skipto 5`                 |
| `!clear` | `ClearCommand` | Detiene la música y vacía la cola entera.          | `!clear`                    |
| `!queue` | `QueueCommand` | Muestra la lista de canciones en cola.             | `!queue`                    |
| `!remove <índice>` | `RemoveCommand` | Elimina una canción específica de la cola.         | `!remove 3`                 |
| `!shuffle` | `ShuffleCommand` | Mezcla aleatoriamente la cola actual.              | `!shuffle`                  |
| `!volume <0-100>` | `VolumeCommand` | Ajusta el volumen del bot.                         | `!volume 50`                |
| `!like` | `LikeCommand` | Añade la canción actual a tus "Favoritos".         | `!like`                     |
| `!cancel` | `CancelCommand` | Cancela la descarga de una canción.                | `!cancel`                   |
| `!delete` | `DeleteCommand` | Elimina el track actual (Físico/BD) o de la cola.  | `!delete`                   |

---

### Gestión de Playlists

Comandos para crear, editar y escuchar tus listas personalizadas.

| Comando                              | Archivo | Descripción | Ejemplo                          |
|--------------------------------------| --- | --- |----------------------------------|
| `!createplaylist <nombre>`           | `CreatePlaylistCommand` | Crea una nueva playlist vacía. | `!createplaylist Rock indie`     |
| `!deleteplaylist <id>`               | `DeletePlaylistCommand` | Elimina una playlist completa. | `!deleteplaylist 5`              |
| `!playlists`                         | `ListPlaylistsCommand` | Lista todas tus playlists guardadas. | `!playlists`                     |
| `!showplaylist <id>`                 | `ShowPlaylistCommand` | Muestra las canciones dentro de una playlist. | `!showplaylist 3`                |
| `!playplaylist <id>`                 | `PlayPlaylistCommand` | Carga una playlist en la cola. | `!playplaylist 11`               |
| `!renameplaylist <id> <nuevo>`       | `RenamePlaylistCommand` | Cambia el nombre de una playlist. | `!renameplaylist 5 Soda estereo` |
| `!addtoplaylist <nombre> [url]`      | `AddToPlaylistCommand` | Añade la canción actual (o URL) a una playlist. | `!addtoplaylist Crimen Cerati`   |
| `!removefromplaylist <id> <id_posicion_cancion>` | `RemoveToPlaylistCommand` | Elimina una canción de una playlist. | `!removefromplaylist 2 4`        |
| `!dislike`                           | `DislikeCommand` | Elimina la canción actual de "Favoritos". | `!dislike`                       |

---

### Operaciones de Conjuntos (Playlists Avanzadas)

Herramienta para crear una cola de reproducción usando teoría de conjuntos aplicada a dos o más playlist

| Comando                      | Archivo | Descripción | Ejemplo            |
|------------------------------| --- | --- |--------------------|
| `!union <pl1> <pl2> ...`     | `UnionCommand` | Crea una playlist nueva sumando dos existentes. | `!union 1 2`       |
| `!intersect <pl1> <pl2> ...` | `IntersectCommand` | Crea una playlist solo con las canciones repetidas en ambas. | `!intersect 1 2 3` |
| `!symdiff <pl1> <pl2> ...`   | `SymdiffCommand` | Crea una playlist con canciones que NO se repiten entre las dos. | `!symdiff 5 10`    |

---

### Estadísticas (Stats)

Comandos para ver qué es lo que más (o menos) suena.

| Comando | Archivo | Descripción | Ejemplo |
| --- | --- | --- | --- |
| `!top [n]` | `TopCommand` | Muestra tus canciones más escuchadas. | `!top 10` |
| `!topglobal [n]` | `TopGlobalCommand` | Muestra lo más escuchado en todo el servidor. | `!topglobal` |
| `!least [n]` | `LeastCommand` | Muestra tus canciones menos escuchadas. | `!least 5` |
| `!leastglobal [n]` | `LeastGlobalCommand` | Muestra lo menos escuchado del servidor. | `!leastglobal` |

---

### Sistema

| Comando | Archivo | Descripción | Ejemplo |
| --- | --- | --- | --- |
| `!help` | `HelpCommand` | Muestra la lista de comandos disponibles. | `!help` |
---

Desarrollado por **Dei** usando [TS3J](https://github.com/Manevolent/ts3j) y [yt-dlp](https://github.com/yt-dlp/yt-dlp).

---