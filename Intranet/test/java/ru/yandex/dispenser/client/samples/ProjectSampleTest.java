package ru.yandex.dispenser.client.samples;

import java.util.Arrays;

import ru.yandex.qe.dispenser.api.v1.DiPersonGroup;
import ru.yandex.qe.dispenser.client.v1.DiPerson;
import ru.yandex.qe.dispenser.client.v1.Dispenser;
import ru.yandex.qe.dispenser.client.v1.builder.PersonGroupModificationRequestBuilder;
import ru.yandex.qe.dispenser.client.v1.impl.DispenserConfig;
import ru.yandex.qe.dispenser.client.v1.impl.ProjectFilter;
import ru.yandex.qe.dispenser.client.v1.impl.RemoteDispenserFactory;

public class ProjectSampleTest {
    private final Dispenser dispenser = new RemoteDispenserFactory(new DispenserConfig()).get();

    public void getProject() {
        dispenser.projects().get().withKey("yandex").perform();
    }

    public void createProject() {
        dispenser.project("search-infra").create()
                .withName("Search Infra")
                .withResponsibles(DiPersonGroup.builder().addPersons("whistler").build())
                .withMembers(DiPersonGroup.builder().addPersons("lyadzhin", "vkokarev", "amosov-f", "starlight").build())
                .withParentProject("yandex")
                .performBy(DiPerson.login("whistler"));
    }

    public void updateProject() {
        dispenser.project("search-infra").update()
                .withName("Search Infrastructure")
                .performBy(DiPerson.login("whistler"));
    }

    public void deleteProject() {
        dispenser.project("search-infra").delete().performBy(DiPerson.login("whistler"));
    }

    public void attachMembers() {
        dispenser.project("search-infra").members().attach("inikifor").performBy(DiPerson.login("whistler"));
        dispenser.project("search-infra").members().attach("inikifor", "salavat").performBy(DiPerson.login("whistler"));
        dispenser.project("search-infra").members().attach(Arrays.asList("inikifor", "salavat")).performBy(DiPerson.login("whistler"));
    }

    public void attachMembersInFunctionalStyle() {
        final PersonGroupModificationRequestBuilder<?> membersRequest = dispenser.project("search-infra").members();
        Arrays.asList("inikifor", "salavat").stream().forEach(membersRequest::attach);
        membersRequest.performBy(DiPerson.login("whistler"));
    }

    public void detachMembers() {
        dispenser.project("search-infra").members().detach("inikifor").performBy(DiPerson.login("whistler"));
        dispenser.project("search-infra").members().detach("inikifor", "salavat").performBy(DiPerson.login("whistler"));
        dispenser.project("search-infra").members().detach(Arrays.asList("inikifor", "salavat")).performBy(DiPerson.login("whistler"));
    }

    public void modifyMembers() {
        dispenser.project("search-infra").members()
                .attach("inikifor", "salavat")
                .detach("starlight", "amosov-f")
                .performBy(DiPerson.login("whistler"));
    }

    public void modifyResponsibles() {
        dispenser.project("search-infra").responsibles().attach("lyadzhin", "den").performBy(DiPerson.login("whistler"));
        dispenser.project("search-infra").responsibles().detach("lyadzhin", "den").performBy(DiPerson.login("whistler"));
    }

    public void getUserProjects() {
        dispenser.projects().get().avaliableFor("amosov-f").perform();
        dispenser.projects().get().filterBy(ProjectFilter.onlyLeafs()).perform();
    }
}
