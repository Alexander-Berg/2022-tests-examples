package ru.yandex.intranet.d.web.front.folders;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.UnitIds;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.accounts.AccountsDao;
import ru.yandex.intranet.d.dao.accounts.AccountsQuotasDao;
import ru.yandex.intranet.d.dao.folders.FolderDao;
import ru.yandex.intranet.d.dao.quotas.QuotasDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.accounts.AccountModel;
import ru.yandex.intranet.d.model.accounts.AccountsQuotasModel;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.model.quotas.QuotaModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.ProviderDto;
import ru.yandex.intranet.d.web.model.ResourceDto;
import ru.yandex.intranet.d.web.model.folders.FrontAmountsDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderWithQuotesDto;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccount;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedAccountResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedFolder;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedProvider;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResource;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceBuilder;
import ru.yandex.intranet.d.web.model.folders.front.ExpandedResourceType;
import ru.yandex.intranet.d.web.model.folders.front.FolderPermission;
import ru.yandex.intranet.d.web.model.folders.front.ProviderPermission;
import ru.yandex.intranet.d.web.model.folders.front.ResourceTypeDto;
import ru.yandex.intranet.d.web.model.resources.ResourceSegmentationSegmentDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_3_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_5_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_WITH_NON_VIRTUAL_RESOURCES_ID;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID;
import static ru.yandex.intranet.d.TestProviders.YDB_ID;
import static ru.yandex.intranet.d.TestProviders.YP_ID;
import static ru.yandex.intranet.d.TestProviders.YT_ID;
import static ru.yandex.intranet.d.TestResourceTypes.YP_HDD;
import static ru.yandex.intranet.d.TestResourceTypes.YP_SSD;
import static ru.yandex.intranet.d.TestResources.YDB_RAM_SAS;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_HDD_SAS;
import static ru.yandex.intranet.d.TestResources.YP_RAM_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_MAN;
import static ru.yandex.intranet.d.TestResources.YP_SSD_VLA;
import static ru.yandex.intranet.d.TestResources.YP_VIRTUAL;
import static ru.yandex.intranet.d.TestSegmentations.YP_SEGMENT;
import static ru.yandex.intranet.d.TestServices.TEST_SERVICE_ID_DISPENSER;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;

/**
 * Tests for main page endpoint GET /front/folders
 *
 * @author Nikita Minin <spasitel@yandex-team.ru>
 */
@SuppressWarnings({"OptionalGetWithoutIsPresent", "ConstantConditions"})
@IntegrationTest
public class FrontFoldersMainPageTest {
    @Autowired
    private WebTestClient webClient;
    @Autowired
    private YdbTableClient tableClient;
    @Autowired
    private FolderDao folderDao;
    @Autowired
    private AccountsDao accountsDao;
    @Autowired
    private QuotasDao quotasDao;
    @Autowired
    private AccountsQuotasDao accountsQuotasDao;

    @Test
    public void getByEmptyServiceTest() {
        EntityExchangeResult<FrontFolderWithQuotesDto> response = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult();

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.getResponseBody());
        Assertions.assertTrue(response.getResponseBody().getFolders().stream()
                .map(ExpandedFolder::getFolder)
                .map(FrontFolderDto::getFolderType)
                .anyMatch(folderType -> folderType == FolderType.COMMON_DEFAULT_FOR_SERVICE)
        );
    }

    /**
     * Get by service test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontFoldersController#getFolders
     */
    @Test
    public void getByServiceTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(3, responseBody.getFolders().size());
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_2_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_RESERVE_ID)));

        Assertions.assertEquals(10, responseBody.getResources().size());
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(YP_HDD_MAN)));
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)));

        Assertions.assertEquals(2, responseBody.getProviders().size());
        Assertions.assertEquals(Set.of(YP_ID, YDB_ID), responseBody.getProviders()
                .stream().map(ProviderDto::getId).collect(Collectors.toSet()));

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TestFolders.TEST_FOLDER_1_ID
        )).findFirst().get();
        assertEquals(Set.of(FolderPermission.CAN_MANAGE_ACCOUNT, FolderPermission.CAN_UPDATE_PROVISION),
                folder.getPermissions());
        ExpandedProvider yp = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                YP_ID
        )).findFirst().get();
        assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                yp.getPermissions());
        ExpandedResourceType ypHdd =
                yp.getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                        YP_HDD
                )).findFirst().get();

        Assertions.assertEquals("100", ypHdd.getSums().getAllocated().getForEditAmount());

        ExpandedResource ypHddMan = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();

        Assertions.assertEquals("1", ypHddMan.getQuota().getReadableAmount());
        Assertions.assertEquals("TB", ypHddMan.getQuota().getReadableUnit());
        Assertions.assertEquals("800", ypHddMan.getBalance().getForEditAmount());
        Assertions.assertEquals("100000000000", ypHddMan.getAllocated().getRawAmount());

        ExpandedResource ypHddMyt = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MYT
        )).findFirst().get();
        Assertions.assertEquals("0", ypHddMyt.getQuota().getReadableAmount());
        Assertions.assertEquals("GB", ypHddMyt.getQuota().getReadableUnit());
        Assertions.assertEquals("0", ypHddMyt.getQuota().getForEditAmount());
        Assertions.assertEquals(GIGABYTES, ypHddMyt.getQuota().getForEditUnitId());
        Assertions.assertEquals("-80", ypHddMyt.getBalance().getReadableAmount());
        Assertions.assertEquals("GB", ypHddMyt.getBalance().getReadableUnit());
        Assertions.assertEquals("-80", ypHddMyt.getBalance().getForEditAmount());
        Assertions.assertEquals(GIGABYTES, ypHddMyt.getBalance().getForEditUnitId());

        ExpandedAccount account = yp.getAccounts().stream().filter(a -> a.getAccount().getId().equals(
                TestAccounts.TEST_ACCOUNT_1_ID
        )).findFirst().get();
        ExpandedAccountResource accountYpHddMan = account.getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();

        Assertions.assertEquals("100", accountYpHddMan.getAllocated().getForEditAmount());

        ExpandedResource ypHddManFolder2 = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TestFolders.TEST_FOLDER_2_ID
        )).findFirst().get().getProviders().stream().filter(p -> p.getProviderId().equals(
                YP_ID
        )).findFirst().get().getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                YP_HDD
        )).findFirst().get().getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();
        Assertions.assertNotNull(ypHddManFolder2.getAllocated());
        Assertions.assertEquals("0", ypHddManFolder2.getAllocated().getRawAmount());

        ExpandedProvider ydb = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                YDB_ID
        )).findFirst().get();

        Assertions.assertTrue(ydb.getResourceTypes().isEmpty());
        Assertions.assertEquals(1, ydb.getAccounts().size());

        ResourceSegmentationSegmentDto ypHddManSegment =
                responseBody.getResources().stream().filter(resource -> resource.getId().equals(YP_HDD_MAN))
                .findFirst().get()
                .getResourceSegments()
                .stream().filter(segment -> segment.getSegmentationId().equals(YP_SEGMENT))
                .findFirst().get();
        Assertions.assertEquals("Segment", ypHddManSegment.getSegmentationName());
        Assertions.assertEquals(0, ypHddManSegment.getGroupingOrder());
        Assertions.assertEquals("Default", ypHddManSegment.getSegmentName());

        Assertions.assertEquals("Пул", responseBody.getProviders().stream()
                .filter(it -> it.getId().equals(YP_ID))
                .findFirst().get().getUiSettings().get().getTitleForTheAccount().getNameSingularRu().getNominative()
        );
        Assertions.assertEquals("Пул", responseBody.getAccountsSpaces().stream()
                .filter(it -> it.getId().equals(TEST_ACCOUNT_SPACE_5_ID))
                .findFirst().get().getUiSettings().get().getTitleForTheAccount().getNameSingularRu().getNominative()
        );
    }

    @Test
    public void getByServiceProviderAdminTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .get()
                .uri("/front/folders?serviceId=1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(3, responseBody.getFolders().size());
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_2_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_RESERVE_ID)));

        Assertions.assertEquals(10, responseBody.getResources().size());
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(YP_HDD_MAN)));
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)));

        Assertions.assertEquals(2, responseBody.getProviders().size());
        Assertions.assertEquals(Set.of(YP_ID, YDB_ID), responseBody.getProviders()
                .stream().map(ProviderDto::getId).collect(Collectors.toSet()));

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TestFolders.TEST_FOLDER_1_ID
        )).findFirst().get();
        assertEquals(Set.of(), folder.getPermissions());
        ExpandedProvider yp = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                YP_ID
        )).findFirst().get();
        assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                yp.getPermissions());
        ExpandedResourceType ypHdd =
                yp.getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                        YP_HDD
                )).findFirst().get();

        Assertions.assertEquals("100", ypHdd.getSums().getAllocated().getForEditAmount());

        ExpandedResource ypHddMan = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();

        Assertions.assertEquals("1", ypHddMan.getQuota().getReadableAmount());
        Assertions.assertEquals("TB", ypHddMan.getQuota().getReadableUnit());
        Assertions.assertEquals("800", ypHddMan.getBalance().getForEditAmount());
        Assertions.assertEquals("100000000000", ypHddMan.getAllocated().getRawAmount());

        ExpandedResource ypHddMyt = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MYT
        )).findFirst().get();
        Assertions.assertEquals("0", ypHddMyt.getQuota().getReadableAmount());
        Assertions.assertEquals("GB", ypHddMyt.getQuota().getReadableUnit());
        Assertions.assertEquals("0", ypHddMyt.getQuota().getForEditAmount());
        Assertions.assertEquals(GIGABYTES, ypHddMyt.getQuota().getForEditUnitId());
        Assertions.assertEquals("-80", ypHddMyt.getBalance().getReadableAmount());
        Assertions.assertEquals("GB", ypHddMyt.getBalance().getReadableUnit());
        Assertions.assertEquals("-80", ypHddMyt.getBalance().getForEditAmount());
        Assertions.assertEquals(GIGABYTES, ypHddMyt.getBalance().getForEditUnitId());

        ExpandedAccount account = yp.getAccounts().stream().filter(a -> a.getAccount().getId().equals(
                TestAccounts.TEST_ACCOUNT_1_ID
        )).findFirst().get();
        ExpandedAccountResource accountYpHddMan = account.getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();

        Assertions.assertEquals("100", accountYpHddMan.getAllocated().getForEditAmount());

        ExpandedResource ypHddManFolder2 = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TestFolders.TEST_FOLDER_2_ID
        )).findFirst().get().getProviders().stream().filter(p -> p.getProviderId().equals(
                YP_ID
        )).findFirst().get().getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                YP_HDD
        )).findFirst().get().getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();
        Assertions.assertNotNull(ypHddManFolder2.getAllocated());
        Assertions.assertEquals("0", ypHddManFolder2.getAllocated().getRawAmount());

        ExpandedProvider ydb = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                YDB_ID
        )).findFirst().get();

        Assertions.assertTrue(ydb.getResourceTypes().isEmpty());
        Assertions.assertEquals(1, ydb.getAccounts().size());

        ResourceSegmentationSegmentDto ypHddManSegment =
                responseBody.getResources().stream().filter(resource -> resource.getId().equals(YP_HDD_MAN))
                        .findFirst().get()
                        .getResourceSegments()
                        .stream().filter(segment -> segment.getSegmentationId().equals(YP_SEGMENT))
                        .findFirst().get();
        Assertions.assertEquals("Segment", ypHddManSegment.getSegmentationName());
        Assertions.assertEquals(0, ypHddManSegment.getGroupingOrder());
        Assertions.assertEquals("Default", ypHddManSegment.getSegmentName());
    }

    /**
     * Get by service completely zero quotas test.
     *
     * @see ru.yandex.intranet.d.web.controllers.front.FrontFoldersController#getFolders
     */
    @Test
    public void getByServiceCompletelyZeroQuotasTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=19")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody.getFolders().size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_7_ID, responseBody.getFolders().get(0).getFolder().getId());

        Assertions.assertEquals(1, responseBody.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody.getProviders().get(0).getId());

        Assertions.assertEquals(1, responseBody.getResources().size());
        Assertions.assertEquals(YP_HDD_MAN, responseBody.getResources().get(0).getId());

        List<ExpandedAccount> accounts = responseBody.getFolders().get(0)
                .getProviders().get(0)
                .getAccounts();
        assertEquals(1, accounts.size());

        List<ExpandedAccountResource> resources = accounts.get(0).getResources();
        assertEquals(1, resources.size());

        ExpandedAccountResource ypHddMan = resources.get(0);
        assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", ypHddMan.getResourceId());
    }

    @Test
    public void getByServiceAndProviderTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&providerId=" + YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(3, responseBody.getFolders().size());
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_2_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_RESERVE_ID)));

        Assertions.assertEquals(10, responseBody.getResources().size());
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(YP_HDD_MAN)));
        Assertions.assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)));

        Assertions.assertEquals(1, responseBody.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody.getProviders().get(0).getId());

        final ExpandedResource resQuota = responseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID))
                .findFirst().get().getProviders().get(0)
                .getResourceTypes().stream().filter(rt -> rt.getResourceTypeId().equals(YP_SSD)).findFirst().get()
                .getResources().get(0);
        Assertions.assertEquals("2", resQuota.getQuota().getForEditAmount());
        Assertions.assertEquals("2", resQuota.getBalance().getForEditAmount());


        FrontFolderWithQuotesDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&providerId=" + YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody2.getFolders().size());

        Assertions.assertEquals(1, responseBody2.getProviders().size());
        Assertions.assertEquals(YDB_ID, responseBody2.getProviders().get(0).getId());

        Assertions.assertEquals(0, responseBody2.getResources().size());

        ExpandedFolder folder = responseBody2.getFolders().iterator().next();
        Assertions.assertEquals(1, folder.getProviders().size());

        ExpandedProvider ydb = folder.getProviders().iterator().next();

        Assertions.assertTrue(ydb.getResourceTypes().isEmpty());
        Assertions.assertEquals(1, ydb.getAccounts().size());
    }

    @Test
    public void getByServiceAndProviderAndResourceTest() {

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&providerId=" + YP_ID
                        + "&resourceId=" + YP_HDD_MAN)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(2, responseBody.getFolders().size());
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_2_ID)));

        Assertions.assertEquals(1, responseBody.getResources().size());
        Assertions.assertEquals(YP_HDD_MAN, responseBody.getResources().get(0).getId());

        Assertions.assertEquals(1, responseBody.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody.getProviders().get(0).getId());

        final ExpandedResource resQuota = responseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID))
                .findFirst().get().getProviders().get(0)
                .getResourceTypes().get(0).getResources().get(0);
        Assertions.assertEquals("1", resQuota.getQuota().getForEditAmount());
        Assertions.assertEquals("800", resQuota.getBalance().getForEditAmount());


        FrontFolderWithQuotesDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&providerId=" + YP_ID
                        + "&resourceId=" + TestResources.YP_SSD_VLA)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody2.getFolders().size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_2_ID, responseBody2.getFolders().get(0).getFolder().getId());

        Assertions.assertEquals(1, responseBody2.getResources().size());
        Assertions.assertEquals(TestResources.YP_SSD_VLA, responseBody2.getResources().get(0).getId());

        Assertions.assertEquals(1, responseBody2.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody2.getProviders().get(0).getId());

        final ExpandedResource resQuota2 = responseBody2.getFolders().get(0).getProviders()
                .get(0).getResourceTypes().get(0).getResources().get(0);
        Assertions.assertEquals(0, BigDecimal.valueOf(1002)
                .compareTo(new BigDecimal(resQuota2.getQuota().getRawAmount())));
        Assertions.assertEquals(0, BigDecimal.valueOf(-202)
                .compareTo(new BigDecimal(resQuota2.getNegativeBalance().getRawAmount())));
    }

    @Test
    public void getByServiceAndResourceTest() {

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId={serviceId}&resourceId={resourceId}",
                        TEST_SERVICE_ID_DISPENSER, YP_HDD_MAN)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(2, responseBody.getFolders().size());
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID)));
        Assertions.assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TestFolders.TEST_FOLDER_2_ID)));

        Assertions.assertEquals(1, responseBody.getResources().size());
        Assertions.assertEquals(YP_HDD_MAN, responseBody.getResources().get(0).getId());

        Assertions.assertEquals(1, responseBody.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody.getProviders().get(0).getId());

        final ExpandedResource resQuota = responseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID))
                .findFirst().get().getProviders().get(0)
                .getResourceTypes().get(0).getResources().get(0);
        Assertions.assertEquals("1", resQuota.getQuota().getForEditAmount());
        Assertions.assertEquals("800", resQuota.getBalance().getForEditAmount());


        FrontFolderWithQuotesDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId={serviceId}&resourceId={resourceId}",
                        TEST_SERVICE_ID_DISPENSER, YP_SSD_VLA)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody2.getFolders().size());
        Assertions.assertEquals(TestFolders.TEST_FOLDER_2_ID, responseBody2.getFolders().get(0).getFolder().getId());

        Assertions.assertEquals(1, responseBody2.getResources().size());
        Assertions.assertEquals(TestResources.YP_SSD_VLA, responseBody2.getResources().get(0).getId());

        Assertions.assertEquals(1, responseBody2.getProviders().size());
        Assertions.assertEquals(YP_ID, responseBody2.getProviders().get(0).getId());

        final ExpandedResource resQuota2 = responseBody2.getFolders().get(0).getProviders()
                .get(0).getResourceTypes().get(0).getResources().get(0);
        Assertions.assertEquals(0, BigDecimal.valueOf(1002)
                .compareTo(new BigDecimal(resQuota2.getQuota().getRawAmount())));
        Assertions.assertEquals(0, BigDecimal.valueOf(-202)
                .compareTo(new BigDecimal(resQuota2.getNegativeBalance().getRawAmount())));
    }

    @Test
    public void getByServiceAndAbsentProviderTest() {

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId={serviceId}&providerId={providerId}",
                        TEST_SERVICE_ID_DISPENSER, YT_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertTrue(responseBody.getFolders().isEmpty());
        Assertions.assertTrue(responseBody.getResources().isEmpty());
        Assertions.assertTrue(responseBody.getAccountsSpaces().isEmpty());
        Assertions.assertTrue(responseBody.getResourceTypes().isEmpty());
    }

    @Test
    public void getByServiceAndAbsentResourceTest() {

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId={serviceId}&resourceId={resourceId}",
                        TEST_SERVICE_ID_DISPENSER, YP_RAM_MAN)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(responseBody);
        Assertions.assertTrue(responseBody.getFolders().isEmpty());
        Assertions.assertTrue(responseBody.getResources().isEmpty());
        Assertions.assertTrue(responseBody.getAccountsSpaces().isEmpty());
        Assertions.assertTrue(responseBody.getResourceTypes().isEmpty());
    }

    @Test
    public void getByServiceAndProviderPageTest() {
        List<ExpandedFolder> folders = new ArrayList<>();

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&limit=1&providerId=" + YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody.getFolders().size());
        Assertions.assertNotNull(responseBody.getContinuationToken());
        folders.add(responseBody.getFolders().get(0));

        FrontFolderWithQuotesDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&limit=2&continuationToken=" +
                        responseBody.getContinuationToken() + "&providerId=" + YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(2, responseBody2.getFolders().size());
        Assertions.assertNull(responseBody2.getContinuationToken());
        folders.add(responseBody2.getFolders().get(0));
        folders.add(responseBody2.getFolders().get(1));

        ExpandedResource resQuota = folders.stream()
                .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID))
                .findFirst().get().getProviders().get(0)
                .getResourceTypes().stream().filter(rt -> rt.getResourceTypeId().equals(YP_SSD)).findFirst().get()
                .getResources().get(0);
        Assertions.assertEquals("2", resQuota.getQuota().getForEditAmount());
        Assertions.assertEquals("2", resQuota.getBalance().getForEditAmount());
    }

    @Test
    public void getByServicePageTest() {
        List<ExpandedFolder> folders = new ArrayList<>();

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&limit=1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(1, responseBody.getFolders().size());
        folders.add(responseBody.getFolders().get(0));

        FrontFolderWithQuotesDto responseBody2 = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&limit=2&continuationToken=" +
                        responseBody.getContinuationToken())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertEquals(2, responseBody2.getFolders().size());
        folders.add(responseBody2.getFolders().get(0));
        folders.add(responseBody2.getFolders().get(1));
        Assertions.assertNull(responseBody2.getContinuationToken());

        ExpandedResource resQuota = folders.stream()
                .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_1_ID))
                .findFirst().get().getProviders().stream().filter(p -> p.getProviderId().equals(YP_ID))
                .findFirst().get().getResourceTypes().stream().filter(t ->
                        t.getResourceTypeId().equals(YP_HDD))
                .findFirst().get().getResources().stream().filter(r ->
                        r.getResourceId().equals(YP_HDD_MAN))
                .findFirst().get();
        Assertions.assertEquals("1", resQuota.getQuota().getForEditAmount());
        Assertions.assertEquals("800", resQuota.getBalance().getForEditAmount());

        ExpandedResource ypHddManFolder2 = folders.stream().filter(f -> f.getFolder().getId().equals(
                TestFolders.TEST_FOLDER_2_ID
        )).findFirst().get().getProviders().stream().filter(p -> p.getProviderId().equals(
                YP_ID
        )).findFirst().get().getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                YP_HDD
        )).findFirst().get().getResources().stream().filter(r -> r.getResourceId().equals(
                YP_HDD_MAN
        )).findFirst().get();
        Assertions.assertNotNull(ypHddManFolder2.getAllocated());
        Assertions.assertEquals("0", ypHddManFolder2.getAllocated().getRawAmount());
        Assertions.assertEquals("100000000000001", ypHddManFolder2.getBalance().getRawAmount());
    }

    @Test
    public void resourcesSumByTypeShouldBeCalculated() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=6")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        ExpandedFolder folder =
                responseBody.getFolders().stream()
                        .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_3_ID))
                        .findFirst().get();
        Map<String, ResourceTypeDto> resourceTypeById = responseBody.getResourceTypes().stream()
                .collect(Collectors.toMap(ResourceTypeDto::getId, Function.identity()));

        Map<String, ExpandedResourceType> balanceByResourceType = folder.getProviders().stream()
                .flatMap(p -> p.getResourceTypes().stream())
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ResourceTypeDto ydbRam = resourceTypeById.get("8908d0f9-e05d-47b6-bbf9-6f1cdb34b17c");
        FrontAmountsDto ramBalance = balanceByResourceType.get(ydbRam.getId()).getSums();

        Assertions.assertEquals("RAM", ydbRam.getName());
        Assertions.assertEquals("Storage units, binary", ydbRam.getEnsemble());
        Assertions.assertEquals("bytes", ydbRam.getBaseUnit());
        Assertions.assertEquals(5, ydbRam.getSortingOrder());
        Assertions.assertNotNull(ramBalance);
        Assertions.assertEquals(300L, Long.parseLong(ramBalance.getBalance().getRawAmount()));
        Assertions.assertNull(ramBalance.getNegativeBalance());
        Assertions.assertEquals(1394L, Long.parseLong(ramBalance.getQuota().getRawAmount()));

        ResourceTypeDto ypHDD = resourceTypeById.get("44f93060-e367-44e6-b069-98c20d03dd81");
        FrontAmountsDto hddBytesBalance = balanceByResourceType.get(ypHDD.getId()).getSums();
        Assertions.assertNotNull(hddBytesBalance);
        Assertions.assertNotNull(hddBytesBalance.getPositiveBalance());
        Assertions.assertEquals("HDD", ypHDD.getName());
        Assertions.assertEquals("Storage units, decimal", ypHDD.getEnsemble());
        Assertions.assertEquals("bytes", ypHDD.getBaseUnit());
        Assertions.assertEquals(3L, ypHDD.getSortingOrder());
        Assertions.assertEquals(121L, Long.parseLong(hddBytesBalance.getPositiveBalance().getRawAmount()));
        Assertions.assertEquals(-282L, Long.parseLong(hddBytesBalance.getNegativeBalance().getRawAmount()));
        Assertions.assertEquals(3435L, Long.parseLong(hddBytesBalance.getQuota().getRawAmount()));
    }

    @Test
    public void rawAndHumanReadableResourcesShouldBeCalculated() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=12")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        ExpandedFolder folder =
                responseBody.getFolders().stream()
                        .filter(f -> f.getFolder().getId().equals(TestFolders.TEST_FOLDER_4_ID))
                        .findFirst().get();
        Map<String, ResourceTypeDto> resourceTypeById = responseBody.getResourceTypes().stream()
                .collect(Collectors.toMap(ResourceTypeDto::getId, Function.identity()));

        Map<String, ExpandedResourceType> balanceByResourceType = folder.getProviders().stream()
                .flatMap(p -> p.getResourceTypes().stream())
                .collect(Collectors.toMap(ExpandedResourceType::getResourceTypeId, Function.identity()));

        ResourceTypeDto ydbHdd = resourceTypeById.get("44f93060-e367-44e6-b069-98c20d03dd81");
        ExpandedResourceType expandedResourceType = balanceByResourceType.get(ydbHdd.getId());
        FrontAmountsDto hddBalance = expandedResourceType.getSums();

        Assertions.assertEquals("HDD", ydbHdd.getName());
        Assertions.assertEquals("Storage units, decimal", ydbHdd.getEnsemble());
        Assertions.assertEquals("bytes", ydbHdd.getBaseUnit());
        Assertions.assertEquals(3L, ydbHdd.getSortingOrder());
        Assertions.assertNotNull(hddBalance);
        Assertions.assertEquals(100000001999992L, Long.parseLong(hddBalance.getBalance().getRawAmount()));
        Assertions.assertEquals("TB", hddBalance.getBalance().getRawUnit());
        Assertions.assertEquals("100000001999.99", hddBalance.getBalance().getReadableAmount());
        Assertions.assertEquals("PB", hddBalance.getBalance().getReadableUnit());

        Map<String, AmountDto> resourceAmountDtoMap = expandedResourceType.getResources().stream()
                .collect(Collectors.toMap(ExpandedResource::getResourceId, FrontAmountsDto::getQuota));

        compareAmountDto(resourceAmountDtoMap.get("ef333da9-b076-42f5-b7f5-84cd04ab7fcc"), 999990L, "B",
                0, "GB");
        compareAmountDto(resourceAmountDtoMap.get("8709a63b-a307-4533-8a89-09012b05e096"), 1000002L, "TB",
                1000, "PB");
        compareAmountDto(resourceAmountDtoMap.get("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2"), 100000000000000L, "B",
                100, "TB");
    }

    private void compareAmountDto(AmountDto amountDto, long rawValue, String rawUnit, double readableAmount,
                                  String readableUnit) {
        Assertions.assertEquals(rawValue, Long.parseLong(amountDto.getRawAmount()));
        Assertions.assertEquals(rawUnit, amountDto.getRawUnit());
        Assertions.assertEquals(readableAmount, Double.parseDouble(amountDto.getReadableAmount()));
        Assertions.assertEquals(readableUnit, amountDto.getReadableUnit());
    }

    @Test
    public void getByEmptyServiceAcceptableForDAdminsTest() {
        EntityExchangeResult<FrontFolderWithQuotesDto> response = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/front/folders?serviceId=2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult();

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.getResponseBody());
        Assertions.assertTrue(response.getResponseBody().getFolders().stream()
                .map(ExpandedFolder::getFolder)
                .map(FrontFolderDto::getFolderType)
                .anyMatch(folderType -> folderType == FolderType.COMMON_DEFAULT_FOR_SERVICE)
        );
    }

    @Test
    public void serviceNotFoundTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=222222222")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(r -> {
                    ErrorCollectionDto errorCollection = r.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Set<String> errors = errorCollection.getErrors();
                    Assertions.assertTrue(errors.contains("Service not found."));
                });
    }

    @Test
    public void invalidContinuationTokenTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1&limit=1&continuationToken=invalid")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.BAD_REQUEST)
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(r -> {
                    ErrorCollectionDto errorCollection = r.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Set<String> errors = errorCollection.getErrors();
                    Assertions.assertTrue(errors.contains("Invalid continuation token."));
                });
    }

    @SuppressWarnings("SameParameterValue")
    private QuotaModel quotaModel(String providerId, String resourceId, String folderId, long quota, long balance,
                                  long frozen) {
        return QuotaModel.builder()
                .tenantId(Tenants.DEFAULT_TENANT_ID)
                .providerId(providerId)
                .resourceId(resourceId)
                .folderId(folderId)
                .quota(quota)
                .balance(balance)
                .frozenQuota(frozen)
                .build();
    }

    @SuppressWarnings({"ParameterNumber", "SameParameterValue"})
    private AccountsQuotasModel accountQuotaModel(String providerId, String resourceId, String folderId,
                                                  String accountId, long provided, long allocated,
                                                  Long lastReceivedVersion, String lastOpId) {
        return new AccountsQuotasModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setProviderId(providerId)
                .setResourceId(resourceId)
                .setFolderId(folderId)
                .setAccountId(accountId)
                .setProvidedQuota(provided)
                .setAllocatedQuota(allocated)
                .setLastProvisionUpdate(Instant.now())
                .setLastReceivedProvisionVersion(lastReceivedVersion)
                .setLatestSuccessfulProvisionOperationId(lastOpId)
                .build();
    }

    @SuppressWarnings({"ParameterNumber", "SameParameterValue"})
    private AccountModel accountModel(String providerId, String accountsSpaceId, String externalId, String externalKey,
                                      String folderId, String displayName, Long lastReceivedVersion,
                                      String lastOpId, boolean deleted) {
        return new AccountModel.Builder()
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setId(UUID.randomUUID().toString())
                .setVersion(0L)
                .setProviderId(providerId)
                .setAccountsSpacesId(accountsSpaceId)
                .setOuterAccountIdInProvider(externalId)
                .setOuterAccountKeyInProvider(externalKey)
                .setFolderId(folderId)
                .setDisplayName(displayName)
                .setDeleted(false)
                .setLastAccountUpdate(Instant.now())
                .setLastReceivedVersion(lastReceivedVersion)
                .setLatestSuccessfulAccountOperationId(lastOpId)
                .setDeleted(deleted)
                .build();
    }

    @Test
    @SuppressWarnings("MethodLength")
    public void frozenQuotaShouldBeCalculated() {
        FolderModel folder = FolderModel.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTenantId(Tenants.DEFAULT_TENANT_ID)
                .setServiceId(TEST_SERVICE_ID_DISPENSER)
                .setVersion(0L)
                .setDisplayName("Test")
                .setDescription("Test")
                .setDeleted(false)
                .setFolderType(FolderType.COMMON)
                .setTags(Collections.emptySet())
                .setNextOpLogOrder(2L)
                .build();
        AccountModel accountMan = accountModel(YP_ID, TEST_ACCOUNT_SPACE_3_ID, "test-id", "test",
                folder.getId(), "Test", null, null, false);
        AccountModel accountSas = accountModel(YP_ID, TEST_ACCOUNT_SPACE_2_ID, "test-id-1", "test-1",
                folder.getId(), "Test-1", null, null, false);
        QuotaModel quota1 = quotaModel(YP_ID, YP_HDD_MAN, folder.getId(), 300L, 100L, 100L);
        QuotaModel quota2 = quotaModel(YP_ID, YP_SSD_MAN, folder.getId(), 100L, 0L, 50L);
        QuotaModel quota3 = quotaModel(YP_ID, YP_HDD_SAS, folder.getId(), 200L, 50L, 75L);
        AccountsQuotasModel provision = accountQuotaModel(YP_ID, YP_HDD_MAN, folder.getId(),
                accountMan.getId(), 100L, 30L, null, null);
        AccountsQuotasModel provision2 = accountQuotaModel(YP_ID, YP_SSD_MAN, folder.getId(),
                accountMan.getId(), 50L, 50L, null, null);
        AccountsQuotasModel provision3 = accountQuotaModel(YP_ID, YP_HDD_SAS, folder.getId(),
                accountSas.getId(), 75L, 10L, null, null);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> folderDao
                        .upsertOneRetryable(txSession, folder)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .upsertOneRetryable(txSession, accountMan)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsDao
                        .upsertOneRetryable(txSession, accountSas)))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertAllRetryable(txSession, List.of(quota1, quota2, quota3))))
                .block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .upsertAllRetryable(txSession, List.of(provision, provision2, provision3))))
                .block();

        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=" + TEST_SERVICE_ID_DISPENSER)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        Optional<ExpandedFolder> expandedFolderOptional = responseBody.getFolders().stream()
                .filter(folderModel -> folderModel.getFolder().getId().equals(folder.getId()))
                .findFirst();
        Assertions.assertTrue(expandedFolderOptional.isPresent());
        ExpandedFolder expandedFolder = expandedFolderOptional.get();

        List<ExpandedProvider> providers = expandedFolder.getProviders();
        Assertions.assertEquals(1, providers.size());
        ExpandedProvider expandedProvider = providers.get(0);
        Assertions.assertEquals(YP_ID, expandedProvider.getProviderId());

        Assertions.assertEquals(List.of(
                new ExpandedResourceType(YP_SSD, List.of(new ExpandedResourceBuilder()
                        .setResourceId(YP_SSD_MAN)
                        .setQuota(new AmountDto(
                                "0", "GB",
                                "100", "B",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setBalance(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setPositiveBalance(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setNegativeBalance(null)
                        .setFrozenQuota(new AmountDto(
                                "0", "GB",
                                "50", "B",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setProvided(new AmountDto(
                                "0", "GB",
                                "50", "B",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setProvidedRatio(new BigDecimal("0.5", new MathContext(1)))
                        .setAllocated(new AmountDto(
                                "0", "GB",
                                "50", "B",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .setAllocatedRatio(new BigDecimal("0.5", new MathContext(1)))
                        .setProvidedAndNotAllocated(new AmountDto(
                                "0", "GB",
                                "0", "GB",
                                "0", GIGABYTES,
                                "0", GIGABYTES
                        ))
                        .buildExpandedResource()),
                        new FrontAmountsDto(
                                new AmountDto(
                                        "0", "GB",
                                        "100", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "0", "GB",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "0", "GB",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                null,
                                new AmountDto(
                                        "0", "GB",
                                        "50", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "50", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new BigDecimal("0.5", new MathContext(1)),
                                new AmountDto(
                                        "0", "GB",
                                        "50", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new BigDecimal("0.5", new MathContext(1))
                        )),
                new ExpandedResourceType(YP_HDD,
                        List.of(new ExpandedResourceBuilder()
                                        .setResourceId(YP_HDD_SAS)
                                        .setQuota(new AmountDto(
                                                "0", "GB",
                                                "200", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setBalance(new AmountDto(
                                                "0", "GB",
                                                "50", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setPositiveBalance(new AmountDto(
                                                "0", "GB",
                                                "50", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setNegativeBalance(null)
                                        .setFrozenQuota(new AmountDto(
                                                "0", "GB",
                                                "75", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setProvided(new AmountDto(
                                                "0", "GB",
                                                "75", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setProvidedRatio(new BigDecimal("0.375", new MathContext(3)))
                                        .setAllocated(new AmountDto(
                                                "0", "GB",
                                                "10", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setAllocatedRatio(new BigDecimal("0.05", new MathContext(2)))
                                        .setProvidedAndNotAllocated(new AmountDto(
                                                "0", "GB",
                                                "65", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .buildExpandedResource(),
                                new ExpandedResourceBuilder()
                                        .setResourceId(YP_HDD_MAN)
                                        .setQuota(new AmountDto(
                                                "0", "GB",
                                                "300", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setBalance(new AmountDto(
                                                "0", "GB",
                                                "100", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setPositiveBalance(new AmountDto(
                                                "0", "GB",
                                                "100", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setNegativeBalance(null)
                                        .setFrozenQuota(new AmountDto(
                                                "0", "GB",
                                                "100", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setProvided(new AmountDto(
                                                "0", "GB",
                                                "100", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setProvidedRatio(new BigDecimal("0.3333333333333333", new MathContext(16)))
                                        .setAllocated(new AmountDto(
                                                "0", "GB",
                                                "30", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .setAllocatedRatio(new BigDecimal("0.1", new MathContext(1)))
                                        .setProvidedAndNotAllocated(new AmountDto(
                                                "0", "GB",
                                                "70", "B",
                                                "0", GIGABYTES,
                                                "0", GIGABYTES
                                        ))
                                        .buildExpandedResource()),
                        new FrontAmountsDto(
                                new AmountDto(
                                        "0", "GB",
                                        "500", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "150", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "150", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                null,
                                new AmountDto(
                                        "0", "GB",
                                        "175", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new AmountDto(
                                        "0", "GB",
                                        "175", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new BigDecimal("0.35", new MathContext(2)),
                                new AmountDto(
                                        "0", "GB",
                                        "40", "B",
                                        "0", GIGABYTES,
                                        "0", GIGABYTES
                                ),
                                new BigDecimal("0.08", new MathContext(2))
                        ))
        ), expandedProvider.getResourceTypes());
    }

    @Test
    public void resourceAllowedUnitsShouldBeSorted() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=1")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        ResourceDto ypHddMan = responseBody.getResources().stream().filter(
                resourceDto -> resourceDto.getId().equals(YP_HDD_MAN))
                .findFirst().get();

        Assertions.assertEquals(List.of(
                GIGABYTES,
                UnitIds.TERABYTES,
                UnitIds.PETABYTES
        ), ypHddMan.getResourceUnits().getAllowedUnitIds());


        ResourceDto ypSsdVla = responseBody.getResources().stream().filter(
                resourceDto -> resourceDto.getId().equals(YP_SSD_VLA))
                .findFirst().get();

        Assertions.assertEquals(List.of(
                GIGABYTES,
                UnitIds.TERABYTES,
                UnitIds.PETABYTES
        ), ypSsdVla.getResourceUnits().getAllowedUnitIds());

        ResourceDto ypCpuMan = responseBody.getResources().stream().filter(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_CPU_MAN))
                .findFirst().get();

        Assertions.assertEquals(List.of(
                UnitIds.MILLICORES,
                UnitIds.CORES
        ), ypCpuMan.getResourceUnits().getAllowedUnitIds());
    }

    @Test
    public void getClosingServiceTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=13")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
    }

    @Test
    public void getNonExportableServiceTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=15")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);
    }

    @Test
    public void getByEmptyServiceTestAnother() {
        EntityExchangeResult<FrontFolderWithQuotesDto> response = webClient
                .mutateWith(MockUser.uid("1120000000000005"))
                .get()
                .uri("/front/folders?serviceId=10")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult();

        Assertions.assertEquals(HttpStatus.OK, response.getStatus());
        Assertions.assertNotNull(response.getResponseBody());
        Assertions.assertTrue(response.getResponseBody().getFolders().stream()
                .map(ExpandedFolder::getFolder)
                .map(FrontFolderDto::getFolderType)
                .anyMatch(folderType -> folderType == FolderType.COMMON_DEFAULT_FOR_SERVICE)
        );
    }

    @Test
    public void hidingVirtualResourcesTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=3")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(responseBody);

        ExpandedProvider virtualProvider = responseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID))
                .findFirst()
                .orElseThrow()
                .getProviders().stream()
                .filter(p -> p.getProviderId().equals(YP_ID))
                .findFirst()
                .orElseThrow();
        Optional<ResourceDto> virtualResource = responseBody.getResources().stream()
                .filter(r -> r.getId().equals(YP_VIRTUAL)).findAny();
        Optional<ExpandedAccount> virtualAccount = virtualProvider.getAccounts().stream()
                .filter(a -> a.getAccount().getId().equals(TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID))
                .findFirst();
        Assertions.assertTrue(virtualProvider.getResourceTypes().isEmpty()); // only virtual resource => not shown;
        Assertions.assertTrue(virtualAccount.isPresent()); // but accounts should be present
        Assertions.assertTrue(virtualAccount.get().getResources().isEmpty()); // and have no resources;
        Assertions.assertTrue(virtualResource.isPresent()); // virtual resource should be listed in resources list

        ExpandedProvider nonVirtualProvider = responseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID))
                .findFirst()
                .orElseThrow()
                .getProviders().stream()
                .filter(p -> p.getProviderId().equals(YDB_ID))
                .findFirst()
                .orElseThrow();
        Optional<ResourceDto> nonVirtualResource = responseBody.getResources().stream()
                .filter(r -> r.getId().equals(YDB_RAM_SAS)).findAny();
        Optional<ExpandedAccount> nonVirtualAccount = nonVirtualProvider.getAccounts().stream()
                .filter(a -> a.getAccount().getId().equals(TEST_ACCOUNT_WITH_NON_VIRTUAL_RESOURCES_ID))
                .findFirst();
        // another provider with non-virtual resources should be returned as it is
        Assertions.assertEquals(1, nonVirtualProvider.getResourceTypes().size());
        Assertions.assertTrue(nonVirtualAccount.isPresent());
        Assertions.assertEquals(1, nonVirtualAccount.get().getResources().size());
        Assertions.assertTrue(nonVirtualResource.isPresent());

        // add non-virtual resource with zero quota to provider with only virtual resources
        QuotaModel zeroQuota = quotaModel(YP_ID, YP_HDD_MAN, TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID, 0L, 0L, 0L);
        AccountsQuotasModel zeroAccountQuota = accountQuotaModel(YP_ID, YP_HDD_MAN,
                TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID, TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID,
                0, 0, null, null);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertOneRetryable(txSession, zeroQuota))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .upsertOneRetryable(txSession, zeroAccountQuota))).block();
        FrontFolderWithQuotesDto secondResponseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=3")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResponseBody);
        // resources are still hidden because non-virtual resource has zero quota
        ExpandedProvider updatedVirtualProvider = secondResponseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID))
                .findFirst()
                .orElseThrow()
                .getProviders().stream()
                .filter(p -> p.getProviderId().equals(YP_ID))
                .findFirst()
                .orElseThrow();
        List<ResourceDto> updatedVirtualProviderResources = secondResponseBody.getResources().stream()
                .filter(r -> r.getProviderId().equals(YP_ID))
                .toList();
        Optional<ExpandedAccount> updatedVirtualAccount = updatedVirtualProvider.getAccounts().stream()
                .filter(a -> a.getAccount().getId().equals(TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID))
                .findFirst();
        // quota for non-virtual resource is zero => nothing changes; resources in accounts are hidden
        Assertions.assertTrue(updatedVirtualProvider.getResourceTypes().isEmpty());
        Assertions.assertTrue(updatedVirtualAccount.isPresent());
        Assertions.assertTrue(updatedVirtualAccount.get().getResources().isEmpty());
        // resources with zero quota are hidden in the overall list, but virtual resource with nonzero quota is shown
        Assertions.assertEquals(1, updatedVirtualProviderResources.size());
        Assertions.assertEquals(YP_VIRTUAL, updatedVirtualProviderResources.get(0).getId());

        // add nonzero quota for non-virtual resource to provider with only virtual resources
        QuotaModel extraQuota = quotaModel(YP_ID, YP_HDD_MAN, TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID,
                300L, 100L, 100L);
        AccountsQuotasModel extraAccountQuota = accountQuotaModel(YP_ID, YP_HDD_MAN,
                TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID,
                TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID, 100L, 30L, null, null);
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> quotasDao
                        .upsertOneRetryable(txSession, extraQuota))).block();
        tableClient.usingSessionMonoRetryable(session -> session.usingTxMonoRetryable(
                TransactionMode.SERIALIZABLE_READ_WRITE, txSession -> accountsQuotasDao
                        .upsertOneRetryable(txSession, extraAccountQuota))).block();
        FrontFolderWithQuotesDto thirdResponseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders?serviceId=3")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(thirdResponseBody);
        ExpandedProvider reUpdatedVirtualProvider = thirdResponseBody.getFolders().stream()
                .filter(f -> f.getFolder().getId().equals(TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID))
                .findFirst()
                .orElseThrow()
                .getProviders().stream()
                .filter(p -> p.getProviderId().equals(YP_ID))
                .findFirst()
                .orElseThrow();
        List<ResourceDto> reUpdatedVirtualProviderResources = thirdResponseBody.getResources().stream()
                .filter(r -> r.getProviderId().equals(YP_ID))
                .toList();
        Optional<ExpandedAccount> reUpdatedVirtualAccount = reUpdatedVirtualProvider.getAccounts().stream()
                .filter(a -> a.getAccount().getId().equals(TEST_ACCOUNT_WITH_ONLY_VIRTUAL_RESOURCES_ID))
                .findFirst();
        // quota for non-virtual resource is nonzero => nothing is hidden
        Assertions.assertEquals(2, reUpdatedVirtualProvider.getResourceTypes().size());
        Assertions.assertTrue(reUpdatedVirtualAccount.isPresent());
        Assertions.assertEquals(2, reUpdatedVirtualAccount.get().getResources().size());
        Assertions.assertEquals(2, reUpdatedVirtualProviderResources.size());
        Assertions.assertTrue(reUpdatedVirtualProviderResources.stream()
                .map(ResourceDto::getId)
                .allMatch(id -> id.equals(YP_VIRTUAL) || id.equals(YP_HDD_MAN)));
    }
}
