package visual;

import bot.ClashTeam;
import com.merakianalytics.orianna.types.core.staticdata.Champion;
import discord4j.core.object.entity.MessageChannel;
import util.U;

import java.awt.*;
import java.awt.geom.AffineTransform;

public class ClashImageGenerator extends ImageGenerator {

    public ClashImageGenerator(String[] resp, MessageChannel channel) {
        super("Clashbans:");

        var numberOfPlayers = resp.length / ClashTeam.ENTRIES_PER_PLAYER;
        var y = OUT_HEIGHT * 0.19;

        final var championListWidth = OUT_WIDTH * 0.6 - CHAMPION_SQUARE_SIZE * CHAMPION_SQUARE_SCALE;
        final var championListHeight = OUT_HEIGHT * 0.8 - (CHAMPION_SQUARE_SIZE + 55) * CHAMPION_SQUARE_SCALE;

        var maxRecentlyScore = U.getMaximum(resp, U.splitSelectAtSemicolon(2));
        var maxMasteryScore = U.getMaximum(resp, U.splitSelectAtSemicolon(3));
        var minScoreLog = Math.log(U.getMinimum(resp, U.splitSelectAtSemicolon(1)) + 1);
        var maxScoreLog = Math.log(U.getMaximum(resp, U.splitSelectAtSemicolon(1)) + 1);
        fillArrow(0.95, 0.08, -0.55, 0);

        for (int p = 0; p < numberOfPlayers; p++) {
            var x = OUT_WIDTH * 0.05 + 128;
            var summonerName = resp[p * ClashTeam.ENTRIES_PER_PLAYER].split(":")[0];
            drawSummoner(summonerName, x + 128, y +32);

            for (int i = 0; i < ClashTeam.ENTRIES_PER_PLAYER; i++) {
                var championName = resp[p * ClashTeam.ENTRIES_PER_PLAYER + i].split(":")[1];
                var championScore = Math.log(U.splitSelectAtSemicolon(1).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) + 1);
                var scoreRatio = 1 - ((championScore - minScoreLog) / (maxScoreLog - minScoreLog));
                float scoreRecently = (float) (U.splitSelectAtSemicolon(2).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxRecentlyScore);
                float scoreMastery = (float) (U.splitSelectAtSemicolon(3).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxMasteryScore);

                scoreRecently *= scoreRecently * scoreRecently;
                var trueX = OUT_WIDTH * 0.4 + scoreRatio * championListWidth;
                x = Math.max(trueX, x);
                if (x + (CHAMPION_SQUARE_SIZE + 10) * CHAMPION_SQUARE_SCALE > OUT_WIDTH) {
                    break;
                }
                drawChampionWithReasons(x, y, championName, scoreRecently, scoreMastery);
                x += CHAMPION_SQUARE_SIZE * CHAMPION_SQUARE_SCALE;
            }
            y += championListHeight / 4.0;
        }
        makeMessage(channel, "clash.png");
    }

    void drawChampionWithReasons(double x, double y, String championName, float scoreRecently, float scoreMastery) {

        var masteryScale = ImageGenerator.CHAMPION_SQUARE_SCALE * 1.1;
        var recentlyScale = ImageGenerator.CHAMPION_SQUARE_SCALE * 0.42;

        var champion = Champion.named(championName).get().getImage().get();
        draw(champion, x, y, ImageGenerator.CHAMPION_SQUARE_SCALE);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreMastery));
        var x2 = x + CHAMPION_SQUARE_SIZE * 7.0 / 24 * ImageGenerator.CHAMPION_SQUARE_SCALE - 80 * masteryScale / 2;
        var y2 = y + (CHAMPION_SQUARE_SIZE + 5) * ImageGenerator.CHAMPION_SQUARE_SCALE;
        draw(mastery, x2, y2, masteryScale);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, scoreRecently));
        var x3 = x + CHAMPION_SQUARE_SIZE * 17.0 / 24 * ImageGenerator.CHAMPION_SQUARE_SCALE - 100 * recentlyScale / 2;
        var y3 = y + (CHAMPION_SQUARE_SIZE + 5) * ImageGenerator.CHAMPION_SQUARE_SCALE;
        draw(recently, x3, y3, recentlyScale);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }
}
