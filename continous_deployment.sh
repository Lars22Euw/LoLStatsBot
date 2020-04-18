set -ex
echo Starting to listen
while true; do
kill $(ps auwx | grep java | grep -v grep | awk '{print $1}') || true
echo $(cat discordapi.txt)
echo $(cat riotapi.txt)
mvn install
mvn exec:java -Dexec.mainClass=Bot -Dexec.args="$(cat riotapi.txt) $(cat discordapi.txt)"&
while git pull | grep Already; do
sleep 30
done
done
