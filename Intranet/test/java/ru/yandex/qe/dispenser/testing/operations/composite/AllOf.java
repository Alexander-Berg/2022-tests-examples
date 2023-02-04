package ru.yandex.qe.dispenser.testing.operations.composite;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class AllOf extends Operation {
    private final List<Operation> operations;

    public AllOf(@JsonProperty("probability") final double probability,
                 @JsonProperty("operations") final List<Operation> operations) {
        super(probability, 0.0);
        this.operations = operations;
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        operations.forEach(o -> o.perform(ctx, dispenser));
    }
}
