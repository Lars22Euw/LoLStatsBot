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

    public Message(String input) {
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
        sb.append(historyAvg(summoner));
       /*
       sb.append(historyAvg(summoner, champ));
       */
        System.out.println("a " +sb.toString());
        //if (tmp[2].equalsIgnoreCase("with"))
        //    champ = tmp[2];
    }

    public String historyAvg(String summoner) {
        Summoner sum = Summoner.named(summoner).get();
        if (sum == null || !sum.exists())
            return "Summoner named "+summoner+" wasn't found in euw.\n";

        System.out.println("Summoner named: "+summoner +" was "+sum.exists());

        var player = new Player(sum, manager);

        System.out.println("player build.");

        DateTime date1 = DateTime.now().withMonthOfYear(8);
        var subset = manager.gamesSince(date1, player.matches);
        List<Day[]> weeks = Manager.gamesByWeek(subset);

        for (var week: weeks) {
            if (week == null) {
                sb.append("Empty week");
                continue;
            }
            int c = 0;
            for (Day day: week) {
                if (day == null) {
                    sb.append("\t");
                    continue;
                }
                int tmp = day.matches.size();
                if (tmp == 0) {
                    sb.append("\t");
                } else {
                    String s = tmp +"    ";
                    s = s.substring(0, 4);
                    sb.append(s);
                }
                c += tmp;
            }
            try {
                var time = week[0].matches.get(0).time;
                String weekSum = (c +"    ").substring(0, 4);
                String date = time.dayOfMonth().get()+"."+time.monthOfYear().get()+"."+time.yearOfCentury().get();
                sb.append(weekSum+date+"\n");

            } catch (Exception e) {
                sb.append(c+"\n");
            }
        }
        var avg = Manager.totalGamesPerDay(player.matches);
        for (var a: avg) {
            String s = (a+"    ").substring(0, 4);
            sb.append(s);
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
