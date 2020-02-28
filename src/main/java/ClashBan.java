import com.merakianalytics.orianna.types.core.staticdata.Champion;

import java.util.Objects;

public class ClashBan implements Comparable<ClashBan> {
    Champion champion;
    double score;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClashBan clashBan = (ClashBan) o;
        return Objects.equals(champion, clashBan.champion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(champion);
    }

    public ClashBan(Champion champion, double score) {
        this.champion = champion;
        this.score = score;
    }


    @Override
    public int compareTo(ClashBan o) {
        return ((this.score - o.score) < 0) ? 1 : -1;
    }
}
