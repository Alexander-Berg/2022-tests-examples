package ru.yandex.qe.dispenser.integration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.qe.dispenser.api.v1.DiCheck;
import ru.yandex.qe.dispenser.api.v1.DiCheckCompareType;
import ru.yandex.qe.dispenser.api.v1.DiCheckNotificationType;
import ru.yandex.qe.dispenser.api.v1.DiCheckType;
import ru.yandex.qe.dispenser.api.v1.DiCheckValue;
import ru.yandex.qe.dispenser.domain.Project;
import ru.yandex.qe.dispenser.ws.JugglerChecks;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JugglerChecksTest extends BaseExternalApiTest {

    @Inject
    private JugglerChecks jugglerChecks;

    public static Object[][] credentials() {
        return new Object[][]{{DiCheckNotificationType.MAIL}, {DiCheckNotificationType.TELEGRAM}, {DiCheckNotificationType.TRACKER}};
    }

    @MethodSource("credentials")
    @ParameterizedTest
    void validateJugglerCheckWorkflow(final DiCheckNotificationType type) {
        final Project project = Project.withKey("yandex").name("yandex").build();

        final List<DiCheck> allChecks = jugglerChecks.getAll(project);
        for (final DiCheck allCheck : allChecks) {
            jugglerChecks.remove(project, allCheck.getKey());
        }

        final DiCheck.Body createBody = new DiCheck.Builder()
                .setQuotaSpecKey("/nirvana/yt-cpu/yt-cpu")
                .setPersons(Collections.singleton("denblo"))
                .setCompareType(DiCheckCompareType.GREATER)
                .setAmount(DiCheckValue.percent(99d))
                .setType(DiCheckType.CRIT)
                .setNotificationTypes(EnumSet.of(type))
                .buildBody();

        final DiCheck check = jugglerChecks.create(project, createBody);

        final DiCheck createdCheck = jugglerChecks.get(project, check.getKey());
        assertCheckEqualsBody(createdCheck, createBody);

        final DiCheck.Body updateBody = new DiCheck.Builder()
                .setQuotaSpecKey("/nirvana/yt-gpu/yt-gpu")
                .setPersons(Collections.singleton("denblo"))
                .setCompareType(DiCheckCompareType.GREATER)
                .setAmount(DiCheckValue.percent(90d))
                .setType(DiCheckType.WARN)
                .setNotificationTypes(EnumSet.of(DiCheckNotificationType.TELEGRAM))
                .buildBody();
        jugglerChecks.update(project, check.getKey(), updateBody);

        final DiCheck updatedCheck = jugglerChecks.get(project, check.getKey());

        assertCheckEqualsBody(updatedCheck, updateBody);

        jugglerChecks.remove(project, check.getKey());
    }

    private void assertCheckEqualsBody(final DiCheck check, final DiCheck.Body body) {
        assertEquals(body.getQuotaSpecKey(), check.getQuotaSpecKey());
        assertEquals(body.getAmount(), check.getAmount());
        assertEquals(body.getCompareType(), check.getCompareType());
        assertEquals(body.getNotificationTypes(), check.getNotificationTypes());
        assertEquals(body.getPersons(), check.getPersons());
        assertEquals(body.getType(), check.getType());
        assertEquals(body.getSegments(), check.getSegments());
    }

}
