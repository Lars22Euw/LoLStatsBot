import com.merakianalytics.orianna.types.core.match.Match;
import com.merakianalytics.orianna.types.core.match.Participant;
import com.merakianalytics.orianna.types.core.match.Team;
import com.merakianalytics.orianna.types.core.searchable.SearchableList;
import com.merakianalytics.orianna.types.core.summoner.Summoner;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;


public class Util {

    public static DateTimeFormatter dtf = DateTimeFormat.forPattern("EE yyyy'-'MM'-'dd' 'HH':'mm");

    public static Team getTeam(Summoner summoner, Match match) {
        return match.getBlueTeam().getParticipants().contains(summoner) ?
                match.getBlueTeam() : match.getRedTeam();
    }

    public static List<Summoner> getPlayers(Summoner summoner, Match match) {
        List<Summoner> result = new ArrayList<>();
        final SearchableList<Participant> participants = getTeam(summoner, match).getParticipants();
        participants.forEach(p -> result.add(p.getSummoner()));
        return result;
    }
}
