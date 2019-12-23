import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class Day implements  Comparable<Day> {
    List<Game> matches = new ArrayList<>();
    DateTime time;

    Day(DateTime time) {
        this.time = time;
    }

    @Override
    public int compareTo(Day o) {
        return (int) ((this.time.getMillis() - o.time.getMillis())/1000);
    }
}
