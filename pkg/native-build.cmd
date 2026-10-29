@echo off
rem Experimental native image of notepadfx. Needs Oracle GraalVM 25 and MSVC build tools.
rem Set GRAALVM_HOME to the GraalVM folder and VSBT to a folder holding devcmd.bat (it puts cl.exe on PATH).
setlocal
if "%GRAALVM_HOME%"=="" (echo Set GRAALVM_HOME to an Oracle GraalVM 25 folder. & exit /b 1)
if "%VSBT%"=="" (echo Set VSBT to the folder holding devcmd.bat of the MSVC build tools. & exit /b 1)
call "%VSBT%\devcmd.bat" || exit /b 1
set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%
cd /d "%~dp0.."
call mvn -Pnative -DskipTests package %*
