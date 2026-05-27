#!/bin/bash

# Configura para parar a execucao em caso de qualquer erro
set -e

echo "------------------------------------------"
echo "INICIANDO BUILD DE CAMPO - PLMAGRO"
echo "------------------------------------------"

echo "[1/4] Limpando build anterior..."
./gradlew clean

echo "[2/4] Rodando validacoes (Lint e Testes)..."
./gradlew lint test

echo "[3/4] Gerando APK de Release (Producao)..."
echo "Config: Minify=True, Shrink=True, Env=PROD"
./gradlew :app:assembleRelease

echo "[4/4] Organizando pacote de distribuicao..."
mkdir -p dist

# Verifica o nome do arquivo gerado (pode variar se houver assinatura ou nao)
if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
    cp app/build/outputs/apk/release/app-release.apk dist/PLMAGRO-CAMPO-release.apk
elif [ -f "app/build/outputs/apk/release/app-release-unsigned.apk" ]; then
    cp app/build/outputs/apk/release/app-release-unsigned.apk dist/PLMAGRO-CAMPO-release.apk
else
    echo "ERRO: APK gerado nao encontrado."
    exit 1
fi

echo "------------------------------------------"
echo "APK DE CAMPO GERADO COM SUCESSO"
echo "Local: dist/PLMAGRO-CAMPO-release.apk"
echo "------------------------------------------"
