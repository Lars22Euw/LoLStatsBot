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
        var bans = team.somethingWithPremades();
        for (int i = 0; i < 10; i++) {
            System.out.println(bans.get(i).toString());
        }
    }

    List<ClashBan> somethingWithPremades() {
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
            }
        }
        for (var p: players) {
            p.normalizeRecent();
        }
        // filter pos raus?
        var finalBans = ClashBan.prepareClashBanList();
        System.out.println(finalBans.size());
        for (var p: players) {
            for (var cb : p.recentScores) {
                ClashBan.add(finalBans, cb);
            }
            for (var cb : p.masteryScores) {
                ClashBan.add(finalBans, cb);
            }
        }
        finalBans.sort(ClashBan::compareTo);
        return finalBans;
    }

    public String[] bans() {
        var result = new String[15];
        var bans = somethingWithPremades();
        for (int i = 0; i < 15; i++) {
            result[i] = bans.get(i).toString();
        }
        return result;
    }
}
