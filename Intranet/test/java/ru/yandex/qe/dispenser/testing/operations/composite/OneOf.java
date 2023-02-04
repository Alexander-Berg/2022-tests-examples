package ru.yandex.qe.dispenser.testing.operations.composite;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class OneOf extends Operation {
    private final List<Operation> operations;
    private final double[] weights;

    public OneOf(@JsonProperty("operations") final List<Operation> operations) {
        super(operations.stream().mapToDouble(Operation::getProbability).sum(), 0.0);
        this.operations = operations;
        this.weights = new double[operations.size()];
        double sum = 0;
        for (int i = 0; i < operations.size(); ++i) {
            sum += operations.get(i).getProbability() / getProbability();
            weights[i] = sum;
        }
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        final Operation operation = selectOperation();
        operation.perform(ctx, dispenser);
    }

    private Operation selectOperation() {
        final double rnd = rand.nextDouble();
        for (int i = 0; i < weights.length; ++i) {
            if (rnd < weights[i]) {
                return operations.get(i);
            }
        }
        throw new IllegalStateException("can't select any operation");
    }
}
