package ru.yandex.intranet.d.web.admin.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitIds;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.providers.AccountsSettingsModel;
import ru.yandex.intranet.d.model.providers.ProviderModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.quotas.QuotasMoveStatisticDto;
import ru.yandex.intranet.d.web.model.quotas.QuotasMoveStatisticInputDto;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;

/**
 * Utils admin API tests
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class AdminUtilsApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private ProvidersDao providersDao;

    @Test
    public void quotaMoveStatisticPermissionTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.NOT_D_ADMIN_UID))
                .put()
                .uri("/admin/quotaMovementStatistic")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QuotasMoveStatisticInputDto(
                        Instant.now(),
                        Instant.now(),
                        List.of(1L, 2L)
                ))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getErrors().contains("Access denied."));
    }

    @Test
    public void quotaMoveStatisticTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto transferOne = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(transferOne);
        Assert.notNull(transferOne.getTransfer());

        QuotasMoveStatisticDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/quotaMovementStatistic")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QuotasMoveStatisticInputDto(
                        Instant.now().minus(Duration.ofDays(2)),
                        Instant.now().plus(Duration.ofDays(2)),
                        List.of(2L)
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(QuotasMoveStatisticDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTransfersCreatedIds().size());
        Assertions.assertEquals(1, result.getTransfersAppliedIds().size());
        Assertions.assertTrue(result.getTransfersAppliedIds().stream()
                .anyMatch(s -> s.equals(transferOne.getTransfer().getId())));
    }

    @Test
    public void quotaMoveStatisticProvisionTest() {
        ProviderModel provider = tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                        .getById(txSession, TestProviders.YP_ID, Tenants.DEFAULT_TENANT_ID))).block().get();
        ProviderModel providerNew = ProviderModel.builder(provider)
                .accountsSettings(AccountsSettingsModel.builder(provider.getAccountsSettings())
                        .moveProvisionSupported(true).build())
                .build();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> providersDao
                        .upsertProviderRetryable(txSession, providerNew))).block();

        FrontCreateTransferRequestDto bodyTwo = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.PROVISION_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addProvisionTransfer(new FrontCreateProvisionTransferDto(
                                "bab63272-644a-42f9-b9a5-9eeb4900fe9d",
                                TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE,
                                String.valueOf(TestServices.TEST_SERVICE_ID_CLOSING),
                                "56a41608-84df-41c4-9653-89106462e0ce",
                                TestFolders.TEST_FOLDER_1_ID,
                                String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER),
                                List.of(new FrontCreateQuotaResourceTransferDto(
                                        TestResources.YP_HDD_MAN,
                                        "-1",
                                        UnitIds.GIGABYTES
                                )),
                                List.of(new FrontCreateQuotaResourceTransferDto(
                                        TestResources.YP_HDD_MAN,
                                        "1",
                                        UnitIds.GIGABYTES
                                ))
                        ))
                        .build())
                .build();

        FrontSingleTransferRequestDto transferTwo = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(bodyTwo)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(transferTwo);
        Assert.notNull(transferTwo.getTransfer());

        QuotasMoveStatisticDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/quotaMovementStatistic")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QuotasMoveStatisticInputDto(
                        Instant.now().minus(Duration.ofDays(2)),
                        Instant.now().plus(Duration.ofDays(2)),
                        List.of(1L)
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(QuotasMoveStatisticDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTransfersCreatedIds().size());
        Assertions.assertEquals(1, result.getTransfersAppliedIds().size());
        Assertions.assertTrue(result.getTransfersAppliedIds().stream()
                .anyMatch(s -> s.equals(transferTwo.getTransfer().getId())));
    }

    @Test
    public void quotaMoveStatisticEmptyTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto transferOne = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(transferOne);
        Assert.notNull(transferOne.getTransfer());

        QuotasMoveStatisticDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/quotaMovementStatistic")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QuotasMoveStatisticInputDto(
                        Instant.now().minus(Duration.ofDays(2)),
                        Instant.now().plus(Duration.ofDays(2)),
                        List.of(3L)
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(QuotasMoveStatisticDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTransfersCreatedIds().size());
        Assertions.assertEquals(0, result.getTransfersAppliedIds().size());
    }

    @Test
    public void quotaMoveStatisticInSubtreeTest() {
        FrontCreateTransferRequestDto body = FrontCreateTransferRequestDto.builder()
                .requestType(TransferRequestTypeDto.QUOTA_TRANSFER)
                .description("")
                .addConfirmation(false)
                .parameters(FrontCreateTransferRequestParametersDto.builder()
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_SERVICE_D)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_D))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .addQuotaTransfer(FrontCreateQuotaTransferDto.builder()
                                .destinationFolderId(TestFolders.TEST_FOLDER_2_ID)
                                .destinationServiceId(String.valueOf(TestServices.TEST_SERVICE_ID_DISPENSER))
                                .addResourceTransfer(FrontCreateQuotaResourceTransferDto.builder()
                                        .delta("-1")
                                        .resourceId(TestResources.YP_HDD_MAN)
                                        .deltaUnitId(UnitIds.GIGABYTES)
                                        .build())
                                .build())
                        .build())
                .build();

        FrontSingleTransferRequestDto transferOne = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/front/transfers")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontSingleTransferRequestDto.class)
                .returnResult()
                .getResponseBody();
        Assert.notNull(transferOne);
        Assert.notNull(transferOne.getTransfer());

        QuotasMoveStatisticDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/quotaMovementStatistic")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(new QuotasMoveStatisticInputDto(
                        Instant.now().minus(Duration.ofDays(2)),
                        Instant.now().plus(Duration.ofDays(2)),
                        List.of(1L)
                ))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(QuotasMoveStatisticDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.getTransfersCreatedIds().size());
        Assertions.assertEquals(0, result.getTransfersAppliedIds().size());
    }
}
