@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################
SET mypath=%~dp0
echo %mypath%
cd %mypath%
gradlew.bat --stop
cd -