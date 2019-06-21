package com.jezza;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private static final Map<String, Command> commands = new HashMap<>();
    private static DiscordClient client;

    private static List<String> quotes;

    public static void main(String[] args) {
        client = new DiscordClientBuilder(args[0]).build();

        populateQuotes();

        commands.put("russell", event -> event.getMessage().getChannel()
                .flatMap(channel -> channel.createMessage(generateRandomMessage()))
                .then());

        client.getEventDispatcher().on(MessageCreateEvent.class)
                .flatMap(event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.contains(entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next()))
                .subscribe();


        client.login().block();
    }

    private static String generateRandomMessage(){
        int range = quotes.size();

        Random rand = new Random();

        return quotes.get(rand.nextInt(range));

    }

    private static void populateQuotes() {
        System.out.println(Paths.get(".").toAbsolutePath());
        try (BufferedReader br = Files.newBufferedReader(Paths.get("quotes.txt"))) {

            //br returns as stream and convert it into a List
            quotes = br.lines().collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            quotes = new ArrayList<>();
            quotes.add("software engineer btw");
        }

    }
}

interface Command {
    // Since we are expecting to do reactive things in this method, like
    // send a message, then this method will also return a reactive type.
    Mono<Void> execute(MessageCreateEvent event);
}