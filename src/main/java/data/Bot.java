package data;

import com.merakianalytics.orianna.Orianna;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();

    Manager manager;

     {
        commands.put("matches", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(new Message(event.getMessage().getContent().get()).build()))
                .then());

    }

    public static void main(String[] args) {
        Bot s = new Bot();
        s.manager = new Manager(args[0]);
        String discordToken =  args[1];
        final DiscordClient client = new DiscordClientBuilder(discordToken).build();
        getCommands(client);
        client.login().block();
    }

    private static void getCommands(DiscordClient client) {
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith(entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();
    }
}
