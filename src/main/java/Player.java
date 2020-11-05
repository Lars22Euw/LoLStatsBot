import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.io.*;
import java.util.*;

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

        System.out.println("Latest "+Util.dtf.print(latest));
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
            System.out.println();
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
            System.out.println(name+" "+matches.size());
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

    public List<Summoner> getPremades(List<Match> games) {
        var result = new ArrayList<Summoner>();
        var allSummoners = new ArrayList<Summoner>();

        if (games == null || games.size() == 0){
            System.out.println("No games found.");
            return result;
        }

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
            for (var player: Util.getPlayers(summoner, game)) {
                if (player.equals(summoner)) continue;

                if (allSummoners.contains(player) && !result.contains(player)) {
                    result.add(player);
                    System.out.println("Added "+player.getName());
                }
                if (!allSummoners.contains(player)) allSummoners.add(player);
            }
        }

        System.out.println("Found "+ result.size()+ " hits out of "+allSummoners.size()+" players.");

        return result;
    }

     static HashMap<Integer, Pair> lookupChamps(List<Match> games, Summoner summoner) {
        var gamesWon = new HashMap<Integer, Pair>();
        for (var game: games) {
            var p = game.getParticipants().find(pa -> pa.getSummoner().equals(summoner));
            final boolean winning = p.getTeam().isWinner();
            final int id = p.getChampion().getId();
            if (gamesWon.containsKey(id)) {
                gamesWon.get(id).add(winning);
            } else gamesWon.put(id, new Pair(winning));
        }
        return gamesWon;
    }

    void printChamps(HashMap<Integer, Pair> games, int gamesPlayed) {
        var result = new ArrayList<>(games.entrySet());
        result.sort(comparator);
        for (var entry : result) {
            if (entry.getValue().games < gamesPlayed) continue;
            var name = Champion.withId(entry.getKey()).get().getName();
            System.out.println(name + " " + entry.getValue().wins + "/" + entry.getValue().games);
        }
    }


    /**
     * games won with this player
     * @param games a list of games to check
     * @return Map of wins and total games per playerID
     */
    public static HashMap<String, Pair> lookup(List<Match> games, Summoner summoner) {
        var gamesTogether = new HashMap<String, Pair>();
        for (var game : games) {
            var team = Util.getTeam(summoner, game);
            var winning = team.isWinner();
            for (var p : team.getParticipants()) {
                var sum = p.getSummoner().getAccountId();
                if (gamesTogether.containsKey(sum)) {
                    gamesTogether.get(sum).add(winning);
                } else {
                    gamesTogether.put(sum, new Pair(winning));
                }
            }
        }
        return gamesTogether;
    }

    Comparator comparator = (Comparator<Map.Entry<Object, Pair>>) (o1, o2) -> o1.getValue().compareTo(o2.getValue());

    public void printLookup(HashMap<String, Pair> games, int gamesPlayed) {
        var result = new ArrayList<>(games.entrySet());
        result.sort(comparator);
        for (var entry : result) {
            if (entry.getValue().games < gamesPlayed) continue;
            var name = Summoner.withId(entry.getKey()).get().getName();
            System.out.println(name + " " + entry.getValue().wins + "/" + entry.getValue().games);
        }
        System.out.println("Total wins: "+games.get(summoner.getAccountId()).wins +
                " from "+games.get(summoner.getAccountId()).games);
    }

    public static void main(String[] args) {
        var res = MyMessage.stalk(Summoner.named("PinkMeowerRanger").get(),  2, null);
    }

}
