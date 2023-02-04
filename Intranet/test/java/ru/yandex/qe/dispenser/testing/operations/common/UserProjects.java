package ru.yandex.qe.dispenser.testing.operations.common;

import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.testing.Context;
import ru.yandex.qe.dispenser.testing.operations.Operation;

public class UserProjects extends Operation {

    public UserProjects(final double probability, final double retryProbability) {
        super(probability, retryProbability);
    }

    @Override
    public void doPerform(final Context ctx, final Dispenser dispenser) {
        final String user = ctx.getAnyUser();
        dispenser.projects().get().avaliableFor(user).perform();
    }

}
