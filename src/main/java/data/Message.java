package data;

import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import data.Day;
import data.Manager;
import data.Player;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Used to build a string to send in discord.
 */
public class Message {

    String summoner, champ;
    Manager manager;
    StringBuilder sb;
    boolean inputError;

    public Message(String input, Manager manager) {
        System.out.println("hello123");
        sb = new StringBuilder();
        inputError = false;

        var tmp = input.split(" ");
        if (tmp.length < 2) {
            sb.append("Invalid syntax.\n");
            inputError = true;
            return;
        }
        summoner = tmp[1];
        Summoner sum = Summoner.named(summoner).get();
        if (sum == null || !sum.exists()) {
            sb.append("Summoner named "+summoner+" wasn't found in euw.\n");
            inputError = true;
            return;
        }
        System.out.println("Summoner "+summoner +" is "+sum.exists());

        sb.append(historyAvg(sum));
       /*
       sb.append(historyAvg(summoner, champ));
       */
        System.out.println("a " +sb.toString());
        //if (tmp[2].equalsIgnoreCase("with"))
        //    champ = tmp[2];
    }

    private String weeksToString(List<Day[]> weeks) {
        var sbInner = new StringBuilder();
        for (var week: weeks) {
            if (week == null) {
                sbInner.append("Empty week");
                continue;
            }
            int c = 0;
            for (Day day: week) {
                if (day == null) {
                    sbInner.append("\t");
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    sbInner.append("\t");
                } else {
                    sbInner.append(asString(tmp));
                }
                c += tmp;
            }
            try {
                var time = week[0].matches.get(0).time;
                String date = "KW " + time.weekyear().get() + " " + time.yearOfCentury().get();
                sbInner.append(asString(c)+date+"\n");

            } catch (Exception e) {
                sbInner.append(c+"\n");
            }
        }
        return sbInner.toString();
    }

    private String asString(int a) {
        return (a+"    ").substring(0, 3);
    }

    public String historyAvg(Summoner sum) {
        var player = new Player(sum, manager);
        System.out.println("player build.");

        DateTime date1 = DateTime.now().withMonthOfYear(8);
        var subset = manager.gamesSince(date1, player.matches);
        List<Day[]> weeks = Manager.gamesByWeek(subset);
        sb.append(weeksToString(weeks));

        var avg = Manager.totalGamesPerDay(subset);
        for (var a: avg) {
            sb.append(asString(a));
        }
        sb.append("\n");
        System.out.println("end of history");
        return sb.toString();
    }

    public String historyAvg(String summoner, String champ) {
        StringBuilder sb = new StringBuilder();
        var tmp = Champion.named(champ).get();
        if (tmp == null || !tmp.exists()) {

        }
        return sb.toString();
    }

    public String build() {
        System.out.println("did build");
        StringBuilder sb2 = new StringBuilder();
        sb2.append("```");
        if (!inputError) {
            sb2.append("Matches per day for "+summoner+":\n");
            sb2.append("Mo  Tu  We  Th  Fr  Sa  Su  sum  week\n");
        }
        System.out.println(sb.toString());
        sb2.append(sb.toString());
        sb2.append("```");
        return sb2.toString();
    }
}
