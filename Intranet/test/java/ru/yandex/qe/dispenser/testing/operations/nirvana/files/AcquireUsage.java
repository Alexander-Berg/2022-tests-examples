package ru.yandex.qe.dispenser.testing.operations.nirvana.files;

import java.util.UUID;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class AcquireUsage extends Operation {
    public AcquireUsage(final double probability, final double retryProbability) {
        super(probability, retryProbability);
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        final String project = ctx.getAnyProject();
        final String user = ctx.getAnyUser();
        final String key = "fake-" + UUID.randomUUID().toString();
        final DiEntity entity = DiEntity.withKey(key)
                .bySpecification("yt-file")
                .occupies("storage", DiAmount.of(100, DiUnit.BYTE))
                .build();

        ctx.registerUsage(project, user, entity);
        dispenser.quotas()
                .changeInService("nirvana")
                .createEntity(entity, DiPerson.login(user).chooses(project))
                .perform();


    }
}
