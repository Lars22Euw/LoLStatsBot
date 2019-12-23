import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.*;
import java.util.stream.Collectors;

import static com.merakianalytics.orianna.types.core.match.MatchHistory.forSummoner;

public class ClashPlayer extends Player {
    private final int totalMastery;
    private final List<Double> relativeMastery;
    private final List<ChampionMastery> masteries;
    private final List<Double> bans;

    public static void main(String[] args) {
        var m = new Manager(args[0]);
        var sum = Summoner.named("h4ckbraten").get();
        //assert(sum.exists());
        var result = new ClashPlayer(sum);
        result.getBans(10).forEach(c -> System.out.println(c.getName()));
    }

    public ClashPlayer(Summoner sum, Manager manager) {
        super(sum, manager);
        this.totalMastery = 0;
        relativeMastery = null;
        masteries = null;
        bans = null;
    }

    public ClashPlayer(Summoner sum) {
        super();
        this.masteries = new ArrayList<>(ChampionMasteries.forSummoner(sum).get());
        this.totalMastery = masteries.stream().mapToInt(ChampionMastery::getPoints).sum();
        this.matches = new TreeSet<>(Game::compare2);
        for (var m : forSummoner(sum).withStartIndex(0).withEndIndex(100).get()) {
            matches.add(new Game(m));
        }
        this.relativeMastery = masteries.stream().map(cm -> ((double) cm.getPoints())/totalMastery).collect(Collectors.toList());

        final var scale = 0.5;
        this.bans = new ArrayList<>();
        for (int i = 0; i < masteries.size(); i++) {
            var score = Math.log(masteries.get(i).getPoints()) * scale + relativeMastery.get(i) * (1 - scale);
            bans.add(score);
        }
    }

    public List<Champion> getBans(int amount) {
        var result = new ArrayList<Integer>();
        var idx = 0;
        var worstScore = Double.MAX_VALUE;
        for (int i = 0; i < masteries.size(); i++) {
            if (result.size() < amount) {
                result.add(i);
            }
            if (worstScore < bans.get(i)) {
                result.set(idx, i);
            }
            worstScore = Double.MAX_VALUE;
            for (int j = 0; j < result.size(); j++) {
                var score = bans.get(result.get(j));
                if (score < worstScore) {
                    worstScore = score;
                    idx = j;
                }
            }
        }
        return result.stream().map(i -> masteries.get(i).getChampion()).collect(Collectors.toList());
    }

}
