@echo off

set rmcom=java -classpath "%~dp0%tablo.jar" tablo.CommercialRemover

if "%~1%" == "" (
  echo Usage: %~n0% video.mp4 ...
) else (
  rem start /b /belownormal /wait 
  nice %rmcom% %*
)
