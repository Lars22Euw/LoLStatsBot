package bot;

public class Pair implements Comparable<Pair> {
    public int wins, games;

    public Pair(boolean winning) {
        this.wins = winning ? 1 : 0;
        this.games = 1;
    }

    public void add(boolean winning) {
        this.games += 1;
        this.wins += winning ? 1 : 0;
    }

    @Override
    public int compareTo(Pair o) {
        var g = this.games - o.games;
        return (g != 0) ? g : this.wins - o.wins;
    }
}