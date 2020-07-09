import com.merakianalytics.orianna.Orianna;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Used to build a string to send in discord.
 */
class MyMessage {

    // TODO: time is displayed wrong.
    public static final String DATE_PATTERN = "EE ee. MM. yyyy";
    private Manager manager;
    private StringBuilder sb = new StringBuilder();
    private static final int MONTHS_IN_THE_PAST = 3;

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
                .toLowerCase().trim();
    }

    MyMessage(String command_input, Manager manager) {
        this.manager = manager;

        var tokens = command_input.trim().split(" ");
        if (tokens.length < 2) {
            sb.append("Expected at least one summoner analyse.\n");
            return;
        }

        boolean startTimeSet = false;

        List<Queue> queues = new ArrayList<>();
        List<Champion> champions = new ArrayList<>();
        List<Summoner> summoners;

        try {
            summoners = new ArrayList<>(parseSummoners(tokens[1]));
        } catch (InputError e) {
            sb.append(e.error);
            return;
        }

        DateTime startDate = DateTime.now();
        for (int index = 2; index < tokens.length; index++) {
            try {
                switch (tokens[index]) {
                    default: {
                        // TODO:
                        System.out.println("unexpected token:");
                        sb.append("unexpected Token at index: ").append(index).append(" ").append(tokens[index]);
                        break;
                    }
                    case "-t": {
                        index++;
                        if (index >= tokens.length) break;
                        startDate = parseTime(tokens[index]);
                        startTimeSet = true;
                        break;
                    }
                    case "-w": { // with SUMMONER
                        index++;
                        if (index >= tokens.length) break;
                        summoners.addAll(parseSummoners(tokens[index]));
                        break;
                    }
                    case "-c": { // with CHAMPION
                        index++;
                        if (index >= tokens.length) break;
                        champions.addAll(parseChamps(tokens[index]));
                        break;
                    }
                    case "-q": { // with QUEUE
                        index++;
                        if (index >= tokens.length) break;
                        queues.addAll(parseQueues(tokens[index]));
                        break;
                    }
                }
            } catch (InputError e) {
                sb.append(e.error);
                System.out.println("End of caught error.");
                return;
            }
        }

        if (startDate == null || !startTimeSet) {
            startDate = getDateMinus(MONTHS_IN_THE_PAST, 1);
        }


        DateTime endDate = DateTime.now();
        System.out.println("Args: sums champs queues "+summoners.size()+" "+champions.size()+" "+queues.size()+
                "\nStart: "+ startDate.toString(DATE_PATTERN)+
                "\nEnd:   "+ endDate.toString(DATE_PATTERN));

        SortedSet<Game> matches = manager.gamesWith(summoners, champions, queues, startDate, endDate);
        if (matches == null || matches.size() == 0) {
            System.out.println("wtf. No games found");
            sb.append("No games found.\n");
            return;
        }
        sb.append(stringOf(matches));
    }

    public MyMessage(Manager manager) {
        this.manager = manager;
    }

    private static List<Queue> parseQueues(String token) throws InputError {
        var result = new ArrayList<Queue>();
        for (var q: token.split(",")) {
            try {
                var queue = Queue.valueOf(q.toUpperCase());
                System.out.println("Parsed Queue "+ queue.name());
                result.add(queue);
            } catch (IllegalArgumentException e) {
                throw new InputError(q+" did not match a queue.\n");
            }
        }
        return result;
    }

    private static List<Champion> parseChamps(String token) throws InputError {
        var result = new ArrayList<Champion>();
        for (var c: token.split(",")) {
            Champion champion;
            if (champs.containsKey(c.toLowerCase())) {
                champion = champs.get(c.toLowerCase());
            } else if (champs.containsKey(c.toLowerCase().substring(0, 4))) {
                champion = champs.get(c.toLowerCase().substring(0, 4));
            } else {
                champion = champs.get(c.toLowerCase().substring(0, 3));
            }
            if (champion == null || !champion.exists()) {
                throw new InputError(c+" didn't match a champion.\n");
            } else {
                System.out.println("Parsed "+c);
                result.add(champion);
            }
        }

        return result;
    }

    private static List<Summoner> parseSummoners(String token) throws InputError {
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

    private DateTime parseTime(String s) {
        if (s.endsWith("m")) {
            DateTime date;
            try {
                String sub = s.substring(0, s.length()-1);
                date = getDateMinus(Integer.parseInt(sub), 6);
                System.out.println("Parsed "+date.toString(DATE_PATTERN));
                return date;
            } catch (Exception e) {
                e.printStackTrace();
                throw new InputError("Input was not a number.\n");
            }
        }
        return null;
    }

    /**
     * Returns the date from now - monthDelta with weekday = day
     * @param monthDelta how much months in the past the time should be.
     * @param day day of week the date is set to
     * @return Date.now() - monthDelta and weekday = day
     */
    private DateTime getDateMinus(int monthDelta, int day) {
        var time = DateTime.now();
        int x = time.getMonthOfYear() - monthDelta;
        if (x < 0) {
            time = time.withYear(time.getYear()-1);
        }
        var index = (x + 12) % 12 + 1;
        var time2 = time.withMonthOfYear(index);
        return time2.withDayOfWeek(day);
    }

    private DateTime getMonday(Day[] week) {
        DateTime result = DateTime.now();
        for (Day d: week) {
            if (d == null || d.matches == null || d.matches.size() == 0)
                continue;
            result = d.matches.get(0).time;
        }
        return result.withDayOfWeek(6);
    }

    private String weeksToString(List<Day[]> weeks) {
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
                    sb.append(asString());
                }
                sbInfo.append(asString());
                sbSum.append(asString());
                continue;
            }
            int c = 0;
            for (int i = 0; i < week.length; i++) {
                var day = week[i];

                if (day == null) {
                    dailySBs.get(i).append(asString());
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    dailySBs.get(i).append(asString());
                } else {
                    dailySBs.get(i).append(asString(tmp, 4));
                }
                c += tmp;
            }
            try {
                // TODO: test kw display
                //time = week[0].matches.get(0).time;
                //"KW " +
                //time.toString(DATE_PATTERN);
                String date = time.getWeekOfWeekyear() + " "; //+ time.yearOfCentury().get();
                sbInfo.append(asString(date, 4));
                sbSum.append(asString(c, 4));
                time = time.plusWeeks(1);
            } catch (Exception e) {
                sbInfo.append(c);
            }
        }
        var result = new StringBuilder();
        result.append(sbInfo.toString()).append("\n");
        for (var sbN : dailySBs)
            result.append(sbN.toString()).append("\n");
        result.append(sbSum.toString()).append("\n");
        return result.toString();
    }

    private String asString() {
        return asString("", 4);
    }

    public static String asString(Object a, final int size) {
        if (a == null) a = "";
        String result = a.toString();
        while (result.length() < size) {
            result += " ";
        }
        return result.substring(0, size);
    }

    /**
     * Takes a set of games and returns their string presentation
     * @param games a Set of Games to be displayed
     * @return the string representation
     */
    private String stringOf(SortedSet<Game> games) {
        var sb = new StringBuilder();
        List<Day[]> weeks = Manager.gamesByWeek(games);
        sb.append(weeksToString(weeks));

        var avg = Manager.totalGamesPerDay(games);
        for (var a: avg) {
            sb.append(asString(a, 4));
        }
        sb.append("\n");
        return sb.toString();
    }

    String[] build() {
        return sb.toString().split("\\n");
    }

    public String[] clash(String input) {
        System.out.println("pre clash");
        List<Summoner> summoners = null;
        try {
            summoners = parseSummoners(input.split(" ")[1]);

        } catch (InputError e) {
            return new String[]{e.error};
        }
        System.out.println("pre do clash");
        return manager.doStuffWithClash(summoners);
    }
}
