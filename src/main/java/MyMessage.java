import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.match.MatchHistory;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.*;

/**
 * Used to build a string to send in discord.
 */
class MyMessage {

    private Manager manager;
    StringBuilder sb = new StringBuilder();
    static final int MONTHS_IN_THE_PAST = 3;

    private static Map<String, String> names = new HashMap<>();
    static {
        try {
            final var br = new BufferedReader(
                    new FileReader("names-lookup.txt"));
            while (br.ready()) {
                String line = br.readLine();
                //"Ben-SattenLink-LarsmonX"
                // TODO: something with smurfs
                var split = line.split("-");
                names.put(split[0], split[1]);
            }
            br.close();
        } catch (FileNotFoundException e) {
            System.out.println("Loaded without names-lookup");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, Champion> champs= new HashMap<>();
    static void setChamps() {
        for (var champ: Champions.get()) {
            var nameOrigin = champ.getName();
            var nameClean = cleanIt(nameOrigin);

            if (nameOrigin.length() <= 4) {
                // VI is evil
                champs.put(nameClean, champ);
                //System.out.println(nameClean + " has a short name");
                continue;
            }
            var name3 = nameClean.substring(0, 3);

            if (champs.containsKey(name3)) {
                // if not unique, extend old and new to 4 letters
                final Champion oldChamp = champs.get(name3);
                //System.out.println(oldChamp.getName() + " matches " + nameClean);
                champs.remove(name3);
                champs.put(cleanIt(oldChamp.getName()).substring(0, 4), oldChamp);
                champs.put(nameClean.substring(0, 4), champ);
            } else champs.put(name3, champ);
            champs.put(nameClean, champ);
        }
        champs.put("mundo", Champion.named("Dr. Mundo").get());
        //for (var c: champs.entrySet()) System.out.println(c.getKey());
    }

    private static String cleanIt(String input) {
        return input.replace(" ", "")
                .replace("'", "")
                .replace(".", "")
                .toLowerCase().trim();
    }

    public MyMessage(Manager manager) {
        this.manager = manager;
    }

    public static List<Queue> parseQueues(String token) throws InputError {
        var result = new ArrayList<Queue>();
        for (var q : token.split(",")) {
            try {
                final String queueName = q.toUpperCase();
                if (queueName.equals("*") || queueName.equals("ALL")) {
                    System.out.println("Parsed all queues.");
                    return new ArrayList<>();
                }
                if (queueName.equals("SR")) {
                    result.addAll(List.of(Queue.NORMAL, Queue.CLASH, Queue.CUSTOM, Queue.BLIND_PICK));
                    result.addAll(Queue.RANKED);
                    System.out.println("Parsed Queues on Summoners Rift.");
                    continue;
                }
                if (queueName.equals("RANKED")) {
                    result.addAll(Queue.RANKED);
                    System.out.println("Parsed Queue Ranked.");
                    continue;
                }
                var queue = Queue.valueOf(queueName);
                System.out.println("Parsed Queue "+ queue.name() + ".");
                result.add(queue);
            } catch (IllegalArgumentException e) {
                throw new InputError(q+" did not match a queue.\n");
            }
        }
        return result;
    }

    static List<Champion> parseChamps(String token) throws InputError {
        var result = new ArrayList<Champion>();
        for (var c: token.split(",")) {
            c = cleanIt(c);
            Champion champion;
            if (champs.containsKey(c)) {
                champion = champs.get(c);
            } else if (champs.containsKey(c.substring(0, 4))) {
                champion = champs.get(c.substring(0, 4));
            } else {
                champion = champs.get(c.substring(0, 3));
            }
            if (champion == null || !champion.exists()) {
                throw new InputError(c+" didn't match a champion.\n");
            } else {
                System.out.println("Parsed "+champion.getName());
                result.add(champion);
            }
        }

        return result;
    }

    public static List<Summoner> parseSummoners(String token) throws InputError {
        var result = new ArrayList<Summoner>();

        for (var s: token.split(",")) {
            if (s.startsWith("\\")) s = s.substring(1);
            else if (names.containsKey(s)) s = names.get(s);
            var summoner = Summoner.named(s).get();
            if (summoner == null || !summoner.exists()) {
                var region = summoner.getRegion().toString();
                throw new InputError(s + " not present in " + region + ".\n");
            } else {
                System.out.println("Parsed summoner "+s);
                result.add(summoner);
            }
        }
        return result;
    }

    static DateTime parseTime(String s) {
        if (s.endsWith("m")) {
            DateTime date;
            try {
                String sub = s.substring(0, s.length()-1);
                date = getDateMinus(Integer.parseInt(sub), 6);
                System.out.println("Parsed "+Util.dtf.print(date));
                return date;
            } catch (Exception e) {
                e.printStackTrace();
                throw new InputError("Input was not a number.\n");
            }
        }
        return null;
    }

    public static String stalk(Arguments args) {
        return stalk(args.summoner, args.gamesTogether, args.queues, args.games);
    }

    public static String stalk(Summoner sum, int gamesTogether, List<Queue> queues, int historySize) {
        historySize = Math.min(historySize, 200);
        StringBuilder output = new StringBuilder();
        MatchHistory games;

        if (queues.size() == 0) {
            games = MatchHistory.forSummoner(sum).withEndIndex(historySize).get();
        } else {
            games = MatchHistory.forSummoner(sum).withQueues(queues).withEndIndex(historySize).get();
        }

        var gamesFiltered = Player.lookup(games, sum).entrySet().stream().
                filter(e -> e.getValue().games > gamesTogether).
                sorted(Entry.comparingByValue()).
                collect(Collectors.toCollection(ArrayList::new));
        var champsFiltered = Player.lookupChamps(games, sum).entrySet().stream().
                filter(e -> e.getValue().games > gamesTogether).
                sorted(Entry.comparingByValue()).
                collect(Collectors.toCollection(ArrayList::new));

        final String d2 = "%02d";
        final String f3_0 = "%3.0f%%";

        String[] resPlayer = new String[gamesFiltered.size()];
        for (int i = 1; i < gamesFiltered.size() && i < 11; i++) {
            var entry = gamesFiltered.get(gamesFiltered.size() -i);
            var name = Summoner.withId(entry.getKey()).get().getName();
            var wins = entry.getValue().wins;
            var total = entry.getValue().games;
            double p = wins / (double) total * 100;
            resPlayer[i] = String.format(d2 +"/" + d2 + "  " + f3_0 + "  %-16s", wins, total, p, name);
        }

        String[] resChamps = new String[champsFiltered.size()];
        for (var i = 1; i < champsFiltered.size() & i < 11; i++) {
            var entry = champsFiltered.get(champsFiltered.size()-i);
            var name = Champion.withId(entry.getKey()).get().getName();
            var wins = entry.getValue().wins;
            var total = entry.getValue().games;
            var p = wins / (double) total * 100;
            resChamps[i] = String.format(d2+"/"+d2+"  "+f3_0+"  %-16s", wins, total, p, name);
        }

        var len = Math.max(Math.min(gamesFiltered.size(), 11), Math.min(champsFiltered.size(), 11));
        String[] res = new String[len-2];
        for (int i = 0; i < res.length; i++) {
            String s = (i+1 < resPlayer.length) ? resPlayer[i+1] : String.format("%-29s", "");
            res[i] = (i+1 < resChamps.length) ? s + " | " + resChamps[i+1] +"\n" : s + "\n";
        }
        for (var s: res) output.append(s);
        return output.toString();
    }


    /**
     * Returns the date from now - monthDelta with weekday = day
     * @param monthDelta how much months in the past the time should be.
     * @param day day of week the date is set to
     * @return Date.now() - monthDelta and weekday = day
     */
    static DateTime getDateMinus(int monthDelta, int day) {
        var time = DateTime.now();
        int x = time.getMonthOfYear() - monthDelta;
        if (x < 0) {
            time = time.withYear(time.getYear()-1);
        }
        var index = (x + 12) % 12;
        var time2 = time.withMonthOfYear(index);
        return time2.withDayOfWeek(day);
    }

    private static DateTime getMonday(Day[] week) {
        DateTime result = DateTime.now();
        for (Day d: week) {
            if (d == null || d.matches == null || d.matches.size() == 0)
                continue;
            result = d.matches.get(0).time;
        }
        return result.withDayOfWeek(6);
    }

    private static String[] weeksToString(List<Day[]> weeks) {
        final var days = List.of("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su");
        var sbInfo = new StringBuilder();
        var sbSum = new StringBuilder();
        var dailySBs = new ArrayList<StringBuilder>();
        for (int i = 0; i < 7; i++) {
            dailySBs.add(new StringBuilder());
            dailySBs.get(i).append(days.get(i)).append(": ");
        }

        sbInfo.append("KW: ");
        sbSum.append("Sum ");
        var time = getMonday(weeks.get(0));


        for (var week: weeks) {
            if (week == null) {
                // TODO: add empty collum to each sb
                for (var sb: dailySBs) {
                    sb.append(Util.asString());
                }
                sbInfo.append(Util.asString());
                sbSum.append(Util.asString());
                continue;
            }
            int c = 0;
            for (int i = 0; i < week.length; i++) {
                var day = week[i];

                if (day == null) {
                    dailySBs.get(i).append(Util.asString());
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    dailySBs.get(i).append(Util.asString());
                } else {
                    dailySBs.get(i).append(Util.asString(tmp, 4));
                }
                c += tmp;
            }
            try {
                // TODO: test kw display
                //time = week[0].matches.get(0).time;
                //"KW " +
                //time.toString(DATE_PATTERN);
                String date = time.getWeekOfWeekyear() + " "; //+ time.yearOfCentury().get();
                sbInfo.append(Util.asString(date, 4));
                sbSum.append(Util.asString(c, 4));
                time = time.plusWeeks(1);
            } catch (Exception e) {
                sbInfo.append(c);
            }
        }

        String[] res = new String[9];
        res[0] = sbInfo.toString().concat("Sum");
        for (int n = 0; n < dailySBs.size(); n++)
            res[n+1] = dailySBs.get(n).toString();
        res[8] = sbSum.toString();
        return res;
    }

    /**
     * Takes a set of games and returns their string representation
     * @param games a Set of Games to be displayed
     * @return the string representation
     */
    static String stringOf(SortedSet<Game> games) {
        var sb = new StringBuilder();
        List<Day[]> weeks = Manager.gamesByWeek(games);
        var lines = weeksToString(weeks);
        var avgs = Manager.totalGamesPerDay(games);
        sb.append(lines[0]).append("\n");
        for (int i = 0; i < avgs.length; i++) {
            var avg = Util.asString(avgs[i], 4);
            sb.append(lines[1+i]).append(avg).append("\n");
        }
        sb.append(lines[8]).append("\n");
        return sb.toString();
    }

    String[] build() {
        return sb.toString().split("\\n");
    }

    public String[] clash(List<Summoner> summoners, boolean image) {
        return manager.doStuffWithClash(summoners, image);
    }

    public String[] clash(Arguments arguments) {
        return clash(arguments.summoners, arguments.image);
    }
}
