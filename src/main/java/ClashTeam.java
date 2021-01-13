import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.ArrayList;
import java.util.List;

public class ClashTeam {

    List<ClashPlayer> players;

    String name;
    public static final int ENTRIES_PER_PLAYER = 7;

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
        var bans = team.mergeIntoOrderedList();
        for (int i = 0; i < 10; i++) {
            System.out.println(bans.get(i).toString());
        }
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
                        var factor = pGame.match.getQueue().equals(Queue.CLASH) ? 2
                                : pGame.match.getQueue().equals(Queue.RANKED_SOLO) || pGame.match.getQueue().equals(Queue.RANKED_FLEX) ? 1.4
                                : 1;
                        p.updateRecentScore(pGame, factor);
                    }
                }
            }
        }
        for (var p: players) {
            p.normalizeRecent();
        }
    }

    public List<ClashBan> mergeIntoOrderedList() {
        var finalBans = ClashBan.prepareClashBanList();
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

    public List<ClashBan> mergeIntoOrderedListByPlayer(ClashPlayer p) {
        var finalBans = ClashBan.prepareClashBanList();
        for (var cb : p.recentScores) {
            ClashBan.add(finalBans, cb);
        }
        for (var cb : p.masteryScores) {
            ClashBan.add(finalBans, cb);
        }
        finalBans.sort(ClashBan::compareTo);
        return finalBans;
    }

    public String[] bansText() {
        var result = new String[15];
        somethingWithPremades();
        var bans = mergeIntoOrderedList();
        for (int i = 0; i < 15; i++) {
            result[i] = bans.get(i).toString();
        }
        return result;
    }

    public String[] bansPerPlayer() {
        var result = new String[ENTRIES_PER_PLAYER * players.size()];
        somethingWithPremades();
        int i = 0;
        for (var cp : players) {
            var bans = mergeIntoOrderedListByPlayer(cp);
            for (var j = 0; j < ENTRIES_PER_PLAYER; j++) {
                final var clashBan = bans.get(j);
                double mastery = 0;
                double recency = 0;
                for (var r : clashBan.reasons) {
                    if (r.message.contains("mastery")) {
                        mastery = r.value;
                    } else {
                        recency = r.value;
                    }
                }
                var champ = clashBan.toString().split("\\[")[0].replaceAll("[^a-zA-Z0-9]", "").replaceAll(" ", "");
                result[i++] = cp.name + ":" + champ + ":" + clashBan.toString() + ";" + clashBan.score + ";" + mastery + ";" + recency;
            }
        }
        return result;
    }
}
