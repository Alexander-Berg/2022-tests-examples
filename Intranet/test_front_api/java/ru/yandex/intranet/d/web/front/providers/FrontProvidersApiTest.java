package ru.yandex.intranet.d.web.front.providers;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResourceTypes;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountReserveType;
import ru.yandex.intranet.d.services.accounts.ReserveAccountsService;
import ru.yandex.intranet.d.services.resources.SelectionTreeResourceDto;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.MultilingualGrammaticalFormsDto;
import ru.yandex.intranet.d.web.model.ProviderDto;
import ru.yandex.intranet.d.web.model.ProviderResourceDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderWithQuotesDto;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedFolder;
import ru.yandex.intranet.d.web.model.folders.front.FolderPermission;
import ru.yandex.intranet.d.web.model.folders.front.ProviderPermission;
import ru.yandex.intranet.d.web.model.folders.front.ResourceTypeDto;
import ru.yandex.intranet.d.web.model.providers.ProviderUISettingsDto;
import ru.yandex.intranet.d.web.model.resources.SelectionResourceTreeNodeDto;

import static ru.yandex.intranet.d.TestProviders.DEFAULT_QUOTAS_PROVIDER_ID;

/**
 * Front providers API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontProvidersApiTest {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private YdbTableClient ydbTableClient;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private ReserveAccountsService reserveAccountsService;

    /**
     * Test get provider
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProvider
     */
    @Test
    public void getProviderTest() {
        ProviderDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}", "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getId());
        Assertions.assertTrue(result.isAccountsSpacesSupported());

        final ProviderDto.AccountsSettingsDto accountsSettingsDto = result.getAccountsSettingsDto();
        Assertions.assertTrue(accountsSettingsDto.isDisplayNameSupported());
        Assertions.assertTrue(accountsSettingsDto.isKeySupported());

        Assertions.assertTrue(result.getUiSettings().isPresent());
        MultilingualGrammaticalFormsDto titleForTheAccount = result.getUiSettings().get().getTitleForTheAccount();
        Assertions.assertNotNull(titleForTheAccount);
        Assertions.assertNotNull(titleForTheAccount.getNameSingularRu());
        Assertions.assertEquals("Пул", titleForTheAccount.getNameSingularRu().getNominative());
    }

    /**
     * Test provider not found
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProvider
     */
    @Test
    public void getProviderNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}", "12345678-9012-3456-7890-123456789012")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    /**
     * Test get all providers
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProviders
     */
    @Test
    public void getAllProviderTest() {
        List<ProviderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<ProviderDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(9, result.size());
        Assertions.assertTrue(result.stream().map(ProviderDto::getId)
                .anyMatch(id -> id.equals(TestProviders.YP_ID)));
        Assertions.assertTrue(result.stream()
                .anyMatch(provider -> provider.getId().equals(TestProviders.YDB_ID) &&
                        provider.getReserveFolderId().equals(TestFolders.TEST_FOLDER_2_RESERVE_ID)));
    }

    @Test
    public void providerResourcesCanBeFetched() {
        final List<ProviderResourceDto> providerResources = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}/resources",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<ProviderResourceDto>>() {
                })
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(providerResources);
        Assertions.assertFalse(providerResources.isEmpty());

        Assertions.assertTrue(providerResources.stream()
                .anyMatch(pr -> pr.getId().equals("f1038280-1eca-4df4-bcac-feee2deb8c79")));
    }

    /**
     * Test get provider reserved quotas
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProviderReservedQuotas
     */
    @Test
    public void getProviderReservedQuotasTest() {
        FrontFolderWithQuotesDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}/_reservedQuotas", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        List<ExpandedFolder> actualFolders = result.getFolders();
        Assertions.assertEquals(1, actualFolders.size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_RESERVE_ID, actualFolders.get(0).getFolder().getId());
        Assertions.assertEquals(Set.of(FolderPermission.CAN_MANAGE_ACCOUNT, FolderPermission.CAN_UPDATE_PROVISION),
                actualFolders.get(0).getPermissions());
        Assertions.assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                actualFolders.get(0).getProviders().get(0).getPermissions());

        List<ProviderDto> actualProviders = result.getProviders();
        Assertions.assertEquals(1, actualProviders.size());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", actualProviders.get(0).getId());

        List<ResourceTypeDto> actualResourceTypes = result.getResourceTypes();
        Assertions.assertEquals(1, actualResourceTypes.size());
        Assertions.assertEquals(TestResourceTypes.YP_HDD, actualResourceTypes.get(0).getId());
    }

    @Test
    public void getProviderReservedQuotasProviderAdminTest() {
        FrontFolderWithQuotesDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .get()
                .uri("/front/providers/{id}/_reservedQuotas", TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        List<ExpandedFolder> actualFolders = result.getFolders();
        Assertions.assertEquals(1, actualFolders.size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_1_RESERVE_ID, actualFolders.get(0).getFolder().getId());
        Assertions.assertEquals(Set.of(),
                actualFolders.get(0).getPermissions());
        Assertions.assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                actualFolders.get(0).getProviders().get(0).getPermissions());

        List<ProviderDto> actualProviders = result.getProviders();
        Assertions.assertEquals(1, actualProviders.size());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", actualProviders.get(0).getId());

        List<ResourceTypeDto> actualResourceTypes = result.getResourceTypes();
        Assertions.assertEquals(1, actualResourceTypes.size());
        Assertions.assertEquals(TestResourceTypes.YP_HDD, actualResourceTypes.get(0).getId());
    }

    /**
     * Test get provider reserved quotas with empty reserved folder
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProviderReservedQuotas
     */
    @Test
    public void getProviderReservedQuotasEmptyReservedFolderTest() {
        FrontFolderWithQuotesDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}/_reservedQuotas", TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        List<ExpandedFolder> actualFolders = result.getFolders();
        Assertions.assertEquals(1, actualFolders.size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_2_RESERVE_ID, actualFolders.get(0).getFolder().getId());

        Assertions.assertEquals(Collections.emptyList(), result.getProviders());

        Assertions.assertEquals(Collections.emptyList(), result.getResourceTypes());

        Assertions.assertEquals(Collections.emptyList(), result.getResources());
    }

    /**
     * Test get provider reserved quotas without reserved folder for the provider
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProviderReservedQuotas
     */
    @Test
    public void getProviderReservedQuotasWithoutReservedFolderTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{id}/_reservedQuotas", TestProviders.CLAUD1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(Set.of("Provider reserve folder not found."), result.getErrors());
    }

    /**
     * Test get all providers with default quotas
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontProvidersController#getProviders
     */
    @Test
    public void getAllProviderWithDefaultQuotasTest() {
        List<ProviderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers?withDefaultQuotas=true")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<List<ProviderDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertTrue(result.stream().map(ProviderDto::getId).anyMatch(DEFAULT_QUOTAS_PROVIDER_ID::equals));
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void getResourcesSelectionTreeTest() {
        SelectionResourceTreeNodeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("""
                        Default
                           SAS
                              [978bd75a-cf67-44ac-b944-e8ca949bdf7e]
                              [YP-CPU-SAS, YP-HDD-SAS, YP-TRAFFIC]
                           IVA
                              [53ec7196-bd8f-480d-80d8-c6195f0d6395]
                              [YP-HDD-IVA]
                           MYT
                              [e8d7fc08-52e6-4733-8303-ef0efc73905c]
                              [YP-HDD-MYT]
                           VLA
                              [ea5c58c9-72d1-4b84-baac-33067092e87e]
                              [YP-SSD-VLA, YP-HDD-VLA]
                           MAN
                              [ea5c58c9-72d1-4b84-baac-33067092e87e, 9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-RAM-MAN, YP-CPU-MAN, VIRTUAL-YP, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(result, "")
        );

        SelectionResourceTreeNodeDto result2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?withReadOnly={withReadOnly}",
                        TestProviders.YP_ID, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result2);
        Assertions.assertEquals("""
                        Default
                           SAS
                              [978bd75a-cf67-44ac-b944-e8ca949bdf7e]
                              [YP-CPU-SAS, YP-HDD-SAS, YP-TRAFFIC]
                           IVA
                              [53ec7196-bd8f-480d-80d8-c6195f0d6395]
                              [YP-HDD-IVA]
                           MYT
                              [e8d7fc08-52e6-4733-8303-ef0efc73905c]
                              [YP-HDD-MYT]
                           VLA
                              [ea5c58c9-72d1-4b84-baac-33067092e87e]
                              [YP-SSD-VLA, YP-HDD-VLA]
                           MAN
                              [ea5c58c9-72d1-4b84-baac-33067092e87e, 9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-RAM-MAN, READ-ONLY-YP, YP-CPU-MAN, VIRTUAL-YP, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(result2, "")
        );

        SelectionResourceTreeNodeDto result3 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?withUnmanaged={withUnmanaged}",
                        TestProviders.YP_ID, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result3);
        Assertions.assertEquals("""
                        Default
                           SAS
                              [978bd75a-cf67-44ac-b944-e8ca949bdf7e]
                              [YP-CPU-SAS, YP-HDD-SAS, YP-TRAFFIC]
                           IVA
                              [53ec7196-bd8f-480d-80d8-c6195f0d6395]
                              [YP-HDD-IVA]
                           MYT
                              [e8d7fc08-52e6-4733-8303-ef0efc73905c]
                              [YP-HDD-MYT]
                           VLA
                              [ea5c58c9-72d1-4b84-baac-33067092e87e]
                              [YP-SSD-VLA, YP-HDD-VLA]
                           MAN
                              [ea5c58c9-72d1-4b84-baac-33067092e87e, 9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-RAM-MAN, YP-CPU-MAN, VIRTUAL-YP, UNMANAGED-YP, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(result3, "")
        );

        SelectionResourceTreeNodeDto result4 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?withReadOnly={withReadOnly}" +
                                "&withUnmanaged={withUnmanaged}",
                        TestProviders.YP_ID, true, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result4);
        Assertions.assertEquals("""
                        Default
                           SAS
                              [978bd75a-cf67-44ac-b944-e8ca949bdf7e]
                              [YP-CPU-SAS, YP-HDD-SAS, YP-TRAFFIC]
                           IVA
                              [53ec7196-bd8f-480d-80d8-c6195f0d6395]
                              [YP-HDD-IVA]
                           MYT
                              [e8d7fc08-52e6-4733-8303-ef0efc73905c]
                              [YP-HDD-MYT]
                           VLA
                              [ea5c58c9-72d1-4b84-baac-33067092e87e]
                              [YP-SSD-VLA, YP-HDD-VLA]
                           MAN
                              [ea5c58c9-72d1-4b84-baac-33067092e87e, 9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-RAM-MAN, READ-ONLY-YP, UNMANAGED-AND-READ-ONLY-YP, YP-CPU-MAN, VIRTUAL-YP, \
                        UNMANAGED-YP, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(result4, "")
        );

        SelectionResourceTreeNodeDto result5 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result5);
        Assertions.assertEquals("""
                        Default
                           SAS
                              []
                              [YDB-RAM-SAS]
                        """,
                draw(result5, "")
        );

        Assertions.assertEquals("4c820f85-1e72-4f4a-bf1a-81d4ce1c9bdc", result5.getSegmentationId());
        //noinspection ConstantConditions,OptionalGetWithoutIsPresent
        Assertions.assertEquals("Тестовую сегментацию",
                result5.getSegmentationUISettings().get().getTitle().getNameSingularRu().getAccusative());
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void getResourcesSelectionTreeFiltersTest() {
        SelectionResourceTreeNodeDto resultForService = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?serviceIdForFilter=1",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForService);
        Assertions.assertEquals("""
                Default
                   MAN
                      [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                      [YP-RAM-MAN, YP-HDD-MAN]
                """,
                draw(resultForService, "")
        );

        SelectionResourceTreeNodeDto resultForFolder = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?folderIdForFilter=f714c483-c347-41cc-91d0-c6722f5daac7",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForFolder);
        Assertions.assertEquals("""
                Default
                   MAN
                      [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                      [YP-RAM-MAN, YP-HDD-MAN]
                """,
                draw(resultForFolder, "")
        );

        SelectionResourceTreeNodeDto resultForAccount = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?accountIdForFilter=56a41608-84df-41c4-9653-89106462e0ce",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForAccount);
        Assertions.assertEquals("""
                Default
                   MAN
                      [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                      [YP-RAM-MAN, YP-HDD-MAN]
                """,
                draw(resultForAccount, "")
        );
    }

    @Test
    public void getResourcesSelectionTreeTypesAndOrdersTest() {
       SelectionResourceTreeNodeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?withReadOnly={withReadOnly}" +
                                "&withUnmanaged={withUnmanaged}",
                        TestProviders.YP_ID, true, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);

        Set<SelectionTreeResourceDto> resources = new HashSet<>();
        collectResources(resources, result);

        var cpu = resources.stream()
                .filter(r -> r.getResourceTypeId().equals(TestResourceTypes.YP_CPU))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("CPU", cpu.getResourceTypeNameEn());
        Assertions.assertEquals("CPU", cpu.getResourceTypeNameRu());
        Assertions.assertEquals(1, cpu.getSortingOrder());

        var ram = resources.stream()
                .filter(r -> r.getResourceTypeId().equals(TestResourceTypes.YP_RAM))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("RAM", ram.getResourceTypeNameEn());
        Assertions.assertEquals("RAM", ram.getResourceTypeNameRu());
        Assertions.assertEquals(2, ram.getSortingOrder());

        var virtual = resources.stream()
                .filter(r -> r.getResourceTypeId().equals(TestResourceTypes.YP_VIRTUAL))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals("Virtual YP resource", virtual.getResourceTypeNameEn());
        Assertions.assertEquals("Виртуальный ресурс в YP", virtual.getResourceTypeNameRu());
        Assertions.assertNull(virtual.getSortingOrder());
    }

    @Test
    public void getResourcesSelectionTreeProviderReserveOnly() {
        AccountModel account = ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.getById(
                        session.asTxCommitRetryable(TransactionMode.ONLINE_READ_ONLY),
                        TestAccounts.TEST_ACCOUNT_2_ID, Tenants.DEFAULT_TENANT_ID)).block().get();
        AccountModel reserveAccount = new AccountModel.Builder(account)
                .setReserveType(AccountReserveType.PROVIDER)
                .build();
        ydbTableClient.usingSessionMonoRetryable(session ->
                accountsDao.upsertOneRetryable(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        reserveAccount)).block();
        ydbTableClient.usingSessionMonoRetryable(session ->
                reserveAccountsService.addReserveAccountMono(
                        session.asTxCommitRetryable(TransactionMode.SERIALIZABLE_READ_WRITE),
                        reserveAccount)).block();

        SelectionResourceTreeNodeDto resultForService = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?serviceIdForFilter=3&providerReserveOnly=true",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForService);
        Assertions.assertEquals("""
                Default
                   SAS
                      []
                      [YDB-RAM-SAS]
                """,
                draw(resultForService, "")
        );

        SelectionResourceTreeNodeDto resultForFolder = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?folderIdForFilter=7d4745b7-6d80-e6d6-84f4-2510bbca38e8&providerReserveOnly=true",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForFolder);
        Assertions.assertEquals("""
                Default
                   SAS
                      []
                      [YDB-RAM-SAS]
                """,
                draw(resultForFolder, "")
        );

        SelectionResourceTreeNodeDto resultForAccount = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?accountIdForFilter=56a41608-84df-41c4-9653-89106462e0ce&providerReserveOnly=true",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForAccount);
        Assertions.assertEquals("""
                Default
                   MAN
                      [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                      [YP-RAM-MAN, YP-HDD-MAN]
                """,
                draw(resultForAccount, "")
        );
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void getProviderDefaultUISettingsTest() {
        ProviderUISettingsDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/_defaultUISettings")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ProviderUISettingsDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("аккаунта", result.getTitleForTheAccount().getNameSingularRu().getGenitive());
        Assertions.assertEquals("в аккаунте", result.getTitleForTheAccount().getNameSingularRu().getLocative());
    }

    @Test
    @SuppressWarnings("checkstyle:MethodLength")
    public void getResourcesSelectionTreeFiltersBalanceTest() {
        SelectionResourceTreeNodeDto resultForService = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree?serviceIdForFilter=1&balanceOnly={flag}",
                        TestProviders.YP_ID, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForService);
        Assertions.assertEquals("""
                        Default
                           SAS
                              [978bd75a-cf67-44ac-b944-e8ca949bdf7e]
                              [YP-HDD-SAS, YP-TRAFFIC]
                           MAN
                              [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-CPU-MAN, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(resultForService, "")
        );

        SelectionResourceTreeNodeDto resultForFolder = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/_resourceSelectionTree" +
                                "?folderIdForFilter=f714c483-c347-41cc-91d0-c6722f5daac7&balanceOnly={flag}",
                        TestProviders.YP_ID, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionResourceTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultForFolder);
        Assertions.assertEquals("""
                        Default
                           MAN
                              [9c44cf69-76c5-45a3-9335-57e2669f03ff]
                              [YP-CPU-MAN, YP-HDD-MAN, YP-SSD-MAN]
                        """,
                draw(resultForFolder, "")
        );
    }

    private static void collectResources(
            Set<SelectionTreeResourceDto> set,
            SelectionResourceTreeNodeDto node
    ) {
        if (node.getResources() != null) {
            set.addAll(node.getResources());
        }
        if (node.getChildrenBySegmentId() != null) {
            node.getChildrenBySegmentId().values().forEach(n -> collectResources(set, n));
        }
    }

    private static String draw(SelectionResourceTreeNodeDto root, String indent) {
        if (root.getResources() != null) {
            return indent + root.getAccountsSpacesIds() + "\n" + indent +
                    root.getResources()
                            .stream()
                            .map(SelectionTreeResourceDto::getDisplayName)
                            .toList()
                    + "\n";
        }
        if (root.getChildrenBySegmentId() == null) {
            return "";
        }
        StringBuilder res = new StringBuilder();
        root.getChildrenBySegmentId().forEach((segmentId, node) ->
                res.append(indent).append(segmentId).append("\n")
                        .append(draw(node, indent + "   "))
        );
        return res.toString();
    }
}
