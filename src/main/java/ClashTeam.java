import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.ArrayList;
import java.util.List;

public class ClashTeam {

    List<ClashPlayer> players;

    String name;

    ClashTeam(List<ClashPlayer> players, String name) {
        this.players = players;
        this.name = name;
    }

    public static void main(String[] args) {
        new Manager(args[0]);
        var sum = Summoner.named("TheNonamed").get();
        var thomas = new ClashPlayer(sum);
        var sum2 = Summoner.named("TheLars22").get();
        var lars = new ClashPlayer(sum2);
        var team = new ClashTeam(List.of(thomas, lars),"UoH");
        team.somethingWithPremades();
    }

    void somethingWithPremades() {
        List<Long> allMatches = new ArrayList<>();
        for (var player: players) {
            for (var game: player.matches) {
                allMatches.add(game.id);
            }
        }

        for (var game: allMatches) {
            for (var p: players) {
                for (var pGame: p.matches) {
                    if (pGame.id == game) {
                        p.updateRecentScore(pGame, 1);
                    }
                }
                p.normalizeRecent();
            }
        }
    }

}
