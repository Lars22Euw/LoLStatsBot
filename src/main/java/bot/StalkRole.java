package bot;

import com.merakianalytics.orianna.types.common.Lane;
import com.merakianalytics.orianna.types.common.Queue;
import com.merakianalytics.orianna.types.common.Role;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.summoner.Summoner;


public enum StalkRole {
    TOP,
    JUNGLE,
    MID,
    BOT,
    SUPPORT,
    NONE;

    public static StalkRole findRole(Match m, Summoner summoner) {
        if (m.getQueue().equals(Queue.ARAM)) {
            return NONE;
        }
        var participant= m.getParticipants().find(p -> p.getSummoner().getName().equals(summoner.getName()));
        var role = participant.getRole();
        var lane = participant.getLane();
        return switch (lane) {
            case TOP -> StalkRole.TOP;
            case JUNGLE -> StalkRole.JUNGLE;
            case MID, MIDDLE -> StalkRole.MID;
            case BOT, BOTTOM -> (role == Role.DUO_CARRY) ? BOT : SUPPORT;
            case NONE -> StalkRole.NONE;
        };
    }

    public static StalkRole fromString(String s) {
        return switch (s) {
            case "Top" -> StalkRole.TOP;
            case "Jungle" -> StalkRole.JUNGLE;
            case "Middle" -> StalkRole.MID;
            case "ADC" -> StalkRole.BOT;
            case "Support" -> StalkRole.SUPPORT;
            default -> throw new IllegalStateException("Unexpected value: " + s);
        };
    }
}
