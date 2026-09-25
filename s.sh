#cp -r ~/sd/a/c/* /root/combine/src/combine
#echo "copy completed"
./gradlew deploy
mv build/libs/combinec.jar ~/sd
