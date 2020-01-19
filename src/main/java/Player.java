import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *  a player contains one summoner in the default region.
 *  and all recorded games for that player.
 */
public class Player {
    Summoner summoner = null;
    String name;
    public SortedSet<Game> matches = new TreeSet<>(Game::compare2);

    private static final String folder = "summoners\\";

    public Player() {
        System.out.println("empty constructor called.");
    }

    public Player(Summoner sum, Manager manager) {
        this.name = sum.getName().replace(" ", "");
        this.summoner = sum;
        var latest = read(manager);

        System.out.println(latest.toString());
        System.out.println("matches size: "+matches.size());
        MatchHistory history = summoner.matchHistory().withStartTime(latest).get();

        for (Match match: history) {
            var game = new Game(match);
            matches.add(game);
            manager.games.add(game);
        }

        if (matches == null ||matches.size() == 0) {
            System.out.println(name+ " played 0 games.");
            manager.summonersInactive.add(name);
        } else  if (history.size() <= 1) {
            System.out.println("");
        } else {
            manager.summonersActive.add(this);
            var size = history.size()-1;
            System.out.println(" + "+size+" new Games.");
        }

        write();
    }

    /**
     * Checks users directory for a file with playername.
     * It then loads all entries into the matches list.
     * @return time of last recorded game or 2 years in the past.
     */
    DateTime read(Manager manager) {
        try {
            var br = new BufferedReader(new FileReader(folder+name+".txt"));
            while (br.ready()) {
                var game = new Game(br.readLine());
                matches.add(game);
                manager.games.add(game);
            }
            System.out.print(name+" "+matches.size());
        } catch (FileNotFoundException e) {
            // TODO: no file found. init empty set.
            System.out.println(name+" has no file.");
            matches = new TreeSet<>(Game::compare2);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (matches.size() != 0)
            return matches.last().time;
        return DateTime.now().minusYears(4);
    }

    void write() {
        try {
            var bw = new BufferedWriter(new FileWriter(folder+name+".txt"));
            for (Game game: matches) {
                bw.write(game.print()+"\n");
            }
            bw.close();
        } catch (IOException e) {
            System.err.println("Error in saving history for player "+name);
            e.printStackTrace();
        }

    }

    List<Summoner> getPremades(List<Match> games) {
        List result = new ArrayList<Summoner>();
        List allSumoners = new ArrayList<Summoner>();

        if (games == null || games.size() == 0)
            return result;

        if (games.size() == 1) {
            System.out.println("Cannot detect premades from one match alone.");
            return result;
        }

        System.out.println("Input: "+games.size()+" games.");

        if (summoner == null || !summoner.exists()) {
            System.out.println("Summoner was null or not found.");
            return result;
        }

        System.out.println("Checking premades for: "+name);

        for (var game: games) {
            //game.getBlueTeam().getParticipants().get(0).
            Summoner[] players = game.getBlueTeam().getParticipants().contains(summoner)
                    ? game.getBlueTeam().getParticipants().stream().map(s -> s.getSummoner()).toArray(Summoner[]::new)
                    : game.getRedTeam().getParticipants().stream().map(s -> s.getSummoner()).toArray(Summoner[]::new);
            //var players = game.getParticipants();
            for (var player: players) {
                if (allSumoners.contains(player) && !result.contains(player)) {
                    result.add(player);
                    System.out.println("Added "+player.getName());
                }
                if (!allSumoners.contains(player)) allSumoners.add(player);
            }
        }

        System.out.println("Found "+ result.size()+ " hits out of "+allSumoners.size()+" players.");

        return result;
    }


}
