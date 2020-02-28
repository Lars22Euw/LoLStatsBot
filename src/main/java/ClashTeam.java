import com.merakianalytics.orianna.types.core.staticdata.Champion;
import com.merakianalytics.orianna.types.core.staticdata.Champions;
import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ClashTeam {

    private static final Double RECENTLY_SCALING = 10.0;
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

        var ban1 = new ClashBan(Champions.named("Bard").get().get(0), 0);
        var ban2 = new ClashBan(Champions.named("Bard").get().get(0), 1);
        System.out.println(ban1.equals(ban2));

        var bans = team.somethingWithPremades();
        for (int i = 0; i < 10; i++) {
            System.out.println(bans.get(i).champion.getName() + "  "+bans.get(i).score);
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
                p.normalizeRecent();
            }
        }

        // filter pos raus?
        var sumMap = new HashMap<Champion, Double>();
        var finalBans = new ArrayList<ClashBan>();

        for (var p: players) {
            for (var lane: p.recentScores.keySet()) {
                for (var e: p.recentScores.get(lane).entrySet()) {
                    var champ = e.getKey();
                    var score = e.getValue();
                    if (sumMap.containsKey(champ)) {
                        var x = sumMap.get(champ);
                        sumMap.replace(champ, x + score);
                    } else {
                        sumMap.put(champ, score);
                    }
                }
            }

            // mastery
            for (var m: p.masteryScores) {
                boolean edit = false;
                for (var c: finalBans) {
                    if(c.champion.getName().equals(m.champion.getName())) {
                        c.score += m.score;
                        edit = true;
                        break;
                    }
                }
                if (!edit) {
                    var cb = new ClashBan(m.champion, m.score);
                    finalBans.add(cb);
                }
            }
        }

        // merge
        for (var entry: sumMap.entrySet()) {
            System.out.println(entry.getKey().getName()+" "+entry.getValue());
            var champ = entry.getKey();
            boolean edit = false;
            for (var c: finalBans) {
                if (c.champion.getName().equals(entry.getKey().getName())) {
                    c.score += entry.getValue() * RECENTLY_SCALING;
                    edit = true;
                    break;
                }
            }
            if (!edit) {
                finalBans.add(new ClashBan(champ, entry.getValue() * RECENTLY_SCALING));
            }
        }

        finalBans.sort(ClashBan::compareTo);

        return finalBans;
    }

}
