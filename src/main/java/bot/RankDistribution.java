package bot;

import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.io.*;
import java.util.*;

public class RankDistribution {
    Manager manager;

    List<Match> matches = new ArrayList<>();
    Set<Summoner> summoners = new HashSet<>();
    List<List<Summoner>> buckets = new ArrayList<>();


    RankDistribution(Manager manager, String filename) {
        this.manager = manager;


        try {
            var br = new BufferedReader(new FileReader(filename));
            while (br.ready()) {
                var str = br.readLine();
                for (var game: str.split(", ")) {
//                    if (matches.size() == 5000) return;
                    if (game == null) continue;
                    var match = Match.withId(Long.parseLong(game)).get();
                    if (match == null) continue;
                    matches.add(match);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // game id -> games -> players

    // filter ranked players
    // ranked games played (log buckets)
    // display

    void extractPlayers() throws InterruptedException {
        // get all players per gameID, keep them unique.
        for (var match: matches) {
            try {
                for (var part : match.getParticipants()) {
                    summoners.add(part.getSummoner());
                }
            } catch (Exception e) {
                Thread.sleep(10000);
            }
        }
    }


    void sortByRankedGames() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            buckets.add(new ArrayList<>());
        }
        // bucket n = 2*(n-1); n0 = 10
        // index  0  1  2  3  4
        // games 10 20 40 80 160

        for (var summoner: summoners) {
            try {
                var rs = summoner.getLeaguePosition(Queue.RANKED_SOLO);
                if (rs == null) {
                    buckets.get(0).add(summoner);
                    continue;
                }
                var games = rs.getWins() + rs.getLosses();
//                System.out.println(games+ " " + summoner.getName());
                buckets.get(getBucketIndex(games)).add(summoner);
            } catch (Exception e) {
                Thread.sleep(5000);
            }
        }
    }

    int getBucketIndex(int games) {
        // get bucket index
        return Math.max((int) (Math.log(games / 10 +1) / Math.log(2)), 0);
    }

    void printBuckets() {
        for (var bucket: buckets) {
            System.out.println(bucket.size());
        }
    }

}
