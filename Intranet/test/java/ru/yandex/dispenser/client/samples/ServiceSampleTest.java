package ru.yandex.dispenser.client.samples;

import java.util.Collection;
import java.util.Collections;

import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.request.DiQuotaState;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.RemoteDispenserFactory;

public class ServiceSampleTest {
    private final Dispenser dispenser = new RemoteDispenserFactory(new DispenserConfig()).get();

    public void createService() {
        dispenser.service("nirvana").create()
                .withName("Nirvana")
                .withAdmins("sancho", "vpdelta")
                .performBy(DiPerson.login("superuser"));
    }

    public void getService() {
        dispenser.service("nirvana").get().perform();
    }

    public void updateService() {
        dispenser.service("nirvana").update()
                .withName("Nirvana Service")
                .performBy(DiPerson.login("vpdelta"));
    }

    public void modifyAdmins() {
        dispenser.service("nirvana").admins().attach("sancho", "vpdelta").performBy(DiPerson.login("superuser"));
        dispenser.service("nirvana").admins().detach("sancho", "vpdelta").performBy(DiPerson.login("superuser"));
        dispenser.service("nirvana").admins().attach("sancho").detach("vpdelta").performBy(DiPerson.login("superuser"));
    }

    public void setActualQuotaValues() {
        final Collection<DiQuotaState> quotaStates = Collections.singletonList(
                DiQuotaState.forResource("cpu").forProject("default").withActual(DiAmount.of(100, DiUnit.CURRENCY)).build()
        );
        // TODO Single builder for state?
        dispenser.service("cluster-api").syncState().quotas().changeQuotas(quotaStates).performBy(DiPerson.login("superuser"));
    }
}
