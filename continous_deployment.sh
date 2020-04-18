set -ex
echo Starting to listen
while true; do
kill $(ps auwx | grep java | grep -v grep | awk '{print $1}') || true
mvn install
mvn exec:java -Dexec.mainClass=Bot -Dexec.args="RGAPI-4e62aa33-98eb-44e6-914e-5581379573fe NjQ0OTgzMTY4NzUxMTA4MTEy.Xla_ng.eNOyAL0mNAYrGxQpHOwNzlf0zk0"&
while git pull | grep Already; do
sleep 30
done
done
