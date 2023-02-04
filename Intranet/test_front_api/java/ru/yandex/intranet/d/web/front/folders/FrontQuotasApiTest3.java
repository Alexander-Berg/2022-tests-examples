package ru.yandex.intranet.d.web.front.folders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;
import com.yandex.ydb.table.transaction.TransactionMode;
import io.grpc.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.util.function.Tuple2;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestGrpcResponses;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestServices;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitIds;
import ru.yandex.intranet.d.backend.service.provider_proto.Account;
import ru.yandex.intranet.d.backend.service.provider_proto.AccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.Amount;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundAccountsSpaceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.CompoundResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.LastUpdate;
import ru.yandex.intranet.d.backend.service.provider_proto.PassportUID;
import ru.yandex.intranet.d.backend.service.provider_proto.Provision;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceKey;
import ru.yandex.intranet.d.backend.service.provider_proto.ResourceSegmentKey;
import ru.yandex.intranet.d.backend.service.provider_proto.StaffLogin;
import ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionRequest;
import ru.yandex.intranet.d.backend.service.provider_proto.UserID;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderOperationLogDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.grpc.stub.providers.GrpcResponse;
import ru.yandex.intranet.d.grpc.stub.providers.StubProviderService;
import ru.yandex.intranet.d.loaders.units.UnitsEnsemblesLoader;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasOperationsModel;
import ru.yandex.intranet.d.model.folders.FolderOperationLogModel;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.model.units.UnitsEnsembleModel;
import ru.yandex.intranet.d.services.quotas.ProvisionService;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AccountDto;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.SortOrderDto;
import ru.yandex.intranet.d.web.model.folders.FrontAmountsDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderWithQuotesDto;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccount;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccountResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedProvider;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceType;
import ru.yandex.intranet.d.web.model.folders.front.ProviderPermission;
import ru.yandex.intranet.d.web.model.quotas.AccountsQuotasOperationsDto;
import ru.yandex.intranet.d.web.model.quotas.ProvisionLiteDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsAnswerDto;
import ru.yandex.intranet.d.web.model.quotas.UpdateProvisionsRequestDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_1_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_4_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_5_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_7_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_8_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_IN_CLOSING_SERVICE;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_IN_NON_EXPORTABLE_SERVICE;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_IN_RENAMING_SERVICE;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_4_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_5_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_7_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_CLOSING_SERVICE;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_IN_RENAMING_SERVICE;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestResourceTypes.YP_HDD;
import static ru.yandex.intranet.d.TestResourceTypes.YP_SSD;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_READ_ONLY;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_UNMANAGED;
import static ru.yandex.intranet.d.TestResources.YP_HDD_UNMANAGED_AND_READ_ONLY;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_CLOSING;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_ZERO_QUOTAS;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;
import static ru.yandex.intranet.d.UnitIds.MEBIBYTES;
import static ru.yandex.intranet.d.UnitIds.PETABYTES;
import static ru.yandex.intranet.d.UnitIds.TERABYTES;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.getBody;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.getExpectedSumsYpSSD;
import static ru.yandex.intranet.d.web.front.folders.FrontQuotasApiTest.setUpUpdateAnswer;

/**
 * FTests for page /front/quotas.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontQuotasApiTest3 {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    @Qualifier("messageSource")
    private MessageSource messages;
    @Autowired
    private ProvisionService provisionService;
    @Autowired
    private StubProviderService stubProviderService;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private FolderOperationLogDao folderOperationLogDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;
    @Autowired
    UnitsEnsemblesLoader unitsEnsemblesLoader;

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsOkResponseAdditionalQuotasReturnedTest() {
        prepareUpdateProvisionsOkResponseTestWithAdditionalQuotas(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(TestResources.YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        UnitIds.GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        UnitIds.GIGABYTES),
                new ProvisionLiteDto(TestResources.YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        UnitIds.GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        UnitIds.GIGABYTES));

        List<QuotaModel> originalQuotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<AccountsQuotasModel> originalAccountsQuotasModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> accountsQuotasDao.getByIds(txSession,
                                List.of(new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_HDD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_SSD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(prepareRequestBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<AccountsQuotasModel> accountsQuotasModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> accountsQuotasDao.getByIds(txSession,
                                List.of(new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_HDD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_SSD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<FolderOperationLogModel> folderOperationLogModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> folderOperationLogDao.getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100)
                )
        ).block();
        Assertions.assertEquals(3, originalQuotaModels.size());
        Assertions.assertEquals(2, originalAccountsQuotasModels.size());
        Assertions.assertEquals(3, quotaModels.size());
        Assertions.assertEquals(3, accountsQuotasModels.size());
        Assertions.assertEquals(3, folderOperationLogModels.size());
        Map<String, QuotaModel> originalQuotaModelsByResource = originalQuotaModels.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaModelsByResource = quotaModels.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, AccountsQuotasModel> originalAccountsQuotasModelsByResource = originalAccountsQuotasModels.stream()
                .collect(Collectors.toMap(AccountsQuotasModel::getResourceId, Function.identity()));
        Map<String, AccountsQuotasModel> accountsQuotasModelsByResource = accountsQuotasModels.stream()
                .collect(Collectors.toMap(AccountsQuotasModel::getResourceId, Function.identity()));
        QuotaModel originalHddQuota = originalQuotaModelsByResource.get(TestResources.YP_HDD_MAN);
        QuotaModel originalSsdQuota = originalQuotaModelsByResource.get(TestResources.YP_SSD_MAN);
        QuotaModel originalCpuQuota = originalQuotaModelsByResource.get(TestResources.YP_CPU_MAN);
        QuotaModel hddQuota = quotaModelsByResource.get(TestResources.YP_HDD_MAN);
        QuotaModel ssdQuota = quotaModelsByResource.get(TestResources.YP_SSD_MAN);
        QuotaModel cpuQuota = quotaModelsByResource.get(TestResources.YP_CPU_MAN);
        AccountsQuotasModel originalHddAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_HDD_MAN);
        AccountsQuotasModel originalSsdAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_SSD_MAN);
        AccountsQuotasModel originalCpuAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_CPU_MAN);
        AccountsQuotasModel hddAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_HDD_MAN);
        AccountsQuotasModel ssdAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_SSD_MAN);
        AccountsQuotasModel cpuAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_CPU_MAN);
        FolderOperationLogModel submitFolderOperationLog = folderOperationLogModels.get(1);
        FolderOperationLogModel closeFolderOperationLog = folderOperationLogModels.get(2);
        Assertions.assertEquals(originalHddQuota.getBalance(),
                submitFolderOperationLog.getOldBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(originalSsdQuota.getBalance(),
                submitFolderOperationLog.getOldBalance().get(TestResources.YP_SSD_MAN));
        // Old balance value here, balance is actually changed only with the second event when balance is increased
        Assertions.assertEquals(originalHddQuota.getBalance(),
                submitFolderOperationLog.getNewBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(ssdQuota.getBalance(),
                submitFolderOperationLog.getNewBalance().get(TestResources.YP_SSD_MAN));
        Assertions.assertEquals(originalHddAccountQuota.getProvidedQuota(), submitFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // No old value for this provision, same as zero
        Assertions.assertNull(submitFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN));
        Assertions.assertEquals(hddAccountQuota.getProvidedQuota(), submitFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        Assertions.assertEquals(ssdAccountQuota.getProvidedQuota(), submitFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN).getProvision());
        Assertions.assertEquals(originalHddQuota.getBalance(),
                closeFolderOperationLog.getOldBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(originalSsdQuota.getBalance(),
                closeFolderOperationLog.getOldBalance().get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getOldBalance().get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(hddQuota.getBalance(),
                closeFolderOperationLog.getNewBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(ssdQuota.getBalance(),
                closeFolderOperationLog.getNewBalance().get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getNewBalance().get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(originalHddAccountQuota.getProvidedQuota(), closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // No old value for this provision, same as zero
        Assertions.assertNull(closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(hddAccountQuota.getProvidedQuota(), closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        Assertions.assertEquals(ssdAccountQuota.getProvidedQuota(), closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN).getProvision());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(hddAccountQuota.getProvidedQuota(), closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_HDD_MAN).getProvision());
        Assertions.assertEquals(ssdAccountQuota.getProvidedQuota(), closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_SSD_MAN).getProvision());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(1_000_000_000_000L, originalHddQuota.getQuota());
        Assertions.assertEquals(2_000_000_000_000L, originalSsdQuota.getQuota());
        Assertions.assertEquals(1L, originalCpuQuota.getQuota());
        Assertions.assertEquals(800_000_000_000L, originalHddQuota.getBalance());
        Assertions.assertEquals(2_000_000_000_000L, originalSsdQuota.getBalance());
        Assertions.assertEquals(1L, originalCpuQuota.getBalance());
        Assertions.assertEquals(200_000_000_000L, originalHddAccountQuota.getProvidedQuota());
        Assertions.assertNull(originalSsdAccountQuota);
        Assertions.assertEquals(0L, originalCpuAccountQuota.getProvidedQuota());
        Assertions.assertEquals(100_000_000_000L, originalHddAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(0L, originalCpuAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(1_000_000_000_000L, hddQuota.getQuota());
        Assertions.assertEquals(2_000_000_000_000L, ssdQuota.getQuota());
        Assertions.assertEquals(1L, cpuQuota.getQuota());
        Assertions.assertEquals(900_000_000_000L, hddQuota.getBalance());
        Assertions.assertEquals(1_990_000_000_000L, ssdQuota.getBalance());
        Assertions.assertEquals(1L, cpuQuota.getBalance());
        Assertions.assertEquals(100_000_000_000L, hddAccountQuota.getProvidedQuota());
        Assertions.assertEquals(10_000_000_000L, ssdAccountQuota.getProvidedQuota());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertEquals(0L, cpuAccountQuota.getProvidedQuota());
        Assertions.assertEquals(100_000_000_000L, hddAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(0L, ssdAccountQuota.getAllocatedQuota());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertEquals(0L, cpuAccountQuota.getAllocatedQuota());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsOkResponseAdditionalQuotasReturnedModifiedAmountsTest() {
        prepareUpdateProvisionsOkResponseTestWithAdditionalQuotas(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(TestResources.YP_HDD_MAN, // resourceId
                        "110", // provided amount
                        UnitIds.GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        UnitIds.GIGABYTES),
                new ProvisionLiteDto(TestResources.YP_SSD_MAN, // resourceId
                        "11", // provided amount
                        UnitIds.GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        UnitIds.GIGABYTES));

        List<QuotaModel> originalQuotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<AccountsQuotasModel> originalAccountsQuotasModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> accountsQuotasDao.getByIds(txSession,
                                List.of(new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_HDD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_SSD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(prepareRequestBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk();

        List<QuotaModel> quotaModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> quotasDao.getByIds(txSession,
                                List.of(new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_HDD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_SSD_MAN),
                                        new QuotaModel.Key(TestFolders.TEST_FOLDER_1_ID, TestProviders.YP_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<AccountsQuotasModel> accountsQuotasModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> accountsQuotasDao.getByIds(txSession,
                                List.of(new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_HDD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_SSD_MAN),
                                        new AccountsQuotasModel.Identity(TestAccounts.TEST_ACCOUNT_1_ID,
                                                TestResources.YP_CPU_MAN)),
                                Tenants.DEFAULT_TENANT_ID
                        )
                )
        ).block();
        List<FolderOperationLogModel> folderOperationLogModels = ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(
                        TransactionMode.SERIALIZABLE_READ_WRITE,
                        txSession -> folderOperationLogDao.getFirstPageByFolder(txSession, Tenants.DEFAULT_TENANT_ID,
                                TestFolders.TEST_FOLDER_1_ID, SortOrderDto.ASC, 100)
                )
        ).block();
        Assertions.assertEquals(3, originalQuotaModels.size());
        Assertions.assertEquals(2, originalAccountsQuotasModels.size());
        Assertions.assertEquals(3, quotaModels.size());
        Assertions.assertEquals(3, accountsQuotasModels.size());
        Assertions.assertEquals(3, folderOperationLogModels.size());
        Map<String, QuotaModel> originalQuotaModelsByResource = originalQuotaModels.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, QuotaModel> quotaModelsByResource = quotaModels.stream()
                .collect(Collectors.toMap(QuotaModel::getResourceId, Function.identity()));
        Map<String, AccountsQuotasModel> originalAccountsQuotasModelsByResource = originalAccountsQuotasModels.stream()
                .collect(Collectors.toMap(AccountsQuotasModel::getResourceId, Function.identity()));
        Map<String, AccountsQuotasModel> accountsQuotasModelsByResource = accountsQuotasModels.stream()
                .collect(Collectors.toMap(AccountsQuotasModel::getResourceId, Function.identity()));
        QuotaModel originalHddQuota = originalQuotaModelsByResource.get(TestResources.YP_HDD_MAN);
        QuotaModel originalSsdQuota = originalQuotaModelsByResource.get(TestResources.YP_SSD_MAN);
        QuotaModel originalCpuQuota = originalQuotaModelsByResource.get(TestResources.YP_CPU_MAN);
        QuotaModel hddQuota = quotaModelsByResource.get(TestResources.YP_HDD_MAN);
        QuotaModel ssdQuota = quotaModelsByResource.get(TestResources.YP_SSD_MAN);
        QuotaModel cpuQuota = quotaModelsByResource.get(TestResources.YP_CPU_MAN);
        AccountsQuotasModel originalHddAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_HDD_MAN);
        AccountsQuotasModel originalSsdAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_SSD_MAN);
        AccountsQuotasModel originalCpuAccountQuota = originalAccountsQuotasModelsByResource
                .get(TestResources.YP_CPU_MAN);
        AccountsQuotasModel hddAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_HDD_MAN);
        AccountsQuotasModel ssdAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_SSD_MAN);
        AccountsQuotasModel cpuAccountQuota = accountsQuotasModelsByResource.get(TestResources.YP_CPU_MAN);
        FolderOperationLogModel submitFolderOperationLog = folderOperationLogModels.get(1);
        FolderOperationLogModel closeFolderOperationLog = folderOperationLogModels.get(2);
        Assertions.assertEquals(originalHddQuota.getBalance(),
                submitFolderOperationLog.getOldBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(originalSsdQuota.getBalance(),
                submitFolderOperationLog.getOldBalance().get(TestResources.YP_SSD_MAN));
        // Old balance value here, balance is actually changed only with the second event when balance is increased
        Assertions.assertEquals(originalHddQuota.getBalance(),
                submitFolderOperationLog.getNewBalance().get(TestResources.YP_HDD_MAN));
        // Requested value here for the first event
        Assertions.assertEquals(1_989_000_000_000L,
                submitFolderOperationLog.getNewBalance().get(TestResources.YP_SSD_MAN));
        Assertions.assertEquals(originalHddAccountQuota.getProvidedQuota(), submitFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // No old value for this provision, same as zero
        Assertions.assertNull(submitFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN));
        // Requested value here for the first event
        Assertions.assertEquals(110_000_000_000L, submitFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // Requested value here for the first event
        Assertions.assertEquals(11_000_000_000L, submitFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN).getProvision());
        Assertions.assertEquals(originalHddQuota.getBalance(),
                closeFolderOperationLog.getOldBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(originalSsdQuota.getBalance(),
                closeFolderOperationLog.getOldBalance().get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getOldBalance().get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(hddQuota.getBalance(),
                closeFolderOperationLog.getNewBalance().get(TestResources.YP_HDD_MAN));
        Assertions.assertEquals(ssdQuota.getBalance(),
                closeFolderOperationLog.getNewBalance().get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getNewBalance().get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(originalHddAccountQuota.getProvidedQuota(), closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // No old value for this provision, same as zero
        Assertions.assertNull(closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN));
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getOldProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_CPU_MAN));
        // Requested value in this field of the second event
        Assertions.assertEquals(110_000_000_000L, closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_HDD_MAN).getProvision());
        // Requested value in this field of the second event
        Assertions.assertEquals(11_000_000_000L, closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_SSD_MAN).getProvision());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog.getNewProvisions()
                .get(TestAccounts.TEST_ACCOUNT_1_ID).get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(hddAccountQuota.getProvidedQuota(), closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_HDD_MAN).getProvision());
        Assertions.assertEquals(ssdAccountQuota.getProvidedQuota(), closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_SSD_MAN).getProvision());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertNull(closeFolderOperationLog
                .getActuallyAppliedProvisions().get().get(TestAccounts.TEST_ACCOUNT_1_ID)
                .get(TestResources.YP_CPU_MAN));
        Assertions.assertEquals(1_000_000_000_000L, originalHddQuota.getQuota());
        Assertions.assertEquals(2_000_000_000_000L, originalSsdQuota.getQuota());
        Assertions.assertEquals(1L, originalCpuQuota.getQuota());
        Assertions.assertEquals(800_000_000_000L, originalHddQuota.getBalance());
        Assertions.assertEquals(2_000_000_000_000L, originalSsdQuota.getBalance());
        Assertions.assertEquals(1L, originalCpuQuota.getBalance());
        Assertions.assertEquals(200_000_000_000L, originalHddAccountQuota.getProvidedQuota());
        Assertions.assertNull(originalSsdAccountQuota);
        Assertions.assertEquals(0L, originalCpuAccountQuota.getProvidedQuota());
        Assertions.assertEquals(100_000_000_000L, originalHddAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(0L, originalCpuAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(1_000_000_000_000L, hddQuota.getQuota());
        Assertions.assertEquals(2_000_000_000_000L, ssdQuota.getQuota());
        Assertions.assertEquals(1L, cpuQuota.getQuota());
        Assertions.assertEquals(900_000_000_000L, hddQuota.getBalance());
        Assertions.assertEquals(1_990_000_000_000L, ssdQuota.getBalance());
        Assertions.assertEquals(1L, cpuQuota.getBalance());
        Assertions.assertEquals(100_000_000_000L, hddAccountQuota.getProvidedQuota());
        Assertions.assertEquals(10_000_000_000L, ssdAccountQuota.getProvidedQuota());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertEquals(0L, cpuAccountQuota.getProvidedQuota());
        Assertions.assertEquals(100_000_000_000L, hddAccountQuota.getAllocatedQuota());
        Assertions.assertEquals(0L, ssdAccountQuota.getAllocatedQuota());
        // Request does not modify this quota, therefore response is ignored
        Assertions.assertEquals(0L, cpuAccountQuota.getAllocatedQuota());
    }

    private UpdateProvisionsRequestDto.Builder prepareRequestBody() {
        return UpdateProvisionsRequestDto.builder()
                .setAccountId(TestAccounts.TEST_ACCOUNT_1_ID)
                .setFolderId(TestFolders.TEST_FOLDER_1_ID)
                .setServiceId(TestServices.TEST_SERVICE_ID_DISPENSER);
    }

    private void prepareUpdateProvisionsOkResponseTestWithAdditionalQuotas(StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("ssd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(10)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("cpu")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(500)
                                        .setUnitKey("millicores")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(50)
                                        .setUnitKey("millicores")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
    }



    @Test
    public void addProvisionsToClosingService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_CLOSING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_CLOSING)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "60",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Current service status is not allowed."));
                });
    }

    @Test
    public void removeProvisionsFromClosingService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_CLOSING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_CLOSING)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "40",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class);
    }

    @Test
    public void removeProvisionsWithDifferenceUnitsFromClosingService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_CLOSING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_CLOSING)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "0",
                                TERABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class);
    }

    @Test
    public void addProvisionsToNonExportableService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_NON_EXPORTABLE_SERVICE)
                .setFolderId(TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_NON_EXPORTABLE)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "60",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Set<String> errors = errorCollection.getFieldErrors().get("serviceId");
                    Assertions.assertNotNull(errors);
                    Assertions.assertTrue(errors.contains("Services in the sandbox are not allowed."));
                });
    }

    @Test
    public void removeProvisionsFromNonExportableService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_NON_EXPORTABLE_SERVICE)
                .setFolderId(TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_NON_EXPORTABLE)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "40",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class);
    }

    @Test
    public void addProvisionsToRenamingService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_RENAMING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_RENAMING_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_RENAMING)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "60",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class);
    }

    @Test
    public void removeProvisionsFromRenamingService() {
        setUpUpdateAnswer(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_RENAMING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_RENAMING_SERVICE)
                .setServiceId(TestServices.TEST_SERVICE_ID_RENAMING)
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "40",
                                GIGABYTES,
                                "50",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class);
    }

    @Test
    public void updateProvisionsCorrectBalanceSumsAfterUpdateTest() {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(1)
                                        .setUnitKey("terabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(0)
                                        .setUnitKey("terabytes")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("sas")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));

        FrontFolderWithQuotesDto frontFolderWithQuotesDto = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_4_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(frontFolderWithQuotesDto);
        ExpandedResourceType resourceType = frontFolderWithQuotesDto.getFolders().stream()
                .filter(ef -> ef.getFolder().getId().equals(TEST_FOLDER_4_ID))
                .findFirst()
                .orElseThrow()
                .getProviders()
                .stream()
                .filter(ep -> ep.getProviderId().equals(YP_ID))
                .findFirst()
                .orElseThrow()
                .getResourceTypes()
                .stream()
                .filter(rt -> rt.getResourceTypeId().equals(YP_HDD))
                .findFirst()
                .orElseThrow();
        AmountDto balance = resourceType
                .getSums()
                .getBalance();

        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_SAS, // resourceId
                        "1", // provided amount
                        "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5", // provided amount unit key
                        "0", // old provided amount
                        "a9d95bd2-3219-4b6b-980d-bed9b38d3cb5"));

        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody()
                        .setServiceId(12L)
                        .setFolderId(TEST_FOLDER_4_ID)
                        .setAccountId(TEST_ACCOUNT_4_ID)
                        .setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        BigDecimal amount = new BigDecimal(balance.getAmountInMinAllowedUnit())
                .subtract(new BigDecimal(1_000_000_000_000L));
        Optional<UnitsEnsembleModel> block =
                unitsEnsemblesLoader.getUnitsEnsembleByIdImmediate("b02344bf-96af-4cc5-937c-66a479989ce8",
                        Tenants.DEFAULT_TENANT_ID)
                        .block();
        Assertions.assertNotNull(block);
        Assertions.assertEquals(
                new AmountDto("99000001999.99", "PB",
                        "99000001999992", "TB",
                        "99000001999.99", PETABYTES,
                        "99000001999992", TERABYTES),
                responseBody.getExpandedProvider().getResourceTypes().stream()
                        .filter(rt -> rt.getResourceTypeId().equals(YP_HDD))
                        .findFirst()
                        .orElseThrow()
                        .getSums()
                        .getBalance());
    }

    @Test
    public void updateProvisionsWithUnmanagedResources() {
        setUpUpdateAnswer(stubProviderService);

        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(TEST_ACCOUNT_7_ID)
                                .setFolderId(TEST_FOLDER_5_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId(YP_HDD_UNMANAGED)
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(0L)
                                .setAllocatedQuota(0L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(TEST_ACCOUNT_7_ID)
                                .setFolderId(TEST_FOLDER_5_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId(YP_HDD_UNMANAGED_AND_READ_ONLY)
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(0L)
                                .setAllocatedQuota(0L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();

        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_7_ID)
                .setFolderId(TEST_FOLDER_5_ID)
                .setServiceId(17L)
                .setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "100",
                                GIGABYTES,
                                "200",
                                GIGABYTES),
                        new ProvisionLiteDto(YP_HDD_READ_ONLY,
                                "200",
                                GIGABYTES,
                                "200",
                                GIGABYTES),
                        new ProvisionLiteDto(YP_HDD_UNMANAGED,
                                "1",
                                GIGABYTES,
                                "0",
                                GIGABYTES),
                        new ProvisionLiteDto(YP_HDD_UNMANAGED_AND_READ_ONLY,
                                "1",
                                GIGABYTES,
                                "0",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(result -> {
                    Assertions.assertNotNull(result);
                    ErrorCollectionDto errorCollection = result.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Assertions.assertNotNull(errorCollection.getFieldErrors());
                    Map<String, Set<String>> errors = errorCollection.getFieldErrors();
                    Assertions.assertEquals(2,
                        errors.entrySet().stream()
                            .filter(itm -> itm.getValue().contains("Resource is not managed."))
                            .count()
                    );
                });
    }

    @Test
    public void checkAbsenceUnmanagedResourcesInRequestToProvider() {
        setUpUpdateAnswer(stubProviderService);

        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(TEST_ACCOUNT_7_ID)
                                .setFolderId(TEST_FOLDER_5_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId(YP_HDD_UNMANAGED)
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(0L)
                                .setAllocatedQuota(0L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(TEST_ACCOUNT_7_ID)
                                .setFolderId(TEST_FOLDER_5_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId(YP_HDD_UNMANAGED_AND_READ_ONLY)
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(0L)
                                .setAllocatedQuota(0L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();

        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_7_ID)
                .setFolderId(TEST_FOLDER_5_ID)
                .setServiceId(17L)
                .setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_HDD_MAN,
                                "100",
                                GIGABYTES,
                                "200",
                                GIGABYTES),
                        new ProvisionLiteDto(YP_HDD_READ_ONLY,
                                "200",
                                GIGABYTES,
                                "200",
                                GIGABYTES)))
                .build();

        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult();

        Assertions.assertEquals(1L, stubProviderService.getUpdateProvisionCallCount());
        UpdateProvisionRequest requestToProvider = stubProviderService.getUpdateProvisionRequests().getFirst().getT1();
        Assertions.assertNotNull(requestToProvider);
        Assertions.assertEquals(1L, requestToProvider.getUpdatedProvisionsCount());
        Assertions.assertEquals("hdd", requestToProvider.getUpdatedProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(1L, requestToProvider.getKnownProvisionsCount());
        Assertions.assertEquals(1L, requestToProvider.getKnownProvisions(0).getKnownProvisionsCount());
        Assertions.assertEquals("hdd", requestToProvider.getKnownProvisions(0).getKnownProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
    }

    @Test
    public void checkSumAllocatedResourcesInFolderFromOtherAccounts() {
        setUpUpdateAnswer(stubProviderService);

        String accountId = UUID.randomUUID().toString();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsDao.upsertOneRetryable(txSession, new AccountModel.Builder()
                                .setId(accountId)
                                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setProviderId(TestProviders.YP_ID)
                                .setOuterAccountKeyInProvider("keyAccount")
                                .setOuterAccountIdInProvider("123321")
                                .setLastAccountUpdate(Instant.now())
                                .setDisplayName("Account name")
                                .setVersion(1L)
                                .setLastReceivedVersion(1L)
                                .build()
                        ))).block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(accountId)
                                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId(YP_HDD_MAN)
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(200000000000L)
                                .setAllocatedQuota(200000000000L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();

        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_IN_CLOSING_SERVICE)
                .setFolderId(TEST_FOLDER_IN_CLOSING_SERVICE)
                .setServiceId(TEST_SERVICE_ID_CLOSING)
                .setUpdatedProvisions(List.of(
                        new ProvisionLiteDto(YP_SSD_MAN,
                                "800",
                                GIGABYTES,
                                "50000",
                                GIGABYTES)))
                .build();

        UpdateProvisionsAnswerDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getExpandedProvider());
        Assertions.assertNotNull(result.getExpandedProvider().getResourceTypes());
        ExpandedResourceType expandedResourceType = result.getExpandedProvider().getResourceTypes()
                .stream().filter(resourceType -> resourceType.getResourceTypeId().equals(YP_HDD)).findFirst()
                .orElse(null);
        Assertions.assertNotNull(expandedResourceType);
        Assertions.assertNotNull(expandedResourceType.getSums());
        Assertions.assertNotNull(expandedResourceType.getSums().getAllocated());
        Assertions.assertEquals("210000000000", expandedResourceType.getSums().getAllocated().getRawAmount());
    }

    public static void prepareFailedPreconditionWithEmptyResponseTest(StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(TestGrpcResponses.failedPreconditionTestResponse()));

        stubProviderService.setGetAccountResponses(List.of(GrpcResponse
                .success(Account.newBuilder()
                        .setAccountId(TEST_ACCOUNT_5_ID)
                        .setDeleted(false)
                        .setDisplayName("test")
                        .setFolderId(TEST_FOLDER_4_ID)
                        .setKey("test")
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
    }

    /**
     * Update provisions failed precondition with empty response test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsFailedPreconditionWithEmptyResponseTest() {
        prepareFailedPreconditionWithEmptyResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YDB_RAM_SAS, // resourceId
                        "100", // provided amount
                        MEBIBYTES, // provided amount unit key
                        "0", // old provided amount
                        MEBIBYTES));
        ErrorCollectionDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(UpdateProvisionsRequestDto.builder()
                        .setAccountId(TEST_ACCOUNT_5_ID)
                        .setFolderId(TEST_FOLDER_4_ID)
                        .setServiceId(12L)
                        .setUpdatedProvisions(Collections.singletonList(
                                new ProvisionLiteDto(YDB_RAM_SAS, // resourceId
                                        "1", // provided amount
                                        MEBIBYTES, // provided amount unit id
                                        "0", // old provided amount
                                        MEBIBYTES)))
                        .build())
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of("Error occurred while performing the operation on the provider side."),
                responseBody.getErrors());
        Assertions.assertNotNull(responseBody.getDetails());
        Assertions.assertEquals(1, responseBody.getDetails().get("errorFromProvider").size());
        String errorFromProvider = (String) responseBody.getDetails().get("errorFromProvider")
                .stream().findFirst().get();
        Assertions.assertTrue(errorFromProvider.startsWith("""
                FAILED_PRECONDITION Test error Test error
                test: Test error description
                Request id:"""));
    }

    public static void prepareUpdateProvisionsDoesNotReturnCompletelyZeroQuotasTest(
            StubProviderService stubProviderService) {
        stubProviderService.reset();
        stubProviderService.setUpdateProvisionResponses(List.of(GrpcResponse
                .success(ru.yandex.intranet.d.backend.service.provider_proto.UpdateProvisionResponse.newBuilder()
                        .addProvisions(Provision.newBuilder()
                                .setResourceKey(ResourceKey.newBuilder()
                                        .setCompoundKey(CompoundResourceKey.newBuilder()
                                                .setResourceTypeKey("hdd")
                                                .build())
                                        .build())
                                .setProvided(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setAllocated(Amount.newBuilder()
                                        .setValue(100)
                                        .setUnitKey("gigabytes")
                                        .build())
                                .setLastUpdate(LastUpdate.newBuilder()
                                        .setAuthor(UserID.newBuilder()
                                                .setPassportUid(PassportUID.newBuilder()
                                                        .setPassportUid("1")
                                                        .build())
                                                .setStaffLogin(StaffLogin.newBuilder()
                                                        .setStaffLogin("test")
                                                        .build())
                                                .build())
                                        .setOperationId(UUID.randomUUID().toString())
                                        .setTimestamp(Timestamps.fromSeconds(Instant.now().getEpochSecond()))
                                        .build())
                                .build())
                        .setAccountsSpaceKey(AccountsSpaceKey.newBuilder()
                                .setCompoundKey(CompoundAccountsSpaceKey.newBuilder()
                                        .addAllResourceSegmentKeys(List.of(
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("location")
                                                        .setResourceSegmentKey("man")
                                                        .build(),
                                                ResourceSegmentKey.newBuilder()
                                                        .setResourceSegmentationKey("segment")
                                                        .setResourceSegmentKey("default")
                                                        .build()
                                        ))
                                        .build())
                                .build())
                        .build())));
    }

    /**
     * Update provisions does not return completely zero quotas test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontQuotasController#updateProvision
     */
    @Test
    public void updateProvisionsDoesNotReturnCompletelyZeroQuotasTest() {
        prepareUpdateProvisionsDoesNotReturnCompletelyZeroQuotasTest(stubProviderService);
        UpdateProvisionsRequestDto body = UpdateProvisionsRequestDto.builder()
                .setAccountId(TEST_ACCOUNT_8_ID)
                .setFolderId(TEST_FOLDER_7_ID)
                .setServiceId(TEST_SERVICE_ID_ZERO_QUOTAS) // Dispenser
                .setUpdatedProvisions(Collections.singletonList(
                        new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                                "100", // provided amount
                                GIGABYTES, // provided amount unit id
                                "200", // old provided amount
                                GIGABYTES)) // old provided amount unit id
                ).build();
        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        ExpandedProvider expandedProvider = responseBody.getExpandedProvider();
        List<ExpandedResourceType> resourceTypes = expandedProvider.getResourceTypes();
        Assertions.assertNotNull(resourceTypes);
        Assertions.assertEquals(1, resourceTypes.size());

        Map<String, ExpandedResourceType> resourceTypeMap = resourceTypes.stream()
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ExpandedResourceType resourceType = resourceTypeMap.get(YP_HDD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_HDD, resourceType.getResourceTypeId());
        List<ExpandedResource> resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());

        List<ExpandedAccount> accounts = expandedProvider.getAccounts();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(1, accounts.size());

        ExpandedAccount expandedAccount = accounts.iterator().next();
        Assertions.assertNotNull(expandedAccount);

        AccountDto account = expandedAccount.getAccount();
        Assertions.assertNotNull(account);
        Assertions.assertEquals(new AccountDto(
                TEST_ACCOUNT_8_ID,
                "YP account with completely zero quotas",
                TEST_FOLDER_7_ID,
                false,
                TEST_ACCOUNT_SPACE_3_ID,
                false,
                YP_ID, null), account);
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsMultipleKnownAccountsTest() {
        String anotherAccountId = "5f66db03-f9fb-4e11-b9a6-a4501107dac3";
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsDao.upsertOneRetryable(txSession, new AccountModel.Builder()
                                .setId(anotherAccountId)
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setProviderId(TestProviders.YP_ID)
                                .setOuterAccountKeyInProvider("keyAccount")
                                .setOuterAccountIdInProvider("456")
                                .setLastAccountUpdate(Instant.now())
                                .setDisplayName("Account name")
                                .setVersion(1L)
                                .setLastReceivedVersion(1L)
                                .setAccountsSpacesId(TEST_ACCOUNT_SPACE_3_ID)
                                .build()
                        ))).block();
        FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        ExpandedProvider expandedProvider = responseBody.getExpandedProvider();
        List<ExpandedResourceType> resourceTypes = expandedProvider.getResourceTypes();
        Assertions.assertNotNull(resourceTypes);
        Assertions.assertEquals(3, resourceTypes.size());

        Map<String, ExpandedResourceType> resourceTypeMap = resourceTypes.stream()
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ExpandedResourceType resourceType = resourceTypeMap.get(YP_HDD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_HDD, resourceType.getResourceTypeId());
        List<ExpandedResource> resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(2, resources.size());

        List<ExpandedResource> expectedResources = FrontQuotasApiTest.getExpectedResourcesYpHDD();
        Assertions.assertEquals(expectedResources, resources);

        FrontAmountsDto sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(FrontQuotasApiTest.getExpectedSumsYpHDD(), sums);

        resourceType = resourceTypeMap.get(YP_SSD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_SSD, resourceType.getResourceTypeId());
        resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());

        expectedResources = FrontQuotasApiTest.getExpectedResourcesYpSSD();
        Assertions.assertEquals(expectedResources, resources);

        sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(getExpectedSumsYpSSD(), sums);

        List<ExpandedAccount> accounts = expandedProvider.getAccounts();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(2, accounts.size());

        Map<String, ExpandedAccount> accountsById = accounts.stream()
                .collect(Collectors.toMap(v -> v.getAccount().getId(), Function.identity()));
        ExpandedAccount expandedAccount = accountsById.get(TEST_ACCOUNT_1_ID);
        Assertions.assertNotNull(expandedAccount);
        Assertions.assertTrue(accountsById.containsKey(anotherAccountId));

        AccountDto account = expandedAccount.getAccount();
        Assertions.assertNotNull(account);
        Assertions.assertEquals(new AccountDto(
                TEST_ACCOUNT_1_ID,
                "тестовый аккаунт",
                TEST_FOLDER_1_ID,
                false,
                TEST_ACCOUNT_SPACE_3_ID,
                false,
                YP_ID, null), account);

        List<ExpandedAccountResource> resources1 = expandedAccount.getResources();
        Assertions.assertNotNull(resources1);
        Assertions.assertEquals(2, resources1.size());

        List<ExpandedAccountResource> expectedExpandedResource = FrontQuotasApiTest.getExpectedExpandedResource();
        Assertions.assertEquals(expectedExpandedResource, resources1);

        Assertions.assertEquals(1L, stubProviderService.getUpdateProvisionCallCount());
        UpdateProvisionRequest requestToProvider = stubProviderService.getUpdateProvisionRequests().getFirst().getT1();
        Assertions.assertNotNull(requestToProvider);
        Assertions.assertEquals(3L, requestToProvider.getUpdatedProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getUpdatedProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getUpdatedProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getUpdatedProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(80L, requestToProvider.getUpdatedProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(100000000, requestToProvider.getUpdatedProvisions(1)
                .getProvided().getValue()); //100000000 KB (providerApiUnit is KB)
        Assertions.assertEquals(10L, requestToProvider.getUpdatedProvisions(2)
                .getProvided().getValue());
        Assertions.assertEquals(2L, requestToProvider.getKnownProvisionsCount());
        Assertions.assertEquals(3L, requestToProvider.getKnownProvisions(0).getKnownProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getKnownProvisions(0).getKnownProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getKnownProvisions(0).getKnownProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getKnownProvisions(0).getKnownProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(80L, requestToProvider.getKnownProvisions(0).getKnownProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(200000000L, requestToProvider.getKnownProvisions(0).getKnownProvisions(1)
                .getProvided().getValue()); //200000000 KB (providerApiUnit is KB)
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(0).getKnownProvisions(2)
                .getProvided().getValue());
        Assertions.assertEquals(3L, requestToProvider.getKnownProvisions(1).getKnownProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getKnownProvisions(1).getKnownProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getKnownProvisions(1).getKnownProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getKnownProvisions(1).getKnownProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(1).getKnownProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(1).getKnownProvisions(1)
                .getProvided().getValue());
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(1).getKnownProvisions(2)
                .getProvided().getValue());
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void updateProvisionsMultipleNonEmptyKnownAccountsTest() {
        String anotherAccountId = "5f66db03-f9fb-4e11-b9a6-a4501107dac3";
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsDao.upsertOneRetryable(txSession, new AccountModel.Builder()
                                .setId(anotherAccountId)
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setProviderId(TestProviders.YP_ID)
                                .setOuterAccountKeyInProvider("keyAccount")
                                .setOuterAccountIdInProvider("456")
                                .setLastAccountUpdate(Instant.now())
                                .setDisplayName("Account name")
                                .setVersion(1L)
                                .setLastReceivedVersion(1L)
                                .setAccountsSpacesId(TEST_ACCOUNT_SPACE_3_ID)
                                .build()
                        ))).block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                session.usingTxMonoRetryable(TransactionMode.SERIALIZABLE_READ_WRITE, txSession ->
                        accountsQuotasDao.upsertOneRetryable(txSession, new AccountsQuotasModel.Builder()
                                .setAccountId(anotherAccountId)
                                .setFolderId(TEST_FOLDER_1_ID)
                                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                                .setResourceId("14e2705c-ff49-43a4-8048-622e373f5891")
                                .setProviderId(TestProviders.YP_ID)
                                .setProvidedQuota(200000000000L)
                                .setAllocatedQuota(200000000000L)
                                .setLastProvisionUpdate(Instant.now())
                                .build()
                        ))).block();
        FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        ExpandedProvider expandedProvider = responseBody.getExpandedProvider();
        List<ExpandedResourceType> resourceTypes = expandedProvider.getResourceTypes();
        Assertions.assertNotNull(resourceTypes);
        Assertions.assertEquals(3, resourceTypes.size());

        Map<String, ExpandedResourceType> resourceTypeMap = resourceTypes.stream()
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ExpandedResourceType resourceType = resourceTypeMap.get(YP_HDD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_HDD, resourceType.getResourceTypeId());
        List<ExpandedResource> resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(2, resources.size());

        List<ExpandedResource> expectedResources = FrontQuotasApiTest.getExpectedResourcesYpHDD();
        Assertions.assertEquals(expectedResources, resources);

        FrontAmountsDto sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(FrontQuotasApiTest.getExpectedSumsYpHDD(), sums);

        resourceType = resourceTypeMap.get(YP_SSD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_SSD, resourceType.getResourceTypeId());
        resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());

        expectedResources = FrontQuotasApiTest.getExpectedResourcesYpSSD();
        Assertions.assertEquals(expectedResources, resources);

        sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(getExpectedSumsYpSSD(), sums);

        List<ExpandedAccount> accounts = expandedProvider.getAccounts();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(2, accounts.size());

        Map<String, ExpandedAccount> accountsById = accounts.stream()
                .collect(Collectors.toMap(v -> v.getAccount().getId(), Function.identity()));
        ExpandedAccount expandedAccount = accountsById.get(TEST_ACCOUNT_1_ID);
        Assertions.assertNotNull(expandedAccount);
        Assertions.assertTrue(accountsById.containsKey(anotherAccountId));

        AccountDto account = expandedAccount.getAccount();
        Assertions.assertNotNull(account);
        Assertions.assertEquals(new AccountDto(
                TEST_ACCOUNT_1_ID,
                "тестовый аккаунт",
                TEST_FOLDER_1_ID,
                false,
                TEST_ACCOUNT_SPACE_3_ID,
                false,
                YP_ID, null), account);

        List<ExpandedAccountResource> resources1 = expandedAccount.getResources();
        Assertions.assertNotNull(resources1);
        Assertions.assertEquals(2, resources1.size());

        List<ExpandedAccountResource> expectedExpandedResource = FrontQuotasApiTest.getExpectedExpandedResource();
        Assertions.assertEquals(expectedExpandedResource, resources1);

        Assertions.assertEquals(1L, stubProviderService.getUpdateProvisionCallCount());
        UpdateProvisionRequest requestToProvider = stubProviderService.getUpdateProvisionRequests().getFirst().getT1();
        Assertions.assertNotNull(requestToProvider);
        Assertions.assertEquals(3L, requestToProvider.getUpdatedProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getUpdatedProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getUpdatedProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getUpdatedProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(80L, requestToProvider.getUpdatedProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(100000000L, requestToProvider.getUpdatedProvisions(1)
                .getProvided().getValue()); //100000000 KB (providerApiUnit is KB)
        Assertions.assertEquals(10L, requestToProvider.getUpdatedProvisions(2)
                .getProvided().getValue());
        Assertions.assertEquals(2L, requestToProvider.getKnownProvisionsCount());
        Assertions.assertEquals(3L, requestToProvider.getKnownProvisions(0).getKnownProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getKnownProvisions(0).getKnownProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getKnownProvisions(0).getKnownProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getKnownProvisions(0).getKnownProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(80L, requestToProvider.getKnownProvisions(0).getKnownProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(200000000L, requestToProvider.getKnownProvisions(0).getKnownProvisions(1)
                .getProvided().getValue()); //200000000 KB (providerApiUnit is KB)
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(0).getKnownProvisions(2)
                .getProvided().getValue());
        Assertions.assertEquals(3L, requestToProvider.getKnownProvisions(1).getKnownProvisionsCount());
        Assertions.assertEquals("ram", requestToProvider.getKnownProvisions(1).getKnownProvisions(0)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("hdd", requestToProvider.getKnownProvisions(1).getKnownProvisions(1)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals("ssd", requestToProvider.getKnownProvisions(1).getKnownProvisions(2)
                .getResourceKey().getCompoundKey().getResourceTypeKey());
        Assertions.assertEquals(200L, requestToProvider.getKnownProvisions(1).getKnownProvisions(0)
                .getProvided().getValue());
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(1).getKnownProvisions(1)
                .getProvided().getValue());
        Assertions.assertEquals(0L, requestToProvider.getKnownProvisions(1).getKnownProvisions(2)
                .getProvided().getValue());
    }

    @Test
    public void updateProvisionsProviderAdminOkResponseTest() {
        FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        UpdateProvisionsAnswerDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UpdateProvisionsAnswerDto.class)
                .returnResult()
                .getResponseBody();

        Tuple2<UpdateProvisionRequest, Metadata> next =
                stubProviderService.getUpdateProvisionRequests().iterator().next();
        Assertions.assertNotNull(next);
        UpdateProvisionRequest t1 = next.getT1();
        Assertions.assertNotNull(t1);
        String operationId = t1.getOperationId();

        Assertions.assertNotNull(responseBody);
        Assertions.assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                responseBody.getExpandedProvider().getPermissions());
        AccountsQuotasOperationsDto accountsQuotasOperationsDto = responseBody.getAccountsQuotasOperationsDto();
        Assertions.assertNotNull(accountsQuotasOperationsDto);
        AccountsQuotasOperationsDto expectedAccountsQuotasOperationsDto = new AccountsQuotasOperationsDto(operationId,
                AccountsQuotasOperationsModel.RequestStatus.OK);
        Assertions.assertEquals(expectedAccountsQuotasOperationsDto, accountsQuotasOperationsDto);

        ExpandedProvider expandedProvider = responseBody.getExpandedProvider();
        List<ExpandedResourceType> resourceTypes = expandedProvider.getResourceTypes();
        Assertions.assertNotNull(resourceTypes);
        Assertions.assertEquals(3, resourceTypes.size());

        Map<String, ExpandedResourceType> resourceTypeMap = resourceTypes.stream()
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ExpandedResourceType resourceType = resourceTypeMap.get(YP_HDD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_HDD, resourceType.getResourceTypeId());
        List<ExpandedResource> resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(2, resources.size());

        List<ExpandedResource> expectedResources = FrontQuotasApiTest.getExpectedResourcesYpHDD();
        Assertions.assertEquals(expectedResources, resources);

        FrontAmountsDto sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(FrontQuotasApiTest.getExpectedSumsYpHDD(), sums);

        resourceType = resourceTypeMap.get(YP_SSD);
        Assertions.assertNotNull(resourceType);
        Assertions.assertEquals(YP_SSD, resourceType.getResourceTypeId());
        resources = resourceType.getResources();
        Assertions.assertNotNull(resources);
        Assertions.assertEquals(1, resources.size());

        expectedResources = FrontQuotasApiTest.getExpectedResourcesYpSSD();
        Assertions.assertEquals(expectedResources, resources);

        sums = resourceType.getSums();
        Assertions.assertNotNull(sums);

        Assertions.assertEquals(getExpectedSumsYpSSD(), sums);

        List<ExpandedAccount> accounts = expandedProvider.getAccounts();
        Assertions.assertNotNull(accounts);
        Assertions.assertEquals(1, accounts.size());

        ExpandedAccount expandedAccount = accounts.iterator().next();
        Assertions.assertNotNull(expandedAccount);

        AccountDto account = expandedAccount.getAccount();
        Assertions.assertNotNull(account);
        Assertions.assertEquals(new AccountDto(
                TEST_ACCOUNT_1_ID,
                "тестовый аккаунт",
                TEST_FOLDER_1_ID,
                false,
                TEST_ACCOUNT_SPACE_3_ID,
                false,
                YP_ID, null), account);

        List<ExpandedAccountResource> resources1 = expandedAccount.getResources();
        Assertions.assertNotNull(resources1);
        Assertions.assertEquals(2, resources1.size());

        List<ExpandedAccountResource> expectedExpandedResource = FrontQuotasApiTest.getExpectedExpandedResource();
        Assertions.assertEquals(expectedExpandedResource, resources1);
    }

    @Test
    public void updateProvisionsNoPermissionsTest() {
        FrontQuotasApiTest.prepareUpdateProvisionsOkResponseTest(stubProviderService);
        List<ProvisionLiteDto> updatedProvisions = List.of(
                new ProvisionLiteDto(YP_HDD_MAN, // resourceId
                        "100", // provided amount
                        GIGABYTES, // provided amount unit key
                        "200", // old provided amount
                        GIGABYTES),
                new ProvisionLiteDto(YP_SSD_MAN, // resourceId
                        "10", // provided amount
                        GIGABYTES, // provided amount unit key
                        "0", // old provided amount
                        GIGABYTES));
        ErrorCollectionDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .post()
                .uri("/front/quotas/_updateProvisions")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(getBody().setUpdatedProvisions(updatedProvisions).build())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
        Assertions.assertFalse(responseBody.getErrors().isEmpty());
    }

}
