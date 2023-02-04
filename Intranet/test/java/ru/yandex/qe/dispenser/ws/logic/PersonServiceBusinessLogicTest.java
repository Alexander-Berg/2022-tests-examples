package ru.yandex.qe.dispenser.ws.logic;

import org.junit.jupiter.api.Test;

import ru.yandex.qe.dispenser.api.v1.DiPersonInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersonServiceBusinessLogicTest extends BusinessLogicTestBase {
    @Test
    public void rootProjectMemberInfoShouldBeRepresentedCorrectly() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor(LYADZHIN.getLogin()).perform();
        assertTrue(personInfo.getProjectKeysByRole().get(DiPersonInfo.ProjectRole.RESPONSIBLE).isEmpty());
        assertTrue(personInfo.isMemberOfProjects(YANDEX, DEFAULT, SEARCH, VERTICALI, INFRA, INFRA_SPECIAL));
        assertTrue(personInfo.getAdminedServiceKeys().isEmpty());
        assertFalse(personInfo.isDispenserAdmin());
    }

    @Test
    public void rootProjectResponsibleInfoShouldBeRepresentedCorrectly() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor(WHISTLER.getLogin()).perform();
        assertTrue(personInfo.isResponsibleForProjects(YANDEX, DEFAULT, SEARCH, VERTICALI, INFRA, INFRA_SPECIAL));
        assertTrue(personInfo.isMemberOfProjects(YANDEX, DEFAULT, SEARCH, VERTICALI, INFRA, INFRA_SPECIAL));
        assertTrue(personInfo.getAdminedServiceKeys().isEmpty());
        assertFalse(personInfo.isDispenserAdmin());
    }

    public void defaultProjectMemberInfoShouldBeRepresentedCorrectly() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor(QDEEE.getLogin()).perform();
        assertTrue(personInfo.getProjectKeysByRole().get(DiPersonInfo.ProjectRole.RESPONSIBLE).isEmpty());
        assertTrue(personInfo.isMemberOfProjects(DEFAULT));
        assertTrue(personInfo.getAdminedServiceKeys().isEmpty());
        assertFalse(personInfo.isDispenserAdmin());
    }

    @Test
    public void serviceAdminInfoShouldBeRepresentedCorrectly() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor(SANCHO.getLogin()).perform();
        assertTrue(personInfo.isAdminOfServices(NIRVANA));
        assertFalse(personInfo.isAdminOfServices(SCRAPER));
        assertFalse(personInfo.isAdminOfServices(CLUSTER_API));
    }

    @Test
    public void dispenserAdminInfoShouldBeRepresentedCorrectly() {
        final DiPersonInfo personInfo = dispenser().persons().getInfoFor(AMOSOV_F.getLogin()).perform();
        assertTrue(personInfo.isDispenserAdmin());
    }

    @Test
    public void personInfoServiceShoulUseSessionLoginIfQueryParamIsAbsent() {
        final DiPersonInfo personInfo = dispenser().persons().getInfo().perform();
        assertEquals(ZOMB_MOBSEARCH, personInfo.getLogin());
    }
}
