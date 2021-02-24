import com.merakianalytics.orianna.types.common.Lane;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;

import static com.merakianalytics.orianna.types.core.match.MatchHistory.forSummoner;

public class ClashPlayer extends Player {
    public static final int MASTERY_LOWER_CUTOFF = 12000;
    private static final Double RECENTLY_SCALING = 170.0;
    public static int SUMMONER_NAME_SIZE = 3;
    private int totalMastery;
    private List<Double> relativeMastery;
    private List<ChampionMastery> masteries;
    /**
     * champions scored by masteryscore.
     * "Erfahrung"
     */
    List<ClashBan> masteryScores;
    /**
     * champions scored by recent playrate
     */
    //Map<Lane, Map<Champion, Double>> recentScores;
    List<ClashBan> recentScores;
    private final DateTime now;

    public ClashPlayer(Summoner sum, Manager manager) {
        super(sum, manager);
        this.totalMastery = 0;
        relativeMastery = null;
        masteries = null;
        masteryScores = null;
        now = null;
    }

    public ClashPlayer(Summoner sum) {
        super();
        super.summoner = sum;
        super.name = sum.getName();
        if (sum.getName().length() > SUMMONER_NAME_SIZE)
            SUMMONER_NAME_SIZE = sum.getName().length();
        now = DateTime.now();
        setMasteryScores(sum);
        setRecentScores(sum);
    }

    private void setMasteryScores(Summoner sum) {
        this.masteries = new ArrayList<>(ChampionMasteries.forSummoner(sum).get());
        this.totalMastery = masteries.stream().mapToInt(ChampionMastery::getPoints).sum();
        this.relativeMastery = masteries.stream().map(cm -> ((double) cm.getPoints())/totalMastery).collect(Collectors.toList());
        final var scale = 0.3;
        this.masteryScores = new ArrayList<>();
        for (int i = 0; i < masteries.size(); i++) {
            if (masteries.get(i).getPoints() < MASTERY_LOWER_CUTOFF) continue;
            var score = Math.log(masteries.get(i).getPoints() - 12000) * scale + relativeMastery.get(i) * (1 - scale) * 10;
            masteryScores.add(new ClashBan(masteries.get(i).getChampion(), score,
                    makeReason(masteries.get(i).getPoints())));
        }
    }

    private String makeReason(double displayValue) {
        displayValue /= 1000;
        String result = Util.asString(name, SUMMONER_NAME_SIZE) + ": mastery (";
        if ((displayValue / 1000) >= 1) result += (int) (displayValue/1000) + "m)";
        else result += (int) displayValue + "k)";
        return  result;
    }

    private String makeReason2(double score) {
        String result = Util.asString(name, SUMMONER_NAME_SIZE) + ": recency (";
        result += (int) (score * 1.5 + 1) + ")";
        return result;
    }

    private void setRecentScores(Summoner sum) {
        this.matches = new TreeSet<>(Game::compare2);
        // TODO: Handle NONE
        for (var m : forSummoner(sum).withStartIndex(0).withEndIndex(500).withQueues(
                com.merakianalytics.orianna.types.common.Queue.RANKED_SOLO,
                com.merakianalytics.orianna.types.common.Queue.BLIND_PICK,
                com.merakianalytics.orianna.types.common.Queue.NORMAL,
                com.merakianalytics.orianna.types.common.Queue.CLASH).get()
                .filter(m -> !m.getParticipants().find(p -> p.getSummoner().equals(sum)).getLane().equals(Lane.NONE))) {
            matches.add(new Game(m));
        }

        recentScores = ClashBan.prepareClashBanList();
    }

    public void updateRecentScore(Game game, double factor) {
        var participant = game.match.getParticipants().find(p -> p.getSummoner().equals(summoner));
        if (participant == null) {
            System.out.println("Error in retrieving participant");
            return;
        }
        var champ = participant.getChampion();
        ClashBan.add(recentScores, new ClashBan(champ,function(game) * factor));
    }


    private Double function(Game game) {
        var difDays = now.minus(game.time.getMillis()).getMillis()/ 3600000 / 24;
        var b = Math.log(0.8)/30;
        var x = Math.exp(b * difDays);
        if (game.match.getQueue().equals(com.merakianalytics.orianna.types.common.Queue.RANKED_SOLO) ||
           game.match.getQueue().equals(com.merakianalytics.orianna.types.common.Queue.RANKED_FLEX)) {
            return 2 * x;
        }
        if (game.match.getQueue().equals(com.merakianalytics.orianna.types.common.Queue.CLASH)) {
            return 4 * x;
        }
        return x;
    }

    public void normalizeRecent() {
        if (matches.isEmpty()) {
            throw new IllegalArgumentException(name);
        }
        recentScores.forEach(clashBan -> clashBan.score *= RECENTLY_SCALING / matches.size());
        recentScores.forEach(clashBan -> clashBan.reasons.add(
                new Reason(makeReason2(clashBan.score), clashBan.score)));
    }
}
