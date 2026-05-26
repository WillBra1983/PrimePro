@echo off
echo Gerando certificado de assinatura para Prime Pro Fast...
echo.

cd android\app

keytool -genkey -v -keystore primeprofast-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias primeprofast -storepass primeprofast2024 -keypass primeprofast2024 -dname "CN=Wilson Lucas Ferreira, OU=Development, O=Prime Pro Fast, L=Rio Branco, ST=AC, C=BR"

echo.
echo Certificado gerado com sucesso!
echo Arquivo: android\app\primeprofast-release-key.jks
echo Senha: primeprofast2024
echo.
echo IMPORTANTE: Guarde este arquivo e senha com seguranca!
echo Voce precisara deles para todas as atualizacoes futuras.
echo.
pause
