package visual;

import bot.StalkDataset;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.ParticipantStats;
import discord4j.core.object.entity.MessageChannel;
import util.U;
import util.UPair;

import java.awt.*;
import java.awt.image.BufferedImage;

public class StalkImageGenerator extends ImageGenerator {

    public static void stalk(MessageChannel channel, StalkDataset data) {
        var img = new BufferedImage( BACKGROUND_WIDTH * BG_SCALE, BACKGROUND_HEIGHT * BG_SCALE, BufferedImage.TYPE_INT_ARGB);
        var g = img.createGraphics();
        setBackground(g, background);
        g.setColor(GOLD);
        makeTitle(g, "Stalk:");
        makeSmallText(g, "Winrate by Role", 0.2, 0.1);
        makeSmallText(g, "Winrate by Champion", 0.5, 0.1);
        makeSmallText(g, "Winrate by Gamemode", 0.8, 0.1);
        g.setColor(Color.LIGHT_GRAY);
        g.fillArc(200, 200, 100, 100, 0, 60);
        doublePie(g, 0.2, 0.5, 0.3, data.rolesData);
        doublePie(g, 0.5, 0.5, 0.3, data.championsData);
        // doublePie(g, 0.8, 0.5, 0.3, data.gamemodesData);

        makeMessage(channel, img, "farm.png");

    }
}
