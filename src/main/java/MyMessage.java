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

    private Manager manager;
    private StringBuilder sb = new StringBuilder();
    private DateTime startDate = DateTime.now(), endDate = DateTime.now();
    private static final int MONTHS_IN_THE_PAST = 3;

    MyMessage(String input, Manager manager) {
        this.manager = manager;

        var tokens = input.trim().split(" ");
        if (tokens.length < 2) {
            sb.append("Expected at least one summoner name.\n");
            return;
        }

        boolean startTimeSet = false;

        List<Queue> queues = new ArrayList<>();
        List<Champion> champions = new ArrayList<>();
        List<Summoner> summoners = new ArrayList<>();

        try {
            summoners.add(parseSummoner(tokens[1]));
        } catch (InputError e) {
            sb.append(e.error);
            return;
        }

        for (int index = 2; index < tokens.length; index++) {
            try {
                switch (tokens[index]) {
                    default: {
                        // TODO:
                        System.out.println("unexpected token:");
                        sb.append("unexpected Token at index: "+index+" "+tokens[index]);
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
                        summoners.add(parseSummoner(tokens[index]));
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
                        queues.add(parseQueue(tokens[index]));
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
            startDate = setStartDate(MONTHS_IN_THE_PAST, 6);

        System.out.println("Args: sums champs queues "+summoners.size()+" "+champions.size()+" "+queues.size()+
                "\nStart: "+startDate.toString("EE ee. MM. yyyy")+
                "\nEnd:   "+endDate.toString("EE ee. MM. yyyy"));

        SortedSet<Game> matches = manager.gamesWith(summoners, champions, queues, startDate, endDate);
        if (matches == null || matches.size() == 0) {
            System.out.println("wtf. No games found");
            sb.append("No games found.\n");
            return;
        }
        sb.append(stringOf(matches));
    }

    private Queue parseQueue(String token) throws InputError {
        var queue = Queue.valueOf(token.toUpperCase());
        if (queue == null) {
            throw new InputError("Input did not match queuetype.\n");
        }
        System.out.println("Queue input was "+ queue.name());
        return queue;
    }

    private List<Champion> parseChamps(String token) throws InputError {
        var result = new ArrayList<Champion>();
        for (var c: token.split(",")) {
            var champion = Champion.named(token).get();
            if (champion == null || !champion.exists()) {
                throw new InputError("Input didn't match a champion.\n");
            } else {
                System.out.println("hello "+c);
                result.add(champion);
            }
        }

        return result;
    }

    private Summoner parseSummoner(String token) throws InputError {
        Summoner sum = Summoner.named(token).get();
        if (sum == null || !sum.exists()) {
            throw new InputError("Summoner named "+token+" wasn't found in current region.\n");
        }
        System.out.println("Summoner " + token + " exists.");
        return sum;
    }

    private DateTime parseTime(String s) {
        if (s.endsWith("m")) {
            try {
                return setStartDate(Integer.parseInt(s.substring(0, s.length()-1)), 6);
            } catch (Exception e) {
                e.printStackTrace();
                throw new InputError("Number expected.\n");
            }
        }
        return null;
    }

    /**
     * Takes a monthDelta as input, and sets Date to the time deltaMonths in the past.
     * @param monthDelta
     */
    private DateTime setStartDate(int monthDelta, int day) {
        var time = DateTime.now();
        time = time.withMonthOfYear(time.getMonthOfYear() - monthDelta);
        time = time.withDayOfWeek(day);
        return time;
    }

    private DateTime getFirstMonday(Day[] week) {
        DateTime result = DateTime.now();
        for (Day d: week) {
            if (d == null || d.matches == null || d.matches.size() == 0)
                continue;
            result = d.matches.get(0).time;
        }
        return result.withDayOfWeek(6);
    }

    private String weeksToString(List<Day[]> weeks) {
        var days = List.of("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su");
        var sbInfo = new StringBuilder();
        var sbSum = new StringBuilder();
        var dailySBs = new ArrayList<StringBuilder>();
        for (int i = 0; i < 7; i++) {
            dailySBs.add(new StringBuilder());
            dailySBs.get(i).append(days.get(i)).append(": ");
        }

        sbInfo.append("KW: ");
        sbSum.append("Sum ");
        var time = getFirstMonday(weeks.get(0));


        for (var week: weeks) {
            if (week == null) {
                //sb.append("Empty week");
                // TODO: add empty collum to each sb
                continue;
            }
            int c = 0;
            for (int i = 0; i < week.length; i++) {
                var day = week[i];

                if (day == null) {
                    dailySBs.get(i).append(asString("    "));
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    dailySBs.get(i).append(asString("   "));
                } else {
                    dailySBs.get(i).append(asString(tmp));
                }
                c += tmp;
            }
            try {
                // TODO: test kw display
                //time = week[0].matches.get(0).time;
                //"KW " +
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

    private String asString(Object a) {
        // TODO: verify cast to String
        return (a +"    ").substring(0, 4);
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
}
