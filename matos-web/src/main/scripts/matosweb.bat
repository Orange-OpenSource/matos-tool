@echo off
cls
setlocal ENABLEEXTENSIONS
set KEY_NAME="HKLM\SOFTWARE\JavaSoft\Java Runtime Environment"
set VALUE_NAME=CurrentVersion

::
:: get the current version
::


FOR /F "usebackq skip=2 tokens=3" %%A IN (`REG QUERY %KEY_NAME% /v %VALUE_NAME% 2^>nul`) DO (
    set ValueValue=%%A
)

if not defined ValueValue (
    @echo No registry entry found for Java.
    goto end
)

set ok=false
if "%ValueValue%" == "1.6" set ok=true
if "%ValueValue%" == "1.7" set ok=true
if "%ok%" == "false" (
    @echo Unsupported java version %ValueValue%.
    goto end
)

set JAVA_CURRENT="HKLM\SOFTWARE\JavaSoft\Java Runtime Environment\%ValueValue%"
set JAVA_HOME=JavaHome

::
:: get the javahome
::
FOR /F "usebackq skip=2 tokens=3,4" %%A IN (`REG QUERY %JAVA_CURRENT% /v %JAVA_HOME% 2^>nul`) DO (
    set JAVA_PATH=%%A %%B
)

echo %JAVA_PATH%

for %%A in ("%~dp0..") do set MATOS_HOME=%%~fA
echo %MATOS_HOME%

set MATOS_JAR=matosweb.jar
set EXEC=%MATOS_HOME%\lib\%MATOS_JAR%


"%JAVA_PATH%\bin\java.exe" -cp "%MATOS_HOME%"'\lib\*' -Xmx1500m -Dmatos.keystore="%MATOS_HOME%\keystore" -Dmatos.authdb="%MATOS_HOME%\auth.db" -Dmatos.lib="%MATOS_HOME%" -Dmatos.temp="%TEMP%" -jar "%EXEC%"  -extractDirectory "%MATOS_HOME%\extract" 

:end
