package ru.yandex.qe.dispenser.testing.operations.nirvana.workflows;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.Nullable;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class StartWorkflow extends Operation {
    private static final int MAX_WORKFLOW_BLOCKS_COUNT = 400;

    public StartWorkflow(@JsonProperty("probability") final double probability,
                         @JsonProperty("retryProbability") final @Nullable Double retryProbability) {
        super(probability, retryProbability);
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        final String project = ctx.getAnyProject();
        final String user = ctx.getAnyUser();
        final String key = "fake-workflow-" + UUID.randomUUID().toString();
        final DiEntity entity = DiEntity.withKey(key)
                .bySpecification("nirvana-workflow-start")
                .occupies("workflow-start", DiAmount.of(1, DiUnit.COUNT))
                .occupies("workflow-block-start", DiAmount.of(ctx.getRandomInt(MAX_WORKFLOW_BLOCKS_COUNT), DiUnit.COUNT))
                .build();
        dispenser.quotas()
                .changeInService("nirvana")
                .createEntity(entity, DiPerson.login(user).chooses(project))
                .perform();
    }
}
