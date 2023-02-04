package ru.yandex.qe.dispenser.testing.operations.nirvana.files;

import ru.yandex.qe.dispenser.api.v1.request.DiEntityUsage;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class ReleaseUsage extends Operation {
    public ReleaseUsage(final double probability, final double retryProbability) {
        super(probability, retryProbability);
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        ctx.applyToUsageAndRemove(triple -> {
            final String project = triple.getLeft();
            final String user = triple.getMiddle();
            final DiEntityUsage usage = DiEntityUsage.singleOf(triple.getRight());
            dispenser.quotas()
                    .changeInService("nirvana")
                    .releaseEntitySharing(usage, DiPerson.login(user).chooses(project))
                    .perform();
        });
    }
}
