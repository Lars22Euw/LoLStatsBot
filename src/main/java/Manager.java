import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.common.Region;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.io.*;
import java.util.*;

import static com.merakianalytics.orianna.types.core.match.MatchHistory.*;

public class Manager {

    List<User> users = new ArrayList<>();
    SortedSet<Game> games = new TreeSet<>();
    List<Player> summonersActive = new ArrayList<>();
    List<String> summonersInactive = new ArrayList<>();

    private Manager (String filename, String apiKey) {
        this(apiKey);
        try {
            var br = new BufferedReader(new FileReader(filename));
            while (br.ready()) {
                String l = br.readLine();
                var summoners = new ArrayList<>(List.of(l.split("-")));
                var name = summoners.remove(0);
                List<Summoner> sums = new ArrayList<>();
                for (String s: summoners) {
                    var sum = Summoner.named(s).get();
                    if (sum == null || !sum.exists()) {
                        System.out.println("Could not find summoner: "+s);
                    } else {
                        sums.add(sum);
                    }
                }
                users.add(new User(name, sums, this));
            }
            System.out.println("Found " + users.size() + " users.");
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("File error.");
        }
    }

    Manager(String apiKey) {
        Orianna.setRiotAPIKey(apiKey);
        Orianna.setDefaultRegion(Region.EUROPE_WEST);
    }

    public static void main(final String[] args) throws InterruptedException {
        var rd = new RankDistribution(new Manager(args[0]), "gameIds.txt");

        rd.extractPlayers();
        //rd.summoners.add(Summoner.named("TheLars22").get());
        //rd.summoners.add(Summoner.named("TheNonamed").get());
        rd.sortByRankedGames();
        rd.printBuckets();


        for (int a: List.of(0, 5, 9, 10, 11, 29, 30, 31, 71)) {
            //System.out.println(a +" "+rd.getBucketIndex(a));
        }
        /*var m = new Manager("names-test.txt", args[0]);
        int totalSummoners = m.summonersActive.size() + m.summonersInactive.size();
        System.out.println("Found "+totalSummoners+" summoners, "+m.summonersInactive.size()+" inactive.");
        System.out.println("Found "+ m.games.size()+" games in total.");

        var playedWith = m.matrix();
        //m.printMatrix(playedWith);
        m.writeGEXF(playedWith);
        var user = m.users.get(0);
        //System.out.println("User: "+user.name);
        displayGames(gamesByWeek(user.matches));
        //System.out.println(user.matches.last().time.toString());
        int[] avgGamesDay = new int[7];
        for (User u: m.users) {
            var t = totalGamesPerDay(u.matches);
            //if (t == null) continue;
            for (int i= 0; i < 7; i++) {
                avgGamesDay[i] += t[i];
            }
        }
        printAvg(avgGamesDay);*/
    }

    SortedSet<Game> gamesWith(List<Champion> champions, Summoner summoner) {
        SortedSet<Game> matches = new TreeSet<>(Game::compare2);
        if (champions == null || champions.size() == 0) {
            return new Player(summoner, this).matches;
        }
        for (var champ: champions) {
            for (var m: forSummoner(summoner).withChampions(champ).get()) {
                matches.add(new Game(m));
            }
        }
        return matches;
    }

    static SortedSet<Game> gamesSince(DateTime date, SortedSet<Game> matches) {
        if (date == null || date.equals(DateTime.now())) return matches;
        SortedSet<Game> m2 = new TreeSet<>(Game::compare2);
        for (Game game: matches) {
            if (game.time.isAfter(date))
                m2.add(game);
        }
        return m2;
    }

    static  SortedSet<Game> gamesBefore(DateTime date, SortedSet<Game> matches) {
        if (date == null || date.equals(DateTime.now())) return matches;
        SortedSet<Game> m2 = new TreeSet<>(Game::compare2);
        for (Game game: matches) {
            if (game.time.isBefore(date))
                m2.add(game);
        }
        return m2;
    }

    static int[] totalGamesPerDay(SortedSet<Game> matches) {
        List<Day[]> weeks = gamesByWeek(matches);
        int[] avgGpDay = new int[7];
        for (var week: weeks) {
            for (int i = 0; i < 7; i++) {
                if (week == null || week[i] == null || week[i].matches == null) continue;
                avgGpDay[i] += week[i].matches.size();
            }
        }
        return avgGpDay;
    }

    static void printAvg(int[] avgDays) {
        if (avgDays == null) return;
        for (var a: avgDays) {
            //if (a == null) continue;
            System.out.print(a+"\t");
        }
    }

    static SortedSet<Day> gamesPerDay(SortedSet<Game> matches) {
        SortedSet days = new TreeSet<Day>();

        if (matches == null || matches.size() == 0) {
            return days;
        }

        var start = matches.first().time;
        DateTime nextDay = start.plusDays(1).withHourOfDay(6);
        Day day = new Day(nextDay.minusDays(1));

        for (Game m: matches) {
            while (!m.time.isBefore(nextDay)) {
                nextDay = nextDay.plusDays(1);
                days.add(day);
                day = new Day(nextDay);
            }
            day.matches.add(m);
        }
        days.add(day);
        return days;
    }

    /**
     * Adds all games that are played within one day into a list.
     * Then add all day-lists into a total list.
     */
    static List<Day[]> gamesByWeek(SortedSet<Game> matches) {
        SortedSet<Day> days = Manager.gamesPerDay(matches);
        System.out.println(days.last().time.toString());

        Day[] week = new Day[7];
        List<Day[]> listOfWeeks = new ArrayList<>();

        /*if (days == null || days.size() == 0)
            return listOfWeeks;
        */
        var start = days.first().matches.get(0).time;
        int dayIndex = ((start.dayOfWeek().get() -1 ) % 7 + 7) % 7;

        for (var day : days) {
            /*if (day == null || day.matches.size() == 0) {
                // TODO: doStuff
                if (dayIndex < 0 || dayIndex > 6) {
                    System.out.println("dayIndex was "+dayIndex);
                    dayIndex = 0;
                }
                week[dayIndex] = day;
                dayIndex++;
                continue;
            }*/
            if (dayIndex > 6) {
                // start new week;
                listOfWeeks.add(week);
                week = new Day[7];
                dayIndex = 0;
            }
            week[dayIndex] = day;
            dayIndex++;
        }
        listOfWeeks.add(week);

        return listOfWeeks;
    }

    private static void displayGames(List<Day[]> listOfWeeks) {
        System.out.println("\nStarting History");
        System.out.println("Mo, Di, Mi, Do, Fr, Sa, So");
        for (var week: listOfWeeks) {
            if (week == null || week.length == 0) {
                System.out.println("empty week");
                continue;
            }
            for (var day: week) {
                if (day == null || day.matches.size() == 0) System.out.print("\t");
                else System.out.print(day.matches.size()+"\t");
            }
            try {
                System.out.println("; "+week[0].matches.get(0).time.toString());
            } catch (Exception e) {
                System.out.println(";");
            }
        }
        System.out.println("End of history");
    }

    public void printMatrix(int[][] playedWith) {
        // TODO: Assumes player has < 10.000 games per entry (tab size  = 4)
        StringBuilder sb = new StringBuilder();
        sb.append("\t");
        for (var user: users)
            sb.append(user.name, 0, 3).append("\t");
        System.out.println(sb);

        for (int i = 0; i < users.size(); i++) {
            sb = new StringBuilder();
            sb.append(users.get(i).name, 0, 3).append("\t");
            for (int j = 0; j < users.size(); j++) {
                if ((Integer.toString(playedWith[i][j])).length() >= 4)
                    sb.append(playedWith[i][j]);
                else
                    sb.append(playedWith[i][j]).append("\t");
            }
            System.out.println(sb);
        }
    }

    /**
     * populates users^2 matrix, where each entry represents the games played together.
     * @return matrix with games together.
     */
    private int[][] matrix() {
        int[][] playedWith = new int[users.size()][users.size()];
        for (var g: games) {
            for (User u1: users) {
                if (!u1.matches.contains(g)) continue;

                for (User u2: users) {
                    if (!u2.matches.contains(g)) continue;
                    playedWith[users.indexOf(u1)][users.indexOf(u2)]++;
                }
            }
        }
        return playedWith;
    }

    private void writeGEXF(int[][] playedWith) {
        try {
            var bw = new BufferedWriter(new FileWriter("result.gexf"));
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "\n<gexf xmlns:viz=\"http:///www.gexf.net/1.1draft/viz\" version=\"1.1\" xmlns=\"http://www.gexf.net/1.1draft\">" +
                    "\n\t<meta lastmodifieddate=\"2010-03-03+23:44\">" +
                    "\n\t\t<creator>Gephi 0.7</creator>" +
                    "\n\t</meta>" +
                    "\n\t<graph defaultedgetype=\"undirected\" idtype=\"string\" type=\"static\">" +
                    "\n\t\t<nodes count=\"" + users.size() + "\">\n");
            for (int i = 0; i < users.size(); i++) {
                bw.write("\t\t\t<node id=\"" + i + ".0\" label=\"" + users.get(i).name + "\">" +
			    "\n\t\t\t\t<viz:size value=\""+ users.get(i).matches.size()+".0\"></viz:size>" +
			    "\n\t\t\t</node>\n");
                //      <node id="0.0" label="Lars">
                //          <viz:size value="10.0"></viz:size>
                //      </node>
            }

            StringBuilder sb = new StringBuilder();
            int edges = getEdges(playedWith, sb);

            bw.write("\t\t</nodes>\n" +
                    "\t\t<edges count=\""+(edges+1)+"\">\n");
            bw.append(sb);
            bw.write("\t\t</edges>\n" +
                    "\t</graph>\n" +
                    "</gexf>");
            bw.close();
        } catch (Exception e) {
            System.out.println("Something went wrong.");
        }
    }

    private int getEdges(int[][] playedWith, StringBuilder sb) {
        int edges = 0;
        for (int i = 0; i < playedWith.length; i++) {
            for (int j = i+1; j < playedWith[i].length; j++) {
                if (playedWith[i][j] == 0) continue;
                sb.append("\t\t\t<edge id=\"").append(edges).append("\" ").append("source=\"").append((float) i).append("\" ").append("target=\"").append((float) j).append("\" ").append("weight=\"").append((float) playedWith[i][j]).append("\"/>\n");
        // \t\t\t<edge id="0" source="0.0" target="1.0" weight="14.0"/>
                edges++;
            }
        }
        return edges;
    }

    SortedSet<Game> withQueues(List<Queue> queues, SortedSet<Game> matches) {
        if (queues == null || queues.size() == 0) return matches;
        SortedSet<Game> result = new TreeSet<>(Game::compare2);
        for (var game : matches)
            for (var q : queues) {
                if (game.queue.equalsIgnoreCase(q.name()))
                    result.add(game);
            }
        return result;
    }

    public SortedSet<Game> gamesWith(List<Summoner> summoners, List<Champion> champions, List<Queue> queues, DateTime startDate, DateTime endDate) {
        SortedSet<Game> result = new TreeSet<>(Game::compare2);

        if (summoners == null || summoners.size() == 0) return result;
        result = gamesWith(champions, summoners.remove(0));

        if (summoners.size() >= 1) {
            result = gamesWithPlayers(summoners, champions, result);
        }

        result = Manager.gamesSince(startDate, result);
        result = Manager.gamesBefore(endDate, result);
        result = withQueues(queues, result);

        return result;
    }

    /**
     * takes a list of summoners, where all their games with the same id as the given matches list will be returned
     * @param summoners list of summoners to compare with
     * @param champions
     * @param matches
     * @return games played together by all players.
     */
    private SortedSet<Game> gamesWithPlayers(List<Summoner> summoners, List<Champion> champions, SortedSet<Game> matches) {
        if (summoners == null || summoners.size() == 0) return matches;

        SortedSet<Long> ids = new TreeSet();
        for (var game: matches) {
            ids.add(game.id);
        }
        SortedSet<Game> gamesTogether = new TreeSet();
        System.out.println("Games together:"+ids.size());

        ids.forEach(System.out::println);

        for (var game: gamesWith(champions, summoners.remove(0))) {
            if(ids.contains(game.id)) {
                gamesTogether.add(game);
            }
        }
        return gamesWithPlayers(summoners, champions, gamesTogether);
    }

    public void doStuffWithClash(List<Summoner> summoners) {
        var clashPlayers = new ArrayList<ClashPlayer>();
        for (var s : summoners) {
            clashPlayers.add(new ClashPlayer(s));
        }




        System.out.println(forSummoner(Summoner.named("TheLars22").get()).withStartIndex(0).withEndIndex(100).get().size());
    }
}
