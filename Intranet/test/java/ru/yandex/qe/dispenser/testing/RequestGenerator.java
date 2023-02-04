package ru.yandex.qe.dispenser.testing;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.cxf.jaxrs.client.WebClient;
import org.jetbrains.annotations.NotNull;

import ru.yandex.qe.dispenser.api.util.SerializationUtils;
import ru.yandex.qe.dispenser.client.RequestStyleLoggingWebClient;
import ru.yandex.qe.dispenser.client.v1.DiOAuthToken;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.LoggingDispenserFactory;

public final class RequestGenerator {
    private final Random rand = new Random();
    private final Scenario[] scenarios;
    private final Context ctx;

    public RequestGenerator(
            @JsonProperty("projectsCount") final int projectsCount,
            @JsonProperty("usersCount") final int usersCount,
            @JsonProperty("scenarios") final List<Scenario> scenarios) {

        final int wsum = scenarios.stream().mapToInt(Scenario::getWeight).sum();
        this.scenarios = new Scenario[wsum];
        int i = 0;
        for (final Scenario scenario : scenarios) {
            for (int j = 0; j < scenario.getWeight(); ++j) {
                this.scenarios[i++] = scenario;
            }
        }
        this.ctx = new Context(usersCount, projectsCount);
    }


    public void next(final Context ctx, final Dispenser dispenser) {
        final Scenario scenario = scenarios[rand.nextInt(scenarios.length)];
        scenario.execute(ctx, dispenser);
    }

    public void generate(final int count, final Dispenser dispenser) {
        IntStream.rangeClosed(1, count).forEach(i -> {
            System.out.println("--");
            System.out.println("Generating scenario " + i);
            next(ctx, dispenser);
        });
    }

    private static String DISPENSER_CLIENT_ID = "TANK";
    private static String DISPENSER_FAKE_HOST = "http://dispenser-fake.yandex-team.ru";

    public static void main(final String[] args) throws IOException, ParseException {

        // -n 1000 -h http://127.0.0.1:8082 -c testing/testing.json -o out.curl

        final Options options = new Options();
        options.addOption(new Option("h", "host", true, "target host"));
        options.addOption(new Option("c", "config", true, "config resource"));
        options.addOption(new Option("o", "out", true, "output file"));
        options.addOption(new Option("n", "number", true, "number of series"));
        options.addOption(new Option("d", "dolbilka", false, "send real requests to target host"));
        options.addOption(new Option("t", "token", false, "oauth2 token"));

        final CommandLineParser clp = new PosixParser();
        final CommandLine cl = clp.parse(options, args);

        final boolean dolbilka = cl.hasOption("d");
        final String host = cl.getOptionValue("h");
        final Integer n = Integer.valueOf(cl.getOptionValue("n"));
        final String out = cl.getOptionValue("o");
        final String config = cl.getOptionValue("c");
        final String authToken = cl.getOptionValue("t");

        final RequestGenerator generator = SerializationUtils.readValue(
                RequestGenerator.class.getClassLoader().getResourceAsStream(config),
                RequestGenerator.class);

        final OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(out));
        final DispenserConfig dispenserConfig = new DispenserConfig()
                .setDispenserHost(dolbilka ? Objects.requireNonNull(host) : DISPENSER_FAKE_HOST)
                .setServiceZombieOAuthToken(DiOAuthToken.of(authToken))
                .setClientId(DISPENSER_CLIENT_ID)
                .setEnvironment(DispenserConfig.Environment.LOAD_TESTING);
        final Dispenser dispenser = new LoggingDispenserFactory(dispenserConfig) {
            @NotNull
            @Override
            public WebClient createUnconfiguredClient(@NotNull final String address) {
                return new RequestStyleLoggingWebClient(address, s -> {
                    try {
                        writer.write(s);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }, !dolbilka);
            }
        }.get();
        generator.generate(n, dispenser);

        writer.flush();
        writer.close();
    }

}
