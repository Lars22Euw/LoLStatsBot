package bot;

import com.merakianalytics.orianna.types.common.Role;
import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.summoner.Summoner;


public enum StalkRole {
    TOP,
    JUNGLE,
    MID,
    BOT_CARRY,
    SUPPORT,
    NONE;


    public static StalkRole findRole(Match m, Summoner summoner) {
        var participant= m.getParticipants().find(p -> p.getSummoner().getName().equals(summoner.getName()));
        var role = participant.getRole();
        var lane = participant.getLane();
        return switch (lane) {
            case TOP -> StalkRole.TOP;
            case JUNGLE -> StalkRole.JUNGLE;
            case MID, MIDDLE -> StalkRole.MID;
            case BOT, BOTTOM -> (role == Role.DUO_CARRY) ? BOT_CARRY : SUPPORT;
            case NONE -> StalkRole.NONE;
        };
    }
}
