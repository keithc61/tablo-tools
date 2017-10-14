@echo off

set tablo=java -jar D:/space/test-440-x64/tablo2go/tablo.jar
set tablo=%tablo% -config=sample-config.properties
set tablo=%tablo% -crf=25
set tablo=%tablo% -movie

start /b /low /wait %tablo% %*
