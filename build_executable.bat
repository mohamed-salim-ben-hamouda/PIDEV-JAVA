@echo off
echo ==============================================
echo  Generation de l'executable du module Quiz
echo ==============================================
echo.
echo Ce script va compiler le projet et generer un environnement d'execution Java autonome.
echo L'executable sera disponible dans target\app\bin\app.bat
echo.

call mvnw clean javafx:jlink

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ==============================================
    echo  Succes! 
    echo  L'application peut etre lancee via: target\app\bin\app.bat
    echo ==============================================
) else (
    echo.
    echo ==============================================
    echo  Echec de la generation.
    echo ==============================================
)
pause
