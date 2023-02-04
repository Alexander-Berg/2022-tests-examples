package ru.yandex.dispenser.client.samples;

import ru.yandex.qe.dispenser.api.v1.DiQuotingMode;
import ru.yandex.qe.dispenser.api.v1.DiResourceType;
import ru.yandex.qe.dispenser.api.v1.request.DiEntity;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.RemoteDispenserFactory;

public class ResourceSampleTest {
    private final Dispenser dispenser = new RemoteDispenserFactory(new DispenserConfig()).get();

    public void createResource() {
        dispenser.service("nirvana").resource("storage").create()
                .withName("Nirvana storage")
                .withType(DiResourceType.STORAGE)
                .inMode(DiQuotingMode.ENTITIES_ONLY)
                .performBy(DiPerson.login("sancho"));
    }

    public void getResource() {
        dispenser.service("nirvana").resource("storage").get().perform();
    }

    public void updateResource() {
        dispenser.service("nirvana").resource("storage").update()
                .withDescription("Nirvana data storage")
                .performBy(DiPerson.login("sancho"));
    }

    public void deleteResource() {
        dispenser.service("nirvana").resource("storage").delete().performBy(DiPerson.login("sancho"));
    }


    public void getEntities() {
        final DiEntity entity = dispenser.getEntities().inService("nirvana").bySpecification("yt-file").withKey("1234").perform();
        final DiListResponse<DiEntity> entities = dispenser.getEntities().inService("nirvana").bySpecification("yt-file").limit(1000).perform();
        final DiListResponse<DiEntity> trashEntities = dispenser.getEntities().inService("nirvana").bySpecification("storage").trashOnly().limit(1000).perform();
    }
}
