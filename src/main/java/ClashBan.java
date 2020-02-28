import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClashBan implements Comparable<ClashBan> {
    Champion champion;
    double score;
    List<Reason> reasons = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClashBan clashBan = (ClashBan) o;
        return Objects.equals(champion.getName(), clashBan.champion.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(champion);
    }

    ClashBan(Champion champion, double score) {
        this.champion = champion;
        this.score = score;
    }

    public ClashBan(Champion champion, double score, String reason) {
        this.champion = champion;
        this.score = score;
        this.reasons.add(new Reason(reason, score));
    }

    public static void add(List<ClashBan> scores, ClashBan cb2) {
        findClashban(scores, cb2.champion).add(cb2);
    }


    @Override
    public String toString() {
        return champion.getName() +
                "\t" + reasons.toString();
    }

    public void add(ClashBan cb2) {
        if (!this.equals(cb2)) throw new IllegalArgumentException("Champion mismatch on add.");
        score += cb2.score;
        reasons.addAll(cb2.reasons);
        reasons.sort(Comparator.comparingDouble(r -> -r.value));
        reasons = reasons.stream().limit(2).collect(Collectors.toList());
    }

    static ClashBan findClashban(List<ClashBan> scores, Champion champ) {
        return scores.get(scores.indexOf(new ClashBan(champ, 0)));
    }

    static List<ClashBan> prepareClashBanList() {
        var result = new ArrayList<ClashBan>();
        for (var champ: Champions.get()) {
            result.add(new ClashBan(champ, 0.0));
        }
        return result;
    }

    @Override
    public int compareTo(ClashBan o) {
        return ((this.score - o.score) < 0) ? 1 : -1;
    }
}
