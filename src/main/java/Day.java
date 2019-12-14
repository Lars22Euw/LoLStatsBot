import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class Day implements  Comparable<Day> {
    List<Game> matches = new ArrayList<>();
    DateTime time;

    Day(DateTime time) {
        this.time = time.minusDays(1);
    }

    @Override
    public int compareTo(Day o) {
        var t = time.minus(o.time.getMillis());
        return t.getMillis() < 0 ? -1 : 1;
    }
}
