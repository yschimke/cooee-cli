@echo off
call "C:\Program Files (x86)\Microsoft Visual Studio\2019\BuildTools\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
"C:\Users\yuri\.gradle\caches\com.palantir.graal\20.2.0\11\graalvm-ce-java11-20.2.0\bin\native-image.cmd" "-cp" "build\libs\cooee-cli-master-28862de-dirty-all.jar" "-H:Path=C:\Users\yuri\workspace\cooee-cli\build" "--enable-https" "--no-fallback" "--allow-incomplete-classpath" "-H:Name=cooee" "com.baulsupp.cooee.cli.Main"
