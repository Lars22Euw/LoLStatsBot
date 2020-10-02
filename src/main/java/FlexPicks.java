import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.*;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.league.League;
import com.merakianalytics.orianna.types.core.match.*;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toList;

public class FlexPicks {

    public static final int FLEX_GAMES = 150; // how much games a flex player needs to be scouted
    public static final int CLASH_GAMES = 10;
    private static final int TREE_DEPTH = 1;

    public static HashMap<String, Integer> wins;
    public static HashMap<String, Integer> total;
    private static Set<Summoner> summonerDataset = new HashSet<>();
    private static Set<Match> games = new HashSet<>();
    private static Set<String> flexLeagues = new HashSet<>();

    public static void main(String[] args) throws IOException {
        String sumName = "Ω Regúla";
        new Manager(args[0]);
        var summoner = Summoner.named(sumName).get();
        System.out.println(summoner.exists());
        summonerDataset.add(summoner);
        for (int i = 0; i < TREE_DEPTH; i++) {
            var additionalSummoners = new HashSet<Summoner>();
            for (var sum : summonerDataset) {
                additionalSummoners.addAll(exploreSummoner(sum));
            }
            summonerDataset.addAll(additionalSummoners);
            System.out.println("After iteration " + i + " there are " + summonerDataset.size() + " summoners.");
        }
        games.addAll(getGamesOf5(summonerDataset));
        analyse(games);
        printResults();
    }

    /**
     * Get active flex and clash players in division of seed player.
     * From their clash games, find flex and normal games with players from clash games.
     * return those games, that have 5 clash players.
     */
    static List<Match> getGamesOf5(Set<Summoner> summoners) {
        var premadesPerGame = new HashMap<Long, Integer>();
        summoners.forEach(s -> getMatches(s)
                .forEach(game -> premadesPerGame.put(game.getId(),
                        premadesPerGame.containsKey(game.getId())
                                ? premadesPerGame.get(game.getId()) + 1 : 1)));
        summonerDataset.addAll(summoners);
        return premadesPerGame.entrySet().stream().filter(entry -> entry.getValue() == 5).map(entry -> Match.withId(entry.getKey()).get()).collect(toList());
    }

    private static HashSet<Summoner> exploreSummoner(Summoner summoner) {
        var flexLeague = summoner.getLeague(Queue.RANKED_FLEX);
        if (flexLeagues.contains(flexLeague.getId())) {
            System.out.println("Flex League skipped");
            return new HashSet<>();
        }
        flexLeagues.add(flexLeague.getId());
        var flexSummoners = flexLeague.stream().filter(s -> s.getLosses() + s.getWins() > FLEX_GAMES).map(s -> s.getSummoner()).collect(toList());
        System.out.println(flexSummoners.size());
        var clashSummoners = flexSummoners.stream().filter(s -> {
            League league = s.getLeague(Queue.RANKED_SOLO);
            if (league == null) {
                return true;
            }
            if (league.getTier() == null) {
                return true;
            }
            return league.getTier().compare(Tier.DIAMOND) < 0;
        }).filter(s -> s.matchHistory().withQueues(Queue.CLASH).get().size() > CLASH_GAMES).collect(toList());
        System.out.println(clashSummoners.size());

        var summoners = new HashSet<Summoner>();
        for (var currentPlayer: clashSummoners) {
            System.out.println("Current: "+ currentPlayer.getName());
            var localSummoners = new HashSet<Summoner>();
            localSummoners.add(currentPlayer);
            for (var clashGame: currentPlayer.matchHistory().withQueues(Queue.CLASH).get()) {
                var team = clashGame.getBlueTeam().getParticipants().contains(currentPlayer)
                        ? clashGame.getBlueTeam().getParticipants()
                        : clashGame.getRedTeam().getParticipants();
                for (var sum: team) {
                    var s = sum.getSummoner();
                    if (localSummoners.contains(s)) continue;
                    localSummoners.add(s);
                    System.out.println("Local: "+s.getName());
                }
            }
            summoners.addAll(localSummoners);
        }
        return summoners;
    }

    private static MatchHistory getMatches(Summoner s) {
        return s.matchHistory().withQueues(Queue.RANKED_FLEX, Queue.NORMAL, Queue.CLASH).withEndIndex(100).get();
    }

    static void analyse(Set<Match> matches) {
        wins = new HashMap<>();
        total = new HashMap<>();
        Orianna.getChampions().forEach( c -> {
            wins.put(c.getName(), 0);
            total.put(c.getName(), 0);
        });
        for (var match: matches) {
            var blue = match.getBlueTeam();
            var red = match.getRedTeam();
            if (blue.isWinner()) {
                blue.getParticipants().forEach(incrementTable(wins));
            } else {
                red.getParticipants().forEach(incrementTable(wins));
            }
            match.getParticipants().forEach(incrementTable(total));
        }
    }

    private static Consumer<Participant> incrementTable(HashMap<String, Integer> map) {
        return p -> {
            var champion = p.getChampion();
            if (champion == null) {
                System.out.println("champ not found");
            } else if (!champion.exists()) {
                System.out.println("champ "+champion.getName()+" doesn't exist");
            } else if (!map.containsKey(champion.getName())) {
                System.out.println("Map didn't contain champ "+champion.getName());
            } else {
                map.put(champion.getName(), map.get(champion.getName()) + 1);
            }
        };
    }

    static void printResults() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        var bw = new BufferedWriter(new FileWriter("winrates_5er" + now.getHour() + "_" + now.getMinute() + ".csv"));
        Orianna.getChampions().forEach( c -> {
            var perc = (wins.get(c.getName()) / (double) total.get(c.getName()));
            System.out.println(c.getName()+" "+perc+" "+total.get(c.getName()));
            try {
                bw.write(c.getName() + "," + perc + "," + total.get(c.getName()) + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        bw.close();
    }

}






