package ru.yandex.intranet.d.web.controllers.front;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.errors.ValidationMessages;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunAmounts;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunAnswerDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunFolderQuotaDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.ChangedEditFormField;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.OldEditFormFields;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.intranet.d.UnitIds.EXABYTES;
import static ru.yandex.intranet.d.UnitIds.GIBIBYTES;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.MEBIBYTES;
import static ru.yandex.intranet.d.UnitIds.PETABYTES;
import static ru.yandex.intranet.d.UnitIds.TEBIBYTES;
import static ru.yandex.intranet.d.UnitIds.TERABYTES;
import static ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.EditedField.ABSOLUTE;
import static ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.EditedField.DELTA;
import static ru.yandex.intranet.d.web.model.quotas.UpdateProvisionDryRunRequestDto.EditedField.UNIT;

/**
 * UpdateProvisionDryRunTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 19-11-2021
 */
@IntegrationTest
public class UpdateProvisionDryRunTest {
    @Autowired
    private WebTestClient webClient;

    @Test
    void updateProvisionDryRunWithEmptyRelatedResourcesAbsoluteChangeTest() {
        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "100", // quota
                                "20", // balance
                                "80", // provided
                                "0", // allocated
                                GIGABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "80", // providedAbsolute
                                "0", // providedDelta
                                GIGABYTES, // forEditUnitId
                                "80", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                "95", // providedAbsolute
                                null, // providedDelta
                                null // forEditUnitId
                        ), // newAmounts
                        ABSOLUTE, // editedField
                        null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("5", "GB", "5000000000", "B",
                                "5", GIGABYTES, "5", GIGABYTES))
                        .setProvidedAmount(new AmountDto("95", "GB", "95000000000", "B",
                                "95", GIGABYTES, "95", GIGABYTES))
                        .setDeltaAmount(new AmountDto("15", "GB", "15000000000", "B",
                                "15", GIGABYTES, "15", GIGABYTES))
                        .setBalance("5")
                        .setProvidedAbsolute("95")
                        .setProvidedDelta("15")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("95")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.95")
                        .setAllocatedRatio("0")
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithEmptyRelatedResourcesDeltaChangeTest() {
        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "100", // quota
                                "20", // balance
                                "80", // provided
                                "0", // allocated
                                GIGABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "80", // providedAbsolute
                                "0", // providedDelta
                                GIGABYTES, // forEditUnitId
                                "80", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                null, // providedAbsolute
                                "15", // providedDelta
                                null // forEditUnitId
                        ), // newAmounts
                        DELTA, // editedField
                        null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("5", "GB", "5000000000", "B",
                                "5", GIGABYTES, "5", GIGABYTES))
                        .setProvidedAmount(new AmountDto("95", "GB", "95000000000", "B",
                                "95", GIGABYTES, "95", GIGABYTES))
                        .setDeltaAmount(new AmountDto("15", "GB", "15000000000", "B",
                                "15", GIGABYTES, "15", GIGABYTES))
                        .setBalance("5")
                        .setProvidedAbsolute("95")
                        .setProvidedDelta("15")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("95")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.95")
                        .setAllocatedRatio("0")
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithEmptyRelatedResourcesUnitChangeTest() {
        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "100", // quota
                                "20", // balance
                                "80", // provided
                                "0", // allocated
                                GIGABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "80", // providedAbsolute
                                "0", // providedDelta
                                GIGABYTES, // forEditUnitId
                                "80", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                null, // providedAbsoluteValue
                                null, // providedDelta
                                TERABYTES // forEditUnitId
                        ), // newAmounts
                        UNIT, // editedField
                        null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "0.02", TERABYTES, "20", GIGABYTES))
                        .setProvidedAmount(new AmountDto("80", "GB", "80000000000", "B",
                                "0.08", TERABYTES, "80", GIGABYTES))
                        .setDeltaAmount(new AmountDto("0", "GB", "0", "GB",
                                "0", TERABYTES, "0", GIGABYTES))
                        .setBalance("0.02")
                        .setProvidedAbsolute("0.08")
                        .setProvidedDelta("0")
                        .setAllocated("0")
                        .setForEditUnitId(TERABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("80")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.8")
                        .setAllocatedRatio("0")
                        .build(),
                responseBody
        );

        UpdateProvisionDryRunAnswerDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "100", // quota
                                "20", // balance
                                "80", // provided
                                "0", // allocated
                                GIGABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "0.07", // providedAbsolute
                                "0.01", // providedDelta
                                TERABYTES, // forEditUnitId
                                "70", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                null, // providedAbsoluteValue
                                null, // providedDelta
                                GIGABYTES // forEditUnitId
                        ), // newAmounts
                        UNIT, // editedField
                        null
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("30", "GB", "30000000000", "B",
                                "30", GIGABYTES, "30", GIGABYTES))
                        .setProvidedAmount(new AmountDto("70", "GB", "70000000000", "B",
                                "70", GIGABYTES, "70", GIGABYTES))
                        .setDeltaAmount(new AmountDto("-10", "GB", "-10000000000", "B",
                                "-10", GIGABYTES, "-10", GIGABYTES))
                        .setBalance("30")
                        .setProvidedAbsolute("70")
                        .setProvidedDelta("-10")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("70")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.7")
                        .setAllocatedRatio("0")
                        .build(),
                responseBody2
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResource() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "20", // allocated
                GIGABYTES, // forEditUnitId
                GIGABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_HDD_MAN, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("20", "GB", "20000000000", "B",
                        "20", GIGABYTES, "20", GIGABYTES))
                .setProvidedAmount(new AmountDto("80", "GB", "80000000000", "B",
                        "80", GIGABYTES, "80", GIGABYTES))
                .setDeltaAmount(new AmountDto("60", "GB", "60000000000", "B",
                        "60", GIGABYTES, "60", GIGABYTES))
                .setBalance("20")
                .setProvidedAbsolute("80")
                .setProvidedDelta("60")
                .setAllocated("20")
                .setForEditUnitId(GIGABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("80")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("0.8")
                .setAllocatedRatio("0.2")
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setProvidedAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setDeltaAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "20", GIGABYTES, "20", GIGABYTES))
                        .setBalance("100")
                        .setProvidedAbsolute("100")
                        .setProvidedDelta("20")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("100")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.5")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceNegativeProvided() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "0", // allocated
                GIGABYTES, // forEditUnitId
                GIGABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_HDD_MAN, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "-20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("100", "GB", "100000000000", "B",
                        "100", GIGABYTES, "100", GIGABYTES))
                .setProvidedAmount(new AmountDto("0", "GB", "0", "GB",
                        "0", GIGABYTES, "0", GIGABYTES))
                .setDeltaAmount(new AmountDto("-20", "GB", "-20000000000", "B",
                        "-20", GIGABYTES, "-20", GIGABYTES))
                .setBalance("100")
                .setProvidedAbsolute("0")
                .setProvidedDelta("-20")
                .setAllocated("0")
                .setForEditUnitId(GIGABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("0")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("0")
                .setAllocatedRatio("0")
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("140", "GB", "140000000000", "B",
                                "140", GIGABYTES, "140", GIGABYTES))
                        .setProvidedAmount(new AmountDto("60", "GB", "60000000000", "B",
                                "60", GIGABYTES, "60", GIGABYTES))
                        .setDeltaAmount(new AmountDto("-20", "GB", "-20000000000", "B",
                                "-20", GIGABYTES, "-20", GIGABYTES))
                        .setBalance("140")
                        .setProvidedAbsolute("60")
                        .setProvidedDelta("-20")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("60")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.3")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceNegativeBalance() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "100", // quota
                "40", // balance
                "60", // provided
                "0", // allocated
                GIGABYTES, // forEditUnitId
                GIGABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_HDD_MAN, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("0", "GB", "0", "GB",
                        "0", GIGABYTES, "0", GIGABYTES))
                .setProvidedAmount(new AmountDto("100", "GB", "100000000000", "B",
                        "100", GIGABYTES, "100", GIGABYTES))
                .setDeltaAmount(new AmountDto("40", "GB", "40000000000", "B",
                        "40", GIGABYTES, "40", GIGABYTES))
                .setBalance("0")
                .setProvidedAbsolute("100")
                .setProvidedDelta("40")
                .setAllocated("0")
                .setForEditUnitId(GIGABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("100")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("1")
                .setAllocatedRatio("0")
                .setValidationMessages(new ValidationMessages().addWarning("newProvided",
                        "The balance is not enough to provide recommended provision. The balance is 40. " +
                                "The recommended provision is 120.").toDto())
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setProvidedAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setDeltaAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "20", GIGABYTES, "20", GIGABYTES))
                        .setBalance("100")
                        .setProvidedAbsolute("100")
                        .setProvidedDelta("20")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("100")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.5")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunRelatedResourcesWithDeletedResources() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_RAM, // resourceId
                "205", // quota
                "200", // balance
                "5", // provided
                "0", // allocated
                GIBIBYTES, // forEditUnitId
                GIBIBYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_SSD_MAN, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "100", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertEquals(0, responseBody.getRelatedResources().size());
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "20", GIGABYTES, "20", GIGABYTES))
                        .setProvidedAmount(new AmountDto("180", "GB", "180000000000", "B",
                                "180", GIGABYTES, "180", GIGABYTES))
                        .setDeltaAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setBalance("20")
                        .setProvidedAbsolute("180")
                        .setProvidedDelta("100")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("180")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.9")
                        .setAllocatedRatio("0")
                        .setRelatedResources(Collections.emptyMap())
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunToProviderWithoutRelatedResource() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.CLAUD2_RAM, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "20", // allocated
                GIBIBYTES, // forEditUnitId
                GIBIBYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.CLAUD1_RAM, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIBIBYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIBIBYTES, // forEditUnitId
                        "81920", // providedAbsoluteInMinAllowedUnit
                        MEBIBYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertEquals(0, responseBody.getRelatedResources().size());
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("100", "GiB", "107374182400", "B",
                                "100", GIBIBYTES, "102400", MEBIBYTES))
                        .setProvidedAmount(new AmountDto("100", "GiB", "107374182400", "B",
                                "100", GIBIBYTES, "102400", MEBIBYTES))
                        .setDeltaAmount(new AmountDto("20", "GiB", "21474836480", "B",
                                "20", GIBIBYTES, "20480", MEBIBYTES))
                        .setBalance("100")
                        .setProvidedAbsolute("100")
                        .setProvidedDelta("20")
                        .setAllocated("0")
                        .setForEditUnitId(GIBIBYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("102400")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.5")
                        .setAllocatedRatio("0")
                        .setRelatedResources(Collections.emptyMap())
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceFractionalMapping() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_SSD_MAN, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "0", // allocated
                GIGABYTES, // forEditUnitId
                GIGABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_HDD_SAS, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("13", "GB", "13000000000", "B",
                        "13", GIGABYTES, "13", GIGABYTES))
                .setProvidedAmount(new AmountDto("87", "GB", "87000000000", "B",
                        "87", GIGABYTES, "87", GIGABYTES))
                .setDeltaAmount(new AmountDto("67", "GB", "67000000000", "B",
                        "67", GIGABYTES, "67", GIGABYTES))
                .setBalance("13")
                .setProvidedAbsolute("87")
                .setProvidedDelta("67")
                .setAllocated("0")
                .setForEditUnitId(GIGABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("87")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("0.87")
                .setAllocatedRatio("0")
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setProvidedAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "100", GIGABYTES, "100", GIGABYTES))
                        .setDeltaAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "20", GIGABYTES, "20", GIGABYTES))
                        .setBalance("100")
                        .setProvidedAbsolute("100")
                        .setProvidedDelta("20")
                        .setAllocated("0")
                        .setForEditUnitId(GIGABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("100")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("0.5")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceUnitNotFoundTest() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "20", // allocated
                GIBIBYTES, // forEditUnitId
                GIBIBYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.YP_HDD_MAN, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIGABYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIGABYTES, // forEditUnitId
                        "80", // providedAbsoluteInMinAllowedUnit
                        GIGABYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        ErrorCollectionDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertTrue(responseBody.getErrors().contains("Unit not found."));
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceDeltaReadableTest() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "0.0000001", // quota
                "0.0000001", // balance
                "0", // provided
                "0", // allocated
                EXABYTES, // forEditUnitId
                EXABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "0.0000001", // quota
                                "0.00000002", // balance
                                "0.00000008", // provided
                                "0", // allocated
                                EXABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "0.00000008", // providedAbsolute
                                "0", // providedDelta
                                EXABYTES, // forEditUnitId
                                "80", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                null, // providedAbsoluteValue
                                "0.00000002", // providedDelta
                                null // forEditUnitId
                        ), // newAmounts
                        DELTA, // editedField
                        Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("40", "GB", "40000000000", "B",
                        "0", EXABYTES, "40", GIGABYTES))
                .setProvidedAmount(new AmountDto("60", "GB", "60000000000", "B",
                        "0", EXABYTES, "60", GIGABYTES))
                .setDeltaAmount(new AmountDto("60", "GB", "60000000000", "B",
                        "0", EXABYTES, "60", GIGABYTES))
                .setBalance("0.00000004")
                .setProvidedAbsolute("0.00000006")
                .setProvidedDelta("0.00000006")
                .setAllocated("0")
                .setForEditUnitId(EXABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("60")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("0.6")
                .setAllocatedRatio("0")
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("0", "GB", "0", "GB",
                                "0", EXABYTES, "0", GIGABYTES))
                        .setProvidedAmount(new AmountDto("100", "GB", "100000000000", "B",
                                "0.0000001", EXABYTES, "100", GIGABYTES))
                        .setDeltaAmount(new AmountDto("20", "GB", "20000000000", "B",
                                "0.00000002", EXABYTES, "20", GIGABYTES))
                        .setBalance("0")
                        .setProvidedAbsolute("0.0000001")
                        .setProvidedDelta("0.00000002")
                        .setAllocated("0")
                        .setForEditUnitId(EXABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("100")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("1")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    void updateProvisionDryRunWithRelatedResourceChangeUnitReadableTest() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.YP_HDD_SAS, // resourceId
                "1000000000000", // quota
                "211111111100", // balance
                "789999999900", // provided
                "0", // allocated
                GIGABYTES, // forEditUnitId
                GIGABYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(new UpdateProvisionDryRunRequestDto(
                        TestResources.YP_HDD_MAN, // resourceId
                        new UpdateProvisionDryRunAmounts(
                                "100000000", // quota
                                "2111111111", // balance
                                "7899999999", // provided
                                "0", // allocated
                                GIGABYTES // forEditUnitId
                        ), // oldAmounts
                        new OldEditFormFields(
                                "7899999999", // providedAbsolute
                                "0", // providedDelta
                                GIGABYTES, // forEditUnitId
                                "7899999999", // providedAbsoluteInMinAllowedUnit
                                GIGABYTES // minAllowedUnitId
                        ), // oldFormFields,
                        new ChangedEditFormField(
                                null, // providedAbsoluteValue
                                null, // providedDelta
                                PETABYTES // forEditUnitId
                        ), // newAmounts
                        UNIT, // editedField
                        Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
                ))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        UpdateProvisionDryRunAnswerDto relatedResourceAnswer = new UpdateProvisionDryRunAnswerDto.Builder()
                .setBalanceAmount(new AmountDto("211111.11", "PB", "211111111100000000000", "B",
                        "211111111100", GIGABYTES, "211111111100", GIGABYTES))
                .setProvidedAmount(new AmountDto("790000", "PB", "789999999900000000000", "B",
                        "789999999900", GIGABYTES, "789999999900", GIGABYTES))
                .setDeltaAmount(new AmountDto("0", "GB", "0", "GB",
                        "0", GIGABYTES, "0", GIGABYTES))
                .setBalance("211111111100")
                .setProvidedAbsolute("789999999900")
                .setProvidedDelta("0")
                .setAllocated("0")
                .setForEditUnitId(GIGABYTES)
                .setProvidedAbsoluteInMinAllowedUnit("789999999900")
                .setMinAllowedUnitId(GIGABYTES)
                .setProvidedRatio("0.7899999999")
                .setAllocatedRatio("0")
                .build();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertNotNull(responseBody.getRelatedResources().get(relatedResourceRequest.getResourceId()));
        assertEquals(relatedResourceAnswer, responseBody.getRelatedResources()
                .get(relatedResourceRequest.getResourceId()));
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("2111.11", "PB", "2111111111000000000", "B",
                                "2111.11", PETABYTES, "2111111111", GIGABYTES))
                        .setProvidedAmount(new AmountDto("7900", "PB", "7899999999000000000", "B",
                                "7900", PETABYTES, "7899999999", GIGABYTES))
                        .setDeltaAmount(new AmountDto("0", "GB", "0", "GB",
                                "0", PETABYTES, "0", GIGABYTES))
                        .setBalance("2111.11")
                        .setProvidedAbsolute("7900")
                        .setProvidedDelta("0")
                        .setAllocated("0")
                        .setForEditUnitId(PETABYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("7899999999")
                        .setMinAllowedUnitId(GIGABYTES)
                        .setProvidedRatio("78.99999999")
                        .setAllocatedRatio("0")
                        .setRelatedResources(
                                Map.of(relatedResourceRequest.getResourceId(), relatedResourceAnswer))
                        .build(),
                responseBody
        );
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    void updateProvisionDryRunRoundingTest() {
        var requestBuilder = new UpdateProvisionDryRunRequestDtoBuilder()
                .setResourceId(TestResources.CLAUD1_RAM)
                .setOldAmounts(new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "0", // allocated
                        GIBIBYTES // forEditUnitId
                ))
                .setOldFormFields(new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIBIBYTES, // forEditUnitId
                        "81920", // providedAbsoluteInMinAllowedUnit
                        MEBIBYTES // minAllowedUnitId
                ));

        var expectedAnswerBuilder = new UpdateProvisionDryRunAnswerDtoBuilder()
                        .setAllocated("0")
                        .setAllocatedRatio("0");

        assertEquals(
                expectedAnswerBuilder
                        .setBalance("118.15")
                        .setProvidedAbsolute("81.85")
                        .setForEditUnitId(GIBIBYTES)
                        .setProvidedDelta("1.85")
                        .setProvidedAbsoluteInMinAllowedUnit("83814")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.409248046875")
                        .setBalanceAmount(new AmountDtoBuilder()
                                .setReadableAmount("118.15", "GiB")
                                .setRawAmount("126863015936", "B")
                                .setForEditAmount("118.15", GIBIBYTES)
                                .setAmountInMinAllowedUnit("120986", MEBIBYTES)
                                .build())
                        .setProvidedAmount(new AmountDtoBuilder()
                                .setReadableAmount("81.85", "GiB")
                                .setRawAmount("87885348864", "B")
                                .setForEditAmount("81.85", GIBIBYTES)
                                .setAmountInMinAllowedUnit("83814", MEBIBYTES)
                                .build())
                        .setDeltaAmount(new AmountDtoBuilder()
                                .setReadableAmount("1.85", "GiB")
                                .setRawAmount("1986002944", "B")
                                .setForEditAmount("1.85", GIBIBYTES)
                                .setAmountInMinAllowedUnit("1894", MEBIBYTES)
                                .build())
                        .setValidationInfo(
                                "newProvided",
                                "Entered amount of the provided quota is lowered to the technically valid value"
                        )
                        .build(),
                postRequest(requestBuilder
                        .setNewProvidedAbsolute("81.85")
                ),
                "Provided absolute changed"
        );

        assertEquals(
                expectedAnswerBuilder
                        .setBalance("118.15")
                        .setProvidedAbsolute("81.85")
                        .setForEditUnitId(GIBIBYTES)
                        .setProvidedDelta("1.85")
                        .setProvidedAbsoluteInMinAllowedUnit("83814")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.409248046875")
                        .setBalanceAmount(new AmountDtoBuilder()
                                .setReadableAmount("118.15", "GiB")
                                .setRawAmount("126863015936", "B")
                                .setForEditAmount("118.15", GIBIBYTES)
                                .setAmountInMinAllowedUnit("120986", MEBIBYTES)
                                .build())
                        .setProvidedAmount(new AmountDtoBuilder()
                                .setReadableAmount("81.85", "GiB")
                                .setRawAmount("87885348864", "B")
                                .setForEditAmount("81.85", GIBIBYTES)
                                .setAmountInMinAllowedUnit("83814", MEBIBYTES)
                                .build())
                        .setDeltaAmount(new AmountDtoBuilder()
                                .setReadableAmount("1.85", "GiB")
                                .setRawAmount("1986002944", "B")
                                .setForEditAmount("1.85", GIBIBYTES)
                                .setAmountInMinAllowedUnit("1894", MEBIBYTES)
                                .build())
                        .setValidationInfo(
                                "newProvided",
                                "Entered amount of the provided quota is lowered to the technically valid value"
                        )
                        .build(),
                postRequest(requestBuilder
                        .setNewProvidedDelta("1.85")
                ),
                "Provided delta changed"
        );

        assertEquals(
                expectedAnswerBuilder
                        .setBalance("0.12")
                        .setProvidedAbsolute("0.08")
                        .setForEditUnitId(TEBIBYTES)
                        .setProvidedDelta("0")
                        .setProvidedAbsoluteInMinAllowedUnit("83814")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.409248046875")
                        .setBalanceAmount(new AmountDtoBuilder()
                                .setReadableAmount("118.15", "GiB")
                                .setRawAmount("126863015936", "B")
                                .setForEditAmount("0.11", TEBIBYTES)
                                .setAmountInMinAllowedUnit("120986", MEBIBYTES)
                                .build())
                        .setProvidedAmount(new AmountDtoBuilder()
                                .setReadableAmount("81.85", "GiB")
                                .setRawAmount("87885348864", "B")
                                .setForEditAmount("0.08", TEBIBYTES)
                                .setAmountInMinAllowedUnit("83814", MEBIBYTES)
                                .build())
                        .setDeltaAmount(new AmountDtoBuilder()
                                .setReadableAmount("1.85", "GiB")
                                .setRawAmount("1986002944", "B")
                                .setForEditAmount("0", TEBIBYTES)
                                .setAmountInMinAllowedUnit("1894", MEBIBYTES)
                                .build())
                        .setValidationInfo()
                        .build(),
                postRequest(requestBuilder
                        .setOldFormFields(new OldEditFormFields(
                                "81.85", // providedAbsolute
                                "1.85", // providedDelta
                                GIBIBYTES, // forEditUnitId
                                "83814", // providedAbsoluteInMinAllowedUnit
                                MEBIBYTES // minAllowedUnitId
                        ))
                        .setNewEditUnitId(TEBIBYTES)
                ),
                "Unit changed up"
        );

        assertEquals(
                expectedAnswerBuilder
                        .setBalance("120986")
                        .setProvidedAbsolute("83814")
                        .setForEditUnitId(MEBIBYTES)
                        .setProvidedDelta("1894")
                        .setProvidedAbsoluteInMinAllowedUnit("83814")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.409248046875")
                        .setBalanceAmount(new AmountDtoBuilder()
                                .setReadableAmount("118.15", "GiB")
                                .setRawAmount("126863015936", "B")
                                .setForEditAmount("120986", MEBIBYTES)
                                .setAmountInMinAllowedUnit("120986", MEBIBYTES)
                                .build())
                        .setProvidedAmount(new AmountDtoBuilder()
                                .setReadableAmount("81.85", "GiB")
                                .setRawAmount("87885348864", "B")
                                .setForEditAmount("83814", MEBIBYTES)
                                .setAmountInMinAllowedUnit("83814", MEBIBYTES)
                                .build())
                        .setDeltaAmount(new AmountDtoBuilder()
                                .setReadableAmount("1.85", "GiB")
                                .setRawAmount("1986002944", "B")
                                .setForEditAmount("1894", MEBIBYTES)
                                .setAmountInMinAllowedUnit("1894", MEBIBYTES)
                                .build())
                        .setValidationInfo()
                        .build(),
                postRequest(requestBuilder
                        .setOldFormFields(new OldEditFormFields(
                                "81.85", // providedAbsolute
                                "1.85", // providedDelta
                                GIBIBYTES, // forEditUnitId
                                "83814", // providedAbsoluteInMinAllowedUnit
                                MEBIBYTES // minAllowedUnitId
                        ))
                        .setNewEditUnitId(MEBIBYTES)
                ),
                "Unit changed down"
        );
    }

    @Test
    void updateProvisionDryRunToProviderWithoutRelatedResourceAllocationWarning() {
        UpdateProvisionDryRunFolderQuotaDto relatedResourceRequest = new UpdateProvisionDryRunFolderQuotaDto(
                TestResources.CLAUD2_RAM, // resourceId
                "100", // quota
                "80", // balance
                "20", // provided
                "20", // allocated
                GIBIBYTES, // forEditUnitId
                GIBIBYTES // oldFormFieldsUnitId
        );

        UpdateProvisionDryRunRequestDto body = new UpdateProvisionDryRunRequestDto(
                TestResources.CLAUD1_RAM, // resourceId
                new UpdateProvisionDryRunAmounts(
                        "200", // quota
                        "120", // balance
                        "80", // provided
                        "80", // allocated
                        GIBIBYTES // forEditUnitId
                ), // oldAmounts
                new OldEditFormFields(
                        "80", // providedAbsolute
                        "0", // providedDelta
                        GIBIBYTES, // forEditUnitId
                        "81920", // providedAbsoluteInMinAllowedUnit
                        MEBIBYTES // minAllowedUnitId
                ), // oldFormFields,
                new ChangedEditFormField(
                        null, // providedAbsolute
                        "-20", // providedDelta
                        null // forEditUnitId
                ), // newAmounts
                DELTA, // editedField
                Map.of(relatedResourceRequest.getResourceId(), relatedResourceRequest)
        );

        UpdateProvisionDryRunAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(body)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertNotNull(responseBody.getRelatedResources());
        assertEquals(0, responseBody.getRelatedResources().size());
        assertEquals(
                new UpdateProvisionDryRunAnswerDto.Builder()
                        .setBalanceAmount(new AmountDto("140", "GiB", "150323855360", "B",
                                "140", GIBIBYTES, "143360", MEBIBYTES))
                        .setProvidedAmount(new AmountDto("60", "GiB", "64424509440", "B",
                                "60", GIBIBYTES, "61440", MEBIBYTES))
                        .setDeltaAmount(new AmountDto("-20", "GiB", "-21474836480", "B",
                                "-20", GIBIBYTES, "-20480", MEBIBYTES))
                        .setBalance("140")
                        .setProvidedAbsolute("60")
                        .setProvidedDelta("-20")
                        .setAllocated("80")
                        .setForEditUnitId(GIBIBYTES)
                        .setProvidedAbsoluteInMinAllowedUnit("61440")
                        .setMinAllowedUnitId(MEBIBYTES)
                        .setProvidedRatio("0.3")
                        .setAllocatedRatio("0.4")
                        .setRelatedResources(Collections.emptyMap())
                        .setValidationMessages(new ValidationMessages().addWarning("newProvided",
                                "The provided quota cannot be less than the allocated quota").toDto())
                        .build(),
                responseBody
        );
    }

    private UpdateProvisionDryRunAnswerDto postRequest(
            UpdateProvisionDryRunRequestDtoBuilder requestBuilder
    ) {
        return webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/quotas/_updateProvisionDryRun")
                .bodyValue(requestBuilder.build())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionDryRunAnswerDto.class)
                .returnResult()
                .getResponseBody();
    }
}
