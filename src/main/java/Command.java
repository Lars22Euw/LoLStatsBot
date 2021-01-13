import discord4j.core.object.entity.Message;

import java.util.function.Function;

public class Command {
    String argument;
    Function<Message, Integer> func;

    public Command(String argument, Function<Message, Integer> func) {
        this.argument = argument;
        this.func = func;
    }

    public Command(String argument) {
        this.argument = argument;
    }
}
