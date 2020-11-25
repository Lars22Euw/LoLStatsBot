for c in $(cat champions.txt); do
   echo "http://ddragon.leagueoflegends.com/cdn/10.24.1/img/champion/${c}.png"
   curl "http://ddragon.leagueoflegends.com/cdn/10.24.1/img/champion/${c}.png" --output "$c".png
done
