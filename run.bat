@echo off
set JAVA_HOME=C:\Users\youss\.jdks\jbr-17.0.14
set PATH=%JAVA_HOME%\bin;%PATH%

java --module-path "%JAVA_HOME%\lib" --add-modules javafx.controls,javafx.fxml --add-reads pidev=ALL-UNNAMED --add-opens pidev/org.example.pidev=ALL-UNNAMED -cp "target/classes;target/dependency/*" org.example.pidev.test.MainFX

pause 