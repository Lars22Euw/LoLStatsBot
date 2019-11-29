package data;

import com.merakianalytics.orianna.Orianna;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static final String PREFIX = ".";

    private DiscordClient client;
    private Manager manager;

     private Bot(String riotAPI, String discordAPI) {
        manager = new Manager(riotAPI);
        client = new DiscordClientBuilder(discordAPI).build();
        commands.put("matches", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(message(event.getMessage().getContent().get())))
                .then());

    }

    private String message(String input) {
         if (manager == null) System.out.println("manager is null");
         return new Message(input, manager).build();
    }

    public static void main(String[] args) {
        var s = new Bot(args[0], args[1]);
        s.getCommands(s.client);
        s.client.login().block();
    }

    private static void getCommands(DiscordClient client) {
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith(PREFIX+entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();
    }
}
