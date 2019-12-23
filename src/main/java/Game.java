import com.merakianalytics.orianna.types.core.match.Match;
import org.joda.time.DateTime;

public class Game implements Comparable<Game> {
    public long id;
    DateTime time;
    String queue;

    public Game(Match match) {
        if (match == null) {
            id = 0;
            time = null;
            queue = "";
        }
        this.id = match.getId();
        this.time = match.getCreationTime();
        if (match.getQueue() != null)
            this.queue = match.getQueue().toString();
    }

    public Game(String line) {
        var tmp = line.split(", ");
        if (tmp == null || tmp.length == 0)
            new Game((Match) null);
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
