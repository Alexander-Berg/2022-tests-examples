package ru.yandex.qe.dispenser.testing;

import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class Scenario {
    private final Random rand = new Random();
    private final String name;
    private final int weight;
    private final List<Operation> operations;

    private Scenario(@JsonProperty("name") final String name,
                     @JsonProperty("weight") final int weight,
                     @JsonProperty("operations") final List<Operation> operations) {
        this.name = name;
        this.weight = weight;
        this.operations = operations;
    }

    public String getName() {
        return name;
    }


    public int getWeight() {
        return weight;
    }

    public void execute(final Context ctx, final Dispenser dispenser) {
        operations.stream()
                .filter(o -> o.getProbability() > rand.nextDouble())
                .forEach(o -> o.perform(ctx, dispenser));
    }
}
