package data;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
                .flatMap(channel -> {
                    return Mono.just(helper(channel, event));
                })
                .then());

    }

    Flux<List<Mono<discord4j.core.object.entity.Message>>> helper(MessageChannel channel, MessageCreateEvent event) {
         List<Mono<discord4j.core.object.entity.Message>> messages = new ArrayList();
         String m = message(event.getMessage().getContent().get());

         for (int i= 0; i < m.length() / 1995; i++) {
           messages.add(channel.createMessage("```"+m.substring(i, Math.min(i*1995, m.length()))+"```"));
         }

        return Flux.just(messages);
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
