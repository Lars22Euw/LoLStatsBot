package bot;

import util.Triple;
import util.UPair;

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
}
