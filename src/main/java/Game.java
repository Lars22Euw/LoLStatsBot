import com.merakianalytics.orianna.types.core.match.Match;
import org.joda.time.DateTime;

public class Game implements Comparable<Game> {
    public long id;
    DateTime time;
    String queue;
    Match match;

    public Game(Match match) {
        assert match != null;
        this.match = match;
        this.id = match.getId();
        this.time = match.getCreationTime();
        if (match.getQueue() != null)
            this.queue = match.getQueue().toString();
    }

    public Game(String line) {
        var tmp = line.split(", ");
        if (tmp.length == 0) {
            System.out.println("Some error with game");
            return;
        }
        try {
            this.id = Long.parseLong(tmp[0]);
            this.time = new DateTime(tmp[1]);
            this.queue = tmp[2];
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("Game from file was missing info.");
        }
    }

    String print() {
        return id + ", " + time + ", " + queue;
    }

    @Override
    public int compareTo(Game o) {
        var idDif = this.id - o.id;
        return (int) idDif;
    }

    public int compare2(Game o) {
        return (int) ((this.time.getMillis() - o.time.getMillis())/1000);
    }
}
