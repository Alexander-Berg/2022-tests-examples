package ru.yandex.qe.dispenser.testing.operations;

import java.util.Optional;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.RequestFilterWebClient;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.common.UserProjects;
import ru.yandex.qe.dispenser.testing.operations.common.UserQuotas;
import ru.yandex.qe.dispenser.testing.operations.composite.AllOf;
import ru.yandex.qe.dispenser.testing.operations.composite.OneOf;
import ru.yandex.qe.dispenser.testing.operations.nirvana.workflows.CreateWorkflow;
import ru.yandex.qe.dispenser.testing.operations.nirvana.workflows.StartWorkflow;

/**
 * NOTE: All operations extending this class should have exaclty one public constructor with the same params as this class constructor.
 * <p>
 * Created by vkokarev on 09.06.16.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "name"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AllOf.class),
        @JsonSubTypes.Type(value = OneOf.class),
        @JsonSubTypes.Type(value = UserProjects.class),
        @JsonSubTypes.Type(value = UserQuotas.class),
        @JsonSubTypes.Type(value = CreateWorkflow.class),
        @JsonSubTypes.Type(value = StartWorkflow.class),
        // TODO Add other types
})
public abstract class Operation {
    protected final Random rand = new Random();
    private final double probability;
    private final double retryProbability;

    protected Operation(@JsonProperty("probability") final double probability,
                        @JsonProperty("retryProbability") @Nullable final Double retryProbability) {
        this.probability = probability;
        this.retryProbability = Optional.ofNullable(retryProbability).orElse(0.0);
    }

    public void perform(final Context ctx, final Dispenser dispenser) {
        // TODO logging instead of stdout
        System.out.println("calling: " + getClass().getSimpleName());
        try {
            doPerform(ctx, dispenser);
        } catch (RequestFilterWebClient.SkipRequestInvocationException ignore) {
        }
        if (getRetryProbability() > rand.nextDouble()) {
            perform(ctx, dispenser);
        }
    }

    protected abstract void doPerform(final Context ctx, final Dispenser dispenser);

    public double getRetryProbability() {
        return retryProbability;
    }

    public double getProbability() {
        return probability;
    }
}
