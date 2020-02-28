import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;

import java.util.*;

public class Bot {

    private static final String PREFIX = ".";

    private DiscordClient client;
    private Manager manager;

    private List<Command> commands = List.of(
            new Command("matches", this::matches),
            new Command("m", this::matches),
            new Command("c", this::clash),
            new Command("clash", this::clash),
            new Command("help", this::help),
            new Command("h", this::help));

    private Integer clash(Message message) {
        var msgText = message.getContent().get();
        var resp = messageClash(msgText);
        StringBuilder sb = new StringBuilder();
        for (var line: resp) {
            System.out.println("some " + line);
            sb.append(line).append("\n");
        }
        System.out.println(sb.toString());
        message.getChannel().block().createMessage("```" + "\nBans in order:\n" + sb.toString() + "```").block();
        return 0;
    }

    private String[] messageClash(String input) {
        if (manager == null) System.out.println("manager is null");
        return new MyMessage(manager).clash(input);
    }

    private Integer matches(Message message) {
        var msgText = message.getContent().get();
        var resp = message(msgText);
        if (resp == null || resp.length == 0) {
            System.out.println("Empty msg");
        }
        StringBuilder sb = new StringBuilder();
        for (var line: resp) {
            if (sb.length() > 1900) {
                message.getChannel().block().createMessage("LoL matches per day:" +"```"+sb.toString()+"```").block();
                sb = new StringBuilder();
            }
            sb.append(line).append("\n");
        }
        message.getChannel().block().createMessage("```"+sb.toString()+"```").block();
        return 0;
    }

    private Integer help(Message message) {
        message.getChannel().block().createMessage("You can message this bot directly.\n" +
                "`.help` will display this very help message.\n" +
                "`.matches` will list matches per day for a given user.\n" +
                "`.clash` will return suitable bans against a list of players.").block();
        return 0;
    }

     Bot(String riotAPI, String discordAPI) {
        manager = new Manager(riotAPI);
        client = new DiscordClientBuilder(discordAPI).build();
    }

    private String[] message(String input) {
         if (manager == null) System.out.println("manager is null");
         return new MyMessage(input, manager).build();
    }

    public static void main(String[] args) {
        var s = new Bot(args[0], args[1]);
        s.getCommands(s.client);
        s.client.login().block();
    }

    private void getCommands(DiscordClient client) {
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .subscribe(event -> {
                    var msgText = event.getMessage().getContent().orElse(null);
                    if (msgText == null) return;
                    var parts = msgText.split(" ");
                    for (var c: commands) {
                        //System.out.println(parts[0]);
                        if ((PREFIX+c.argument).equalsIgnoreCase(parts[0])) {
                            c.func.apply(event.getMessage());
                            break;
                        }
                    }
                });

/*         // ########
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith(PREFIX+entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();*/
    }
}
