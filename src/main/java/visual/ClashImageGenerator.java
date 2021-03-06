package visual;

import bot.ClashTeam;
import discord4j.core.object.entity.MessageChannel;

public class ClashImageGenerator extends ImageGenerator {

    public static void clash(String[] resp, MessageChannel channel) {
        var img = createScaledBufferedImage(background, BG_SCALE);
        var g = img.createGraphics();

        setBackground(g, background);
        g.setColor(GOLD);
        makeTitle(g, "Clashbans:");

        var numberOfPlayers = resp.length / ClashTeam.ENTRIES_PER_PLAYER;
        var y = OUT_HEIGHT * 0.19;
        var championSquareScale = 0.24 * BG_SCALE;
        var masteryScale = championSquareScale * 1.1;
        var recentlyScale = championSquareScale * 0.42;

        final var championListWidth = OUT_WIDTH * 0.6 - CHAMPION_SQUARE_SIZE * championSquareScale;
        final var championListHeight = OUT_HEIGHT * 0.8 - (CHAMPION_SQUARE_SIZE + 55) * championSquareScale;

        var maxRecentlyScore = getMaximum(resp, splitSelectAtSemicolon(2));
        var maxMasteryScore = getMaximum(resp, splitSelectAtSemicolon(3));
        var minScoreLog = Math.log(getMinimum(resp, splitSelectAtSemicolon(1)) + 1);
        var maxScoreLog = Math.log(getMaximum(resp, splitSelectAtSemicolon(1)) + 1);
        fillArrow(g, 0.95, 0.08, -0.55, 0);

        for (int p = 0; p < numberOfPlayers; p++) {
            var x = OUT_WIDTH * 0.05;
            var summonerName = resp[p * ClashTeam.ENTRIES_PER_PLAYER].split(":")[0];
            g.drawString(summonerName, (int) x, (int) (y + g.getFont().getSize() / 2 +  110 * championSquareScale / 2));
            for (int i = 0; i < ClashTeam.ENTRIES_PER_PLAYER; i++) {
                var championName = resp[p * ClashTeam.ENTRIES_PER_PLAYER + i].split(":")[1];
                var championScore = Math.log(splitSelectAtSemicolon(1).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) + 1);
                var scoreRatio = 1 - ((championScore - minScoreLog) / (maxScoreLog - minScoreLog));
                float scoreRecently = (float) (splitSelectAtSemicolon(2).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxRecentlyScore);
                float scoreMastery = (float) (splitSelectAtSemicolon(3).apply(resp[p * ClashTeam.ENTRIES_PER_PLAYER + i]) / maxMasteryScore);

                scoreRecently *= scoreRecently * scoreRecently;
                var trueX = OUT_WIDTH * 0.4 + scoreRatio * championListWidth;
                x = Math.max(trueX, x);
                if (x + (CHAMPION_SQUARE_SIZE + 10) * championSquareScale > OUT_WIDTH) {
                    break;
                }
                drawChampionWithReasons(channel, g, x, y,
                        championSquareScale, masteryScale, recentlyScale,
                        championName, scoreRecently, scoreMastery);
                x += CHAMPION_SQUARE_SIZE * championSquareScale;
            }
            y += championListHeight / 4.0;
        }

        makeMessage(channel, img, "clash.png");

    }

}
