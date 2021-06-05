package bot;

import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import util.Triple;

public class WinData<T> extends Triple<T, Integer, Integer> {

    public WinData(T t, Integer wins, Integer games) {
        super(t, wins, games);
    }

    public T getKey() {
        return first;
    }

    public Integer getWins() {
        return second;
    }

    public Integer getGames() {
        return third;
    }

    public double getRatio() {
        return (double) second / third;
    }

    // TODO: Check if abstract implementation could make this more clean
    public String getLabel() {
        String key = "";
        if (first instanceof Champion) {
            key = ((Champion) first).getName();
        } else if (first instanceof Queue) {
            key = first.toString();
        } else if (first instanceof StalkRole) {
            key = first.toString();
        } else {
            throw new UnsupportedOperationException();
        }
        return key + " " + getWins() +  "W/" + (getGames() - getWins()) + "L (" + Math.round(getRatio() * 100) + "%)";
    }

    public String getImageName() {
        if (first instanceof StalkRole) {
            return queueImage((StalkRole) first, "Diamond");
        } else {
            return null;
        }
    }

    private static String queueImage(StalkRole role, String ranking) {
        final var roleName = role.toString();
        return String.format("ranked-position-icons/Position_%s-%s.png", ranking, roleName.charAt(0) + roleName.substring(1).toLowerCase());
    }
}
