#!/bin/bash

# Colores
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${GREEN}=== Configurador Rápido del Bot (Sin dependencias) ===${NC}"

# Valores por defecto
ID_FILE="identity.ini"
VOLUME="50"
TIMEOUT="10000"

# Función simple para preguntar
ask() {
    local prompt="$1"
    local default="$2"
    local var_name="$3"

    echo -ne "${CYAN}${prompt} [${default}]: ${NC}"
    read input
    eval $var_name="${input:-$default}"
}

# --- 1. DATOS BÁSICOS ---
echo -e "\n${GREEN}--- Configuración General ---${NC}"

ask "Nombre del Bot" "TS3_Bot" BOT_NAME
ask "IP del Servidor TS3" "127.0.0.1" TS_IP

# --- 2. DISCORD ---
echo -e "\n${GREEN}--- Integración con Discord ---${NC}"
echo "Pega aquí tu Webhook URL para enviar notificaciones."
echo "Si no quieres usar Discord, simplemente presiona ENTER."
ask "Webhook URL" "" DISCORD_WEBHOOK

# --- 3. GENERAR JSON ---
echo -e "\nGenerando config.json..."

cat > config.json <<EOF
{
  "default": {
    "timeout": $TIMEOUT,
    "audio": {
      "volume": $VOLUME
    },
    "bot": {
      "nickname": "$BOT_NAME",
      "identity": {
        "file": "$ID_FILE"
      }
    }
  },
  "servers": [
    {
      "address": "$TS_IP",
      "discord_webhook": "$DISCORD_WEBHOOK"
    }
  ]
}
EOF

echo -e "${GREEN}config.json creado correctamente.${NC}"

echo -e "\n${GREEN}--- Spotify API ---${NC}"

if [ ! -d "YoutubeApi" ]; then
    mkdir -p YoutubeApi
fi

ask "¿Configurar credenciales de Spotify? (s/n)" "n" CONFIGURE_SPOTIFY

if [[ "${CONFIGURE_SPOTIFY,,}" == "s" ]]; then
    ask "Client ID" "" SPOTIFY_ID
    ask "Client Secret" "" SPOTIFY_SECRET

    echo "SPOTIFY_CLIENT_ID=$SPOTIFY_ID" > YoutubeApi/.env
    echo "SPOTIFY_CLIENT_SECRET=$SPOTIFY_SECRET" >> YoutubeApi/.env
    echo -e "${GREEN}Credenciales guardadas.${NC}"
else
    echo "Saltando configuración de Spotify."
fi

echo -e "\n${GREEN}=== ¡Listo! ===${NC}"