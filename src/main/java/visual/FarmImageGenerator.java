package visual;

import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import discord4j.core.object.entity.MessageChannel;
import util.U;
import util.UPair;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class FarmImageGenerator extends ImageGenerator {

    public static void farm(MessageChannel channel, List<UPair<Match, ParticipantStats>> data) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BG_SCALE, BACKGROUND_HEIGHT * BG_SCALE, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(g, background);
        g.setColor(GOLD);
        makeTitle(g, "CreepScore:");
        makeSmallText(g, "cs/min", 0.01, 0.33);
        makeSmallText(g, "time", 0.92, 0.93);
        fillArrow(g, 0.05, 0.9, 0.9, 0); // >
        fillArrow(g, 0.05, 0.9, 0, -0.6); // ^

        var baseWidth = 0.87;
        var baseHeight = 0.58;
        var widthOffset = 0.05;
        var heightOffset = 0.9;
        var maxFarmPerMinute = data.stream().map(p -> (p.second.getCreepScore() + p.second.getNeutralMinionsKilled())
                / ((p.first.getDuration().getStandardSeconds() - 90) / 60.0)).max(Double::compareTo).orElse(1.0);
        var totalDuration = data.stream().map(p -> p.first.getDuration().getStandardSeconds()).reduce(Long::sum).orElse(1L);
        for (int i = data.size() - 1; i >= 0; i--) {
            UPair<Match, ParticipantStats> p = data.get(i);
            try {
                U.log(p.first.getQueue());
                switch (p.first.getQueue()) {
                    case NORMAL:
                        g.setColor(Color.BLUE);
                        break;
                    case CLASH:
                        g.setColor(Color.RED);
                        break;
                    case BLIND_PICK:
                        g.setColor(Color.CYAN);
                        break;
                    case ARAM:
                        g.setColor(Color.GREEN);
                        break;
                    case RANKED_SOLO:
                        g.setColor(Color.YELLOW);
                        break;
                    case RANKED_FLEX:
                        g.setColor(Color.ORANGE);
                        break;
                    default:
                        g.setColor(Color.WHITE);
                        break;
                }
            } catch (Exception e) {
                g.setColor(Color.WHITE);
            }
            var height = baseHeight * ((p.second.getCreepScore() + p.second.getNeutralMinionsKilled()) /
                    ((p.first.getDuration().getStandardSeconds() / 60.0) * maxFarmPerMinute));
            var width = baseWidth * (p.first.getDuration().getStandardSeconds() / (double) totalDuration);
            U.log(height, width);
            g.setStroke(new BasicStroke(0));
            doubleRect(g, widthOffset + 0.01, heightOffset - height - 0.001, width, height);
            g.setStroke(new BasicStroke(20.0f / data.size() ));
            g.setColor(Color.BLACK);
            doubleRectBorder(g, widthOffset + 0.01, heightOffset - height - 0.001, width, height);
            widthOffset += width;
        }
        makeMessage(channel, img, "farm.png");

    }
}
