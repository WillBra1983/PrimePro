@echo off
echo ========================================
echo    GERADOR DE AAB PARA GOOGLE PLAY
echo ========================================
echo.

REM Verificar se keystore existe
if not exist "keystore\primeprofast-release.keystore" (
    echo ERRO: Keystore de producao nao encontrado!
    echo.
    echo Execute primeiro: gerar_keystore_producao.bat
    echo.
    pause
    exit /b 1
)

REM Verificar se keystore.properties existe
if not exist "keystore.properties" (
    echo ERRO: Arquivo keystore.properties nao encontrado!
    echo.
    echo Configure as senhas no arquivo keystore.properties
    echo.
    pause
    exit /b 1
)

echo Gerando AAB de producao...
echo.

REM Parar daemons existentes
.\gradlew --stop

REM Gerar AAB de producao
.\gradlew bundleRelease --no-daemon

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo    AAB GERADO COM SUCESSO!
    echo ========================================
    echo.
    echo Localizacao: app\build\outputs\bundle\release\app-release.aab
    echo.
    echo Este arquivo pode ser enviado para o Google Play Console.
    echo.
) else (
    echo.
    echo ERRO: Falha ao gerar AAB!
    echo Verifique as configuracoes do keystore.
    echo.
)

pause
