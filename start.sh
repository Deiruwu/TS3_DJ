#!/bin/bash

# Nombre de la sesión de tmux
SESSION="musicbot"
# Clase principal de Java
MAIN_CLASS="TS3Bot.Main"

# 1. Si la sesión ya existe, nos unimos
tmux has-session -t $SESSION 2>/dev/null
if [ $? = 0 ]; then
    echo "La sesión $SESSION ya existe. Uniéndose..."
    tmux attach -t $SESSION
    exit
fi

# 2. Crear nueva sesión
tmux new-session -d -s $SESSION -n 'Bot-Logs'

# --------------------------------------------------------
# PANEL 1 (ARRIBA): JAVA BOT
# --------------------------------------------------------
# Usamos exec:java para desarrollo.
# Si falla, asegúrate de haber corrido 'mvn install' al menos una vez.
tmux send-keys -t $SESSION:0.0 "mvn clean compile exec:java -Dexec.mainClass=\"$MAIN_CLASS\"" C-m

# --------------------------------------------------------
# PANEL 2 (ABAJO): PYTHON API
# --------------------------------------------------------
tmux split-window -v -t $SESSION:0
tmux send-keys -t $SESSION:0.1 "cd MetadataHubServices" C-m

# --- MAGIA PYTHON ---
# Usamos el pip y python DEL entorno virtual (.venv/bin/...)
# 1. Instalamos dependencias (silencioso para no llenar logs)
tmux send-keys -t $SESSION:0.1 "./.venv/bin/pip install -r requirements.txt" C-m
# 2. Corremos el script
tmux send-keys -t $SESSION:0.1 "./.venv/bin/python3 MetadataHub.py" C-m

# --------------------------------------------------------
# FINALIZAR
# --------------------------------------------------------
tmux select-pane -t $SESSION:0.0
tmux attach -t $SESSION