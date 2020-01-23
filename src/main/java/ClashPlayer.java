import com.merakianalytics.orianna.types.common.Lane;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMasteries;
import com.merakianalytics.orianna.types.core.championmastery.ChampionMastery;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.Participant;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;

import static com.merakianalytics.orianna.types.core.match.MatchHistory.forSummoner;

public class ClashPlayer extends Player {
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
    Map<Lane, Map<Champion, Double>> recentScores;
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
        now = DateTime.now();
        setMasteryScores(sum);
        setRecentScores(sum);
    }

    private void setMasteryScores(Summoner sum) {
        this.masteries = new ArrayList<>(ChampionMasteries.forSummoner(sum).get());
        this.totalMastery = masteries.stream().mapToInt(ChampionMastery::getPoints).sum();

        this.relativeMastery = masteries.stream().map(cm -> ((double) cm.getPoints())/totalMastery).collect(Collectors.toList());

        final var scale = 0.5;
        this.masteryScores = new ArrayList<>();
        for (int i = 0; i < masteries.size(); i++) {
            var score = Math.log(masteries.get(i).getPoints()) * scale + relativeMastery.get(i) * (1 - scale);
            masteryScores.add(new ClashBan( masteries.get(i).getChampion(), score));
        }
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

        recentScores = new HashMap<>();
        for (var lane: Lane.values()) {
            var bans = new HashMap<Champion, Double>();
            for (var champ: Champions.get()) {
                bans.put(champ, 0.0);
            }
            recentScores.put(lane, bans);
        }
    }

    public void updateRecentScore(Game game, double factor) {
        var participant = game.match.getParticipants().find(p -> p.getSummoner().equals(summoner));
        if (participant == null) {
            System.out.println("Error in retrieving participant");
            return;
        }
        var champ = participant.getChampion();
        System.out.println(participant.getLane().toString() + " with " + participant.getChampion().getName());
        var old = recentScores.get(participant.getLane()).get(champ);
        recentScores.get(participant.getLane()).replace(champ, old + function(game)*factor);
    }

    private Double function(Game game) {
        var difDays = now.minus(game.time.getMillis()).getMillis()/ 3600000 / 24;
        var b = Math.log(0.8)/30;
        var x = Math.exp( b * difDays);
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
        recentScores.forEach((l, champs) -> {
            for (var c : champs.keySet()) {
                champs.replace(c, champs.get(c).doubleValue() / matches.size());
            }
        });
    }
}
