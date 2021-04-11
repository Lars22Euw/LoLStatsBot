package bot;

import com.merakianalytics.orianna.types.core.staticdata.Champion;
import util.Triple;
import util.UPair;

import java.util.Queue;

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
        return key + " " + getWins() +  "/" + getGames() + " (" + Math.round(getRatio() * 100) + "%)";
    }
}
