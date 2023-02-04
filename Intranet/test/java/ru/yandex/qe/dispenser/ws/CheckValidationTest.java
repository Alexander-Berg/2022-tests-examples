package ru.yandex.qe.dispenser.ws;

import java.util.Collections;
import java.util.EnumSet;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import ru.yandex.qe.dispenser.api.v1.DiAmount;
import ru.yandex.qe.dispenser.api.v1.DiCheck;
import ru.yandex.qe.dispenser.api.v1.DiCheckCompareType;
import ru.yandex.qe.dispenser.api.v1.DiCheckNotificationType;
import ru.yandex.qe.dispenser.api.v1.DiCheckType;
import ru.yandex.qe.dispenser.api.v1.DiCheckValue;
import ru.yandex.qe.dispenser.api.v1.DiCheckValueType;
import ru.yandex.qe.dispenser.api.v1.DiUnit;
import ru.yandex.qe.dispenser.api.v1.response.DiListResponse;
import ru.yandex.qe.dispenser.ws.logic.BusinessLogicTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckValidationTest extends BusinessLogicTestBase {


    @Test
    public void checksCanBeFetched() {
        final DiListResponse<DiCheck> perform = dispenser()
                .project(YANDEX)
                .checks()
                .get()
                .perform();

        assertFalse(perform.isEmpty());
    }

    @Test
    public void checkCanBeCreated() {
        final DiCheck newCheck = dispenser()
                .project(YANDEX)
                .checks()
                .create(new DiCheck.Builder()
                        .setQuotaSpecKey("/nirvana/yt-gpu/yt-gpu")
                        .setPersons(Collections.singleton(AMOSOV_F.getLogin()))
                        .setType(DiCheckType.CRIT)
                        .setNotificationTypes(EnumSet.of(DiCheckNotificationType.MAIL))
                        .setCompareType(DiCheckCompareType.GREATER)
                        .setAmount(DiCheckValue.percent(90d))
                        .buildBody()
                ).performBy(AMOSOV_F);


        final DiCheck createdCheck = dispenser()
                .project(YANDEX)
                .checks()
                .byKey(newCheck.getKey())
                .get()
                .perform();

        assertEquals(YANDEX, createdCheck.getProjectKey());
        assertEquals("/nirvana/yt-gpu/yt-gpu", createdCheck.getQuotaSpecKey());
        assertIterableEquals(createdCheck.getPersons(), Collections.singleton(AMOSOV_F.getLogin()));
    }

    @Test
    public void checkWithAbsoluteValueCanBeCreated() {
        final DiCheck newCheck = dispenser()
                .project(YANDEX)
                .checks()
                .create(new DiCheck.Builder()
                        .setQuotaSpecKey("/nirvana/yt-gpu/yt-gpu")
                        .setPersons(Collections.singleton(AMOSOV_F.getLogin()))
                        .setType(DiCheckType.CRIT)
                        .setNotificationTypes(EnumSet.of(DiCheckNotificationType.MAIL))
                        .setCompareType(DiCheckCompareType.GREATER)
                        .setAmount(DiCheckValue.absolute(DiAmount.of(10, DiUnit.COUNT)))
                        .buildBody()
                ).performBy(AMOSOV_F);


        final DiCheck createdCheck = dispenser()
                .project(YANDEX)
                .checks()
                .byKey(newCheck.getKey())
                .get()
                .perform();

        assertEquals(YANDEX, createdCheck.getProjectKey());
        assertEquals("/nirvana/yt-gpu/yt-gpu", createdCheck.getQuotaSpecKey());
        assertIterableEquals(createdCheck.getPersons(), Collections.singleton(AMOSOV_F.getLogin()));
        assertEquals(DiCheckValueType.ABSOLUTE, createdCheck.getAmount().getType());
        assertEquals(createdCheck.getAmount().getValue(), DiAmount.of(10, DiUnit.COUNT));
    }

    @Test
    public void checkCanBeUpdated() {
        final DiListResponse<DiCheck> checks = dispenser()
                .project(YANDEX)
                .checks()
                .get()
                .perform();

        final DiCheck firstCheck = checks.iterator().next();

        dispenser()
                .project(YANDEX)
                .checks()
                .byKey(firstCheck.getKey())
                .update(new DiCheck.Builder()
                        .setQuotaSpecKey("/nirvana/storage/storage")
                        .setCompareType(DiCheckCompareType.LESS)
                        .setAmount(DiCheckValue.percent(95d))
                        .setPersons(Collections.singleton(AMOSOV_F.getLogin()))
                        .setType(DiCheckType.WARN)
                        .setNotificationTypes(EnumSet.of(DiCheckNotificationType.TELEGRAM))
                        .buildBody())
                .performBy(AMOSOV_F);

        final DiCheck updatedCheck = dispenser()
                .project(YANDEX)
                .checks()
                .byKey(firstCheck.getKey())
                .get()
                .perform();

        assertEquals("/nirvana/storage/storage", updatedCheck.getQuotaSpecKey());
        assertEquals(DiCheckCompareType.LESS, updatedCheck.getCompareType());
        assertEquals(DiCheckType.WARN, updatedCheck.getType());
    }

    @Test
    public void checkCanBeDeleted() {
        final DiListResponse<DiCheck> checks = dispenser()
                .project(YANDEX)
                .checks()
                .get()
                .perform();

        final DiCheck firstCheck = checks.iterator().next();

        dispenser()
                .project(YANDEX)
                .checks()
                .byKey(firstCheck.getKey())
                .delete()
                .performBy(AMOSOV_F);

        final DiListResponse<DiCheck> updatedChecks = dispenser()
                .project(YANDEX)
                .checks()
                .get()
                .perform();

        assertFalse(updatedChecks.stream()
                .anyMatch(check -> check.getKey().equals(firstCheck.getKey())));

    }

    public DiCheck createCheckWithAbsoluteValue(final Number value) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper().registerModules(new KotlinModule.Builder().build(),
                new Jdk8Module(), new JavaTimeModule());
        final String body = objectMapper.writeValueAsString(ImmutableMap.<String, Object>builder()
                .put("quotaSpecKey", "/nirvana/storage/storage")
                .put("compareType", "LESS")
                .put("persons", Collections.singleton(AMOSOV_F.getLogin()))
                .put("type", "CRIT")
                .put("notificationTypes", Collections.singleton("TELEGRAM"))
                .put("amount", ImmutableMap.of(
                        "type", "ABSOLUTE",
                        "value", ImmutableMap.of(
                                "value", value,
                                "unit", "GIBIBYTE"
                        ))
                )
                .build());

        return createLocalClient()
                .path("/v1/projects/" + YANDEX + "/checks")
                .replaceHeader(HttpHeaders.AUTHORIZATION, "OAuth " + AMOSOV_F)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(body)
                .readEntity(DiCheck.class);
    }

    @Test
    public void checkWithAbsoluteValueCanAcceptOnlyIntegerValues() throws JsonProcessingException {
        final DiAmount amount = (DiAmount) createCheckWithAbsoluteValue(10).getAmount().getValue();
        assertEquals(10, amount.getValue());

        assertThrows(Throwable.class, () -> {
            createCheckWithAbsoluteValue(10.5);
        });
    }

    @Test
    public void checkForSegmentedQuotaIs() {
        final DiCheck newCheck = dispenser()
                .project(YANDEX)
                .checks()
                .create(new DiCheck.Builder()
                        .setQuotaSpecKey("/" + YP + "/" + SEGMENT_CPU + "/" + SEGMENT_CPU)
                        .setPersons(Collections.singleton(AMOSOV_F.getLogin()))
                        .setType(DiCheckType.CRIT)
                        .setNotificationTypes(EnumSet.of(DiCheckNotificationType.MAIL))
                        .setCompareType(DiCheckCompareType.GREATER)
                        .setAmount(DiCheckValue.absolute(DiAmount.of(10, DiUnit.COUNT)))
                        .setSegments(Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1))
                        .buildBody()
                ).performBy(AMOSOV_F);


        final DiCheck createdCheck = dispenser()
                .project(YANDEX)
                .checks()
                .byKey(newCheck.getKey())
                .get()
                .perform();

        assertEquals(YANDEX, createdCheck.getProjectKey());
        assertEquals("/" + YP + "/" + SEGMENT_CPU + "/" + SEGMENT_CPU, createdCheck.getQuotaSpecKey());
        assertIterableEquals(createdCheck.getPersons(), Collections.singleton(AMOSOV_F.getLogin()));
        assertEquals(DiCheckValueType.ABSOLUTE, createdCheck.getAmount().getType());
        assertEquals(createdCheck.getAmount().getValue(), DiAmount.of(10, DiUnit.COUNT));
        assertEquals(Sets.newHashSet(DC_SEGMENT_1, SEGMENT_SEGMENT_1), createdCheck.getSegments());
    }
}
