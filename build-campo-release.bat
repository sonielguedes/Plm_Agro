@echo off
setlocal

echo ------------------------------------------
echo INICIANDO BUILD DE CAMPO - PLMAGRO
echo ------------------------------------------

echo [1/4] Limpando build anterior...
call gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha ao limpar o projeto.
    exit /b %ERRORLEVEL%
)

echo [2/4] Rodando validacoes (Lint e Testes)...
call gradlew.bat lint test
if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha nas validacoes de qualidade ou testes unitarios.
    exit /b %ERRORLEVEL%
)

echo [3/4] Gerando APK de Release (Producao)...
echo Config: Minify=True, Shrink=True, Env=PROD
call gradlew.bat :app:assembleRelease
if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha ao gerar o APK de Release.
    exit /b %ERRORLEVEL%
)

echo [4/4] Organizando pacote de distribuicao...
if not exist "dist" mkdir "dist"

:: Verifica se o APK foi gerado no local padrao do Gradle
if exist "app\build\outputs\apk\release\app-release.apk" (
    copy "app\build\outputs\apk\release\app-release.apk" "dist\PLMAGRO-CAMPO-release.apk" /Y
) else if exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
    copy "app\build\outputs\apk\release\app-release-unsigned.apk" "dist\PLMAGRO-CAMPO-release.apk" /Y
) else (
    echo ERRO: APK gerado nao encontrado.
    exit /b 1
)

if %ERRORLEVEL% neq 0 (
    echo ERRO: Falha ao copiar o arquivo para a pasta dist.
    exit /b %ERRORLEVEL%
)

echo ------------------------------------------
echo APK DE CAMPO GERADO COM SUCESSO
echo Local: dist\PLMAGRO-CAMPO-release.apk
echo ------------------------------------------
pause
