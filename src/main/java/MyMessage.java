import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Used to build a string to send in discord.
 */
class MyMessage {

    // TODO: time is displayed wrong.
    public static final String DATE_PATTERN = "EE ee. MM. yyyy";
    private Manager manager;
    private StringBuilder sb = new StringBuilder();
    private static final int MONTHS_IN_THE_PAST = 3;

    MyMessage(String command_input, Manager manager) {
        this.manager = manager;

        var tokens = command_input.trim().split(" ");
        if (tokens.length < 2) {
            sb.append("Expected at least one summoner name.\n");
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

        if (startDate == null || !startTimeSet)
            startDate = getDateMinus(MONTHS_IN_THE_PAST, 1);

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

    private List<Queue> parseQueues(String token) throws InputError {
        var result = new ArrayList<Queue>();
        for (var q: token.split(",")) {
            try {
                var queue = Queue.valueOf(q.toUpperCase());
                System.out.println("Parsed Queue "+ queue.name());
                result.add(queue);
            } catch (IllegalArgumentException e) {
                throw new InputError("Input did not match queuetypes.\n");
            }
        }
        return result;
    }

    private List<Champion> parseChamps(String token) throws InputError {
        var result = new ArrayList<Champion>();
        for (var c: token.split(",")) {
            var cname = c.substring(0, 1).toUpperCase() + c.substring(1).toLowerCase();
            var champion = Champion.named(cname).get();
            if (champion == null || !champion.exists()) {
                throw new InputError("Input didn't match a champion.\n");
            } else {
                System.out.println("Parsed "+c);
                result.add(champion);
            }
        }

        return result;
    }

    private List<Summoner> parseSummoners(String token) throws InputError {
        var result = new ArrayList<Summoner>();
        for (var s: token.split(",")) {
            var summoner = Summoner.named(s).get();
            if (summoner == null || !summoner.exists()) {
                throw new InputError("Input didn't match a summoner.\n");
            } else {
                System.out.println("Parsed "+s);
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
                System.out.println("Parsed "+sub);
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
        time = time.withMonthOfYear(time.getMonthOfYear() - monthDelta);
        return time.withDayOfWeek(day);
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
                    dailySBs.get(i).append(asString(tmp));
                }
                c += tmp;
            }
            try {
                // TODO: test kw display
                //time = week[0].matches.get(0).time;
                //"KW " +
                //time.toString(DATE_PATTERN);
                String date = time.getWeekOfWeekyear() + " "; //+ time.yearOfCentury().get();
                sbInfo.append(asString(date));
                sbSum.append(asString(c));
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
        return asString("");
    }

    private String asString(Object a) {
        final int size = 4;
        if (a == null) a = "";
        // TODO: verify cast to String
        return (a.toString() + "    ").substring(0, size);
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
            sb.append(asString(a));
        }
        sb.append("\n");
        return sb.toString();
    }

    String[] build() {
        return sb.toString().split("\\n");
    }

    public String[] clash(String input) {
        System.out.println("pre clash");
        var summoners = parseSummoners(input);
        System.out.println("pre do clash");
        manager.doStuffWithClash(summoners);
        System.out.println("post clash");
        var bans = manager.bans;
        return bans.toArray(new String[0]);
    }
}
