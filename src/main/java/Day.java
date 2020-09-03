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
        final long mills = this.time.getMillis() - o.time.getMillis();
        return (int) (mills /1000);
    }
}
