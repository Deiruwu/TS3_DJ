@echo off
setlocal EnableDelayedExpansion
title Configurador TS3 Bot

:: --- COLORES (Simulados) ---
echo ==========================================
echo      CONFIGURADOR RAPIDO (WINDOWS)
echo ==========================================
echo.

:: --- VALORES POR DEFECTO ---
set "DEFAULT_NAME=TS3_Bot"
set "DEFAULT_IP=127.0.0.1"
set "ID_FILE=identity_pi.ini"
set "VOLUME=50"
set "TIMEOUT=10000"

:: --- 1. CONFIGURACION GENERAL ---
echo --- Configuracion General ---

:: Preguntar Nombre
set "BOT_NAME=%DEFAULT_NAME%"
set /p "INPUT_NAME=Nombre del Bot [%DEFAULT_NAME%]: "
if not "%INPUT_NAME%"=="" set "BOT_NAME=%INPUT_NAME%"

:: Preguntar IP
set "TS_IP=%DEFAULT_IP%"
set /p "INPUT_IP=IP del Servidor TS3 [%DEFAULT_IP%]: "
if not "%INPUT_IP%"=="" set "TS_IP=%INPUT_IP%"

:: --- 2. GENERAR JSON ---
echo.
echo Generando config.json...

(
echo {
echo   "default": {
echo     "timeout": %TIMEOUT%,
echo     "audio": {
echo       "volume": %VOLUME%
echo     },
echo     "bot": {
echo       "nickname": "%BOT_NAME%",
echo       "identity": {
echo         "file": "%ID_FILE%"
echo       }
echo     }
echo   },
echo   "servers": [
echo     {
echo       "address": "%TS_IP%"
echo     }
echo   ]
echo }
) > config.json

echo [OK] config.json creado correctamente.

:: --- 3. SPOTIFY API ---
echo.
echo --- Spotify API ---

if not exist "YoutubeApi" mkdir "YoutubeApi"

set "CONF_SPOTIFY=n"
set /p "CONF_SPOTIFY=Configurar credenciales de Spotify? (s/n) [n]: "

if /i "%CONF_SPOTIFY%"=="s" (
    set /p "SPOTIFY_ID=Client ID: "
    set /p "SPOTIFY_SECRET=Client Secret: "

    (
    echo SPOTIFY_CLIENT_ID=!SPOTIFY_ID!
    echo SPOTIFY_CLIENT_SECRET=!SPOTIFY_SECRET!
    ) > YoutubeApi\.env

    echo [OK] Credenciales guardadas en YoutubeApi\.env
) else (
    echo Saltando configuracion de Spotify.
)

echo.
echo ==========================================
echo             ¡LISTO!
echo ==========================================
pause