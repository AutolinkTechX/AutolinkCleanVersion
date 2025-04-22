@echo off
set JAVA_HOME=C:\Users\youss\.jdks\jbr-17.0.14
set PATH=%JAVA_HOME%\bin;%PATH%

java --module-path "%JAVA_HOME%\lib" --add-modules javafx.controls,javafx.fxml -cp "target/classes" org.example.pidev.test.MainFX

pause 