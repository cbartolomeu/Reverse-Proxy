cd ..
call gradlew build
start cmd.exe /c "gradlew startNode1"
start cmd.exe /c "gradlew startNode2"
start cmd.exe /c "gradlew startReverseProxy"
start cmd.exe /c "gradlew startClient"