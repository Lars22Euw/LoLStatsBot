package data;

import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Used to build a string to send in discord.
 */
class Message {

    private String summoner, champ;
    private Manager manager;
    private StringBuilder sb;
    private DateTime date;
    private boolean inputError;
    private static final int SUBSET = 3;

    Message(String input, Manager manager) {
        this.manager = manager;
        sb = new StringBuilder();
        inputError = false;

        date = DateTime.now();
        date = date.withMonthOfYear( (date.getMonthOfYear() - SUBSET) % 12);

        var tmp = input.split(" ");
        if (tmp.length < 2) {
            sb.append("Invalid syntax.\n");
            inputError = true;
            return;
        }
        summoner = tmp[1];
        Summoner sum = Summoner.named(summoner).get();
        if (sum == null || !sum.exists()) {
            sb.append("Summoner named ").append(summoner).append(" wasn't found in euw.\n");
            inputError = true;
            return;
        }
        //System.out.println("Summoner "+summoner +" is "+sum.exists());

        sb.append(historyAvg(sum));


        System.out.println("a \n" +sb.toString());
        if (tmp[2].equalsIgnoreCase("with")) {
            champ = tmp[2];
        }
        var tmp2 = Champion.named(champ).get();
        if (tmp2 == null || !tmp2.exists()) {
            System.out.println("hello champ");
            sb.append(historyAvg(sum, tmp2));
        }
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
        var sbInfo = new StringBuilder();
        var sbSum = new StringBuilder();
        var listOfSB = new ArrayList<StringBuilder>();
        for (int i = 0; i < 7; i++) {
            listOfSB.add(new StringBuilder());
        }

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
                    sb.append("   ");
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    listOfSB.get(i).append("   ");
                } else {
                    listOfSB.get(i).append(asString(tmp));
                }
                c += tmp;
            }
            try {
                // TODO: test kw display
                //time = week[0].matches.get(0).time;
                //"KW " +
                String date = time.getWeekOfWeekyear()+1 + " "; //+ time.yearOfCentury().get();
                sbInfo.append(asString(date));
                sbSum.append(asString(c));
                time = time.plusWeeks(1);
            } catch (Exception e) {
                sbInfo.append(c);
            }
        }
        var result = new StringBuilder();
        result.append(sbInfo.toString()).append("\n");
        for (var sbN : listOfSB)
            result.append(sbN.toString()).append("\n");
        result.append(sbSum.toString()).append("\n");
        return result.toString();
    }

    private String asString(Object a) {
        // TODO: verify cast to String
        return (a +"    ").substring(0, 3);
    }

    private String historyAvg(Summoner sum) {
        var player = new Player(sum, manager);
        System.out.println("player build.");

        var subset = Manager.gamesSince(date, player.matches);
        return stringOf(subset);
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
        System.out.println("end of history");
        return sb.toString();
    }

    private String historyAvg(Summoner sum, Champion champ) {
        var list = manager.gamesWith(champ, sum);
        var subset = Manager.gamesSince(date, list);
        return stringOf(subset);
    }

    String build() {
        System.out.println("did build");
        StringBuilder sb2 = new StringBuilder();

        if (!inputError) {
            sb2.append("Matches per day for ").append(summoner).append(":\n");
            sb2.append("Mo  Tu  We  Th  Fr  Sa  Su  sum  week\n");
        }
        System.out.println(sb.toString());
        sb2.append(sb.toString());

        return sb2.toString();
    }
}
