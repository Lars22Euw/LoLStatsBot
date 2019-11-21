package data;

import com.merakianalytics.orianna.types.core.summoner.Summoner;

import java.util.*;

/**
 * a data.User may have multiple summoner names (Players) in the default region.
 */
public class User {

    String name;
    public List<Player> players = new ArrayList<>();
    public SortedSet<Game> matches = new TreeSet<>();

    public User(String name, List<Summoner> usernames, Manager manager) {
        this.name = name;

        for (Summoner s : usernames) {
            players.add(new Player(s, manager));
        }


        // TODO: verify that none of the players have played together.
        for (Player player : players) {
            matches.addAll(player.matches);
        }

    }
}
