package ru.yandex.intranet.d.tms.jobs;

import java.util.List;

import com.yandex.ydb.table.transaction.TransactionMode;
import io.jsonwebtoken.lang.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.thymeleaf.TemplateEngine;

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
import ru.yandex.intranet.d.services.notifications.NotificationMailSenderStub;
import ru.yandex.intranet.d.services.quotas.QuotasMoveStatisticService;
import ru.yandex.intranet.d.services.transfer.TextFormatterService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.transfers.TransferRequestTypeDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateProvisionTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaResourceTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateQuotaTransferDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontCreateTransferRequestParametersDto;
import ru.yandex.intranet.d.web.model.transfers.front.FrontSingleTransferRequestDto;

/**
 * Test for cron job to statistic about quotas move
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class QuotasMoveStatisticJobTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private NotificationMailSenderStub notificationMailSenderStub;

    @Autowired
    private QuotasMoveStatisticService quotasMoveStatisticService;

    @Autowired
    private TemplateEngine emailTemplateEngine;

    @Autowired
    private TextFormatterService textFormatterService;

    @Autowired
    private YdbTableClient tableClient;

    @Autowired
    private ProvidersDao providersDao;

    @Autowired
    @Qualifier("emailMessageSource")
    private MessageSource emailMessageSource;

    @Value("${notifications.quotaMove.destination}")
    private String notificationDestination;

    @Value("${notifications.mail.from}")
    private String fromAddress;

    @Test
    public void quotasMoveStatisticJobTest() {
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

        List<Long> notificationServiceIds = List.of(2L);
        QuotasMoveStatisticJob quotasMoveStatisticJob = new QuotasMoveStatisticJob(
                quotasMoveStatisticService,
                emailMessageSource,
                emailTemplateEngine,
                textFormatterService,
                notificationMailSenderStub,
                notificationServiceIds,
                notificationDestination,
                fromAddress);
        quotasMoveStatisticJob.execute();

        Assertions.assertEquals(1, notificationMailSenderStub.getCounter());
        Assertions.assertTrue(notificationMailSenderStub.getMail().get(0).getPlainTextBody()
                .contains(transferOne.getTransfer().getId()));
        Assertions.assertTrue(notificationMailSenderStub.getMail().get(0).getPlainTextBody()
                .contains("YP-HDD-MAN: 1 GB"));
    }

    @Test
    public void quotasMoveStatisticProvisionJobTest() {
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

        List<Long> notificationServiceIds = List.of(1L);
        QuotasMoveStatisticJob quotasMoveStatisticJob = new QuotasMoveStatisticJob(
                quotasMoveStatisticService,
                emailMessageSource,
                emailTemplateEngine,
                textFormatterService,
                notificationMailSenderStub,
                notificationServiceIds,
                notificationDestination,
                fromAddress);
        quotasMoveStatisticJob.execute();

        Assertions.assertEquals(1, notificationMailSenderStub.getCounter());
        Assertions.assertTrue(notificationMailSenderStub.getMail().get(0).getPlainTextBody()
                .contains(transferTwo.getTransfer().getId()));
        Assertions.assertTrue(notificationMailSenderStub.getMail().get(0).getPlainTextBody()
                .contains("YP-HDD-MAN: 1 GB"));
    }

    @Test
    public void quotasMoveStatisticJobEmptyTest() {
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

        List<Long> notificationServiceIds = List.of(3L);
        QuotasMoveStatisticJob quotasMoveStatisticJob = new QuotasMoveStatisticJob(
                quotasMoveStatisticService,
                emailMessageSource,
                emailTemplateEngine,
                textFormatterService,
                notificationMailSenderStub,
                notificationServiceIds,
                notificationDestination,
                fromAddress);
        quotasMoveStatisticJob.execute();

        Assertions.assertEquals(0, notificationMailSenderStub.getCounter());
    }

}
