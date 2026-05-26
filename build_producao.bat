@echo off
echo Construindo Prime Pro Fast para producao...
echo.

cd android

echo Limpando build anterior...
call gradlew clean

echo.
echo Gerando Android App Bundle (AAB)...
call gradlew bundleRelease

echo.
echo Build concluido!
echo Arquivo gerado: android\app\build\outputs\bundle\release\app-release.aab
echo.
echo Este arquivo deve ser enviado para o Google Play Console.
echo.
pause
