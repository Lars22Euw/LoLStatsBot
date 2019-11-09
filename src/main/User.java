package main;

import org.joda.time.DateTime;

import java.util.*;

/**
 * a User may have multiple summoner names (Players) in the default region.
 */
public class User {

    String name;
    List<Player> players = new ArrayList<>();
    SortedSet<Game> matches = new TreeSet<>();

    public User(String name, List<String> usernames, Manager manager) {
        this.name = name;

        for (String s : usernames)
            players.add(new Player(s, manager));

        // TODO: verify that none of the players have played together.
        for (Player player : players) {
            matches.addAll(player.matches);
        }

    }
}
