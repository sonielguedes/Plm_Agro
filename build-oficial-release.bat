@echo off
setlocal

echo ====================================================
echo INICIANDO BUILD OFICIAL DE PRODUCAO - PLMAGRO
echo terminal telemetrico operacional agricola
echo ====================================================

echo [1/6] Limpando ambiente...
call gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo ERRO CRITICO: Falha ao limpar o projeto.
    exit /b %ERRORLEVEL%
)

echo [2/6] Executando analise estatica de qualidade (Lint)...
call gradlew.bat lint
if %ERRORLEVEL% neq 0 (
    echo ERRO CRITICO: Falha no Lint. Corrija os problemas antes de prosseguir.
    exit /b %ERRORLEVEL%
)

echo [3/6] Executando testes unitarios de regressao...
call gradlew.bat test
if %ERRORLEVEL% neq 0 (
    echo ERRO CRITICO: Testes unitarios falharam. Build interrompido.
    exit /b %ERRORLEVEL%
)

echo [4/6] Compilando APK Oficial de Producao...
echo Modo: Release | Minify: Ativo | R8: Ativo
call gradlew.bat :app:assembleRelease
if %ERRORLEVEL% neq 0 (
    echo ERRO CRITICO: Falha na compilacao do APK de Producao.
    exit /b %ERRORLEVEL%
)

echo [5/6] Validando integridade do binario...
if not exist "app\build\outputs\apk\release\app-release.apk" (
    if not exist "app\build\outputs\apk\release\app-release-unsigned.apk" (
        echo ERRO CRITICO: Binario nao encontrado.
        exit /b 1
    )
)

echo [6/6] Finalizando pacote de distribuicao oficial...
if not exist "dist" mkdir "dist"
if exist "app\build\outputs\apk\release\app-release.apk" (
    copy "app\build\outputs\apk\release\app-release.apk" "dist\PLMAGRO-OFICIAL-release.apk" /Y
) else (
    copy "app\build\outputs\apk\release\app-release-unsigned.apk" "dist\PLMAGRO-OFICIAL-release.apk" /Y
)

echo ====================================================
echo BUILD CONCLUIDO COM SUCESSO!
echo APK: dist\PLMAGRO-OFICIAL-release.apk
echo PRONTO PARA INSTALACAO NO VEICULO.
echo ====================================================
pause
