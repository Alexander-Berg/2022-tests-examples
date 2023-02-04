package ru.yandex.intranet.d.web.controllers.front;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestAccounts;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResourceTypes;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestSegmentations;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.AmountDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
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
import ru.yandex.intranet.d.web.model.resources.ResourceSegmentationSegmentDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_1_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2_ID;
import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_7_ID;
import static ru.yandex.intranet.d.TestResources.YP_HDD_MAN;
import static ru.yandex.intranet.d.UnitIds.GIGABYTES;

/**
 * FrontQuotasControllerTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @see FrontQuotasController
 * @since 20-02-2021
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@IntegrationTest
class FrontQuotasControllerTest {
    @Autowired
    private WebTestClient webClient;

    /**
     * Get folders test.
     *
     * @see FrontQuotasController#getFolders
     */
    @Test
    public void getFoldersTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + "," + TEST_FOLDER_2_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertEquals(2, responseBody.getFolders().size());
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_1_ID)));
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_2_ID)));

        assertEquals(6, responseBody.getResources().size());
        assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_HDD_MAN)));
        assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)));

        ResourceSegmentationSegmentDto ypSsdVlaResourceLocation = responseBody.getResources().stream()
                .filter(resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)).findFirst().get()
                .getResourceSegments().stream()
                .filter(segment -> segment.getSegmentationId().equals(TestSegmentations.YP_LOCATION)).findFirst().get();
        assertEquals("Location", ypSsdVlaResourceLocation.getSegmentationName());
        assertEquals("VLA", ypSsdVlaResourceLocation.getSegmentName());

        assertEquals(1, responseBody.getProviders().size());
        assertEquals(TestProviders.YP_ID, responseBody.getProviders().get(0).getId());

        assertEquals(6, responseBody.getAccountsSpaces().size());

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_1_ID
        )).findFirst().get();
        assertEquals(Set.of(FolderPermission.CAN_MANAGE_ACCOUNT, FolderPermission.CAN_UPDATE_PROVISION),
                folder.getPermissions());
        FrontFolderDto folderDto = folder.getFolder();
        assertNotNull(folderDto);
        assertEquals(FolderType.COMMON, folderDto.getFolderType());
        assertEquals("Проверочная папка", folderDto.getDisplayName());

        ExpandedProvider yp = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().get();
        assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                yp.getPermissions());
        ExpandedResourceType ypHdd =
                yp.getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                        TestResourceTypes.YP_HDD
                )).findFirst().get();

        assertEquals("100", ypHdd.getSums().getAllocated().getForEditAmount());

        ExpandedResource ypHddMan = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();

        assertEquals("1", ypHddMan.getQuota().getReadableAmount());
        assertEquals("TB", ypHddMan.getQuota().getReadableUnit());
        assertEquals("800", ypHddMan.getBalance().getForEditAmount());
        assertEquals("100000000000", ypHddMan.getAllocated().getRawAmount());

        ExpandedResource ypHddMyt = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MYT
        )).findFirst().get();
        assertEquals("0", ypHddMyt.getQuota().getReadableAmount());
        assertEquals("GB", ypHddMyt.getQuota().getReadableUnit());
        assertEquals("0", ypHddMyt.getQuota().getForEditAmount());
        assertEquals(GIGABYTES, ypHddMyt.getQuota().getForEditUnitId());
        assertEquals("-80", ypHddMyt.getBalance().getReadableAmount());
        assertEquals("GB", ypHddMyt.getBalance().getReadableUnit());
        assertEquals("-80", ypHddMyt.getBalance().getForEditAmount());
        assertEquals(GIGABYTES, ypHddMyt.getBalance().getForEditUnitId());

        ExpandedAccount account = yp.getAccounts().stream().filter(a -> a.getAccount().getId().equals(
                TestAccounts.TEST_ACCOUNT_1_ID
        )).findFirst().get();
        ExpandedAccountResource accountYpHddMan = account.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();

        assertEquals("100", accountYpHddMan.getAllocated().getForEditAmount());

        ExpandedFolder folder2 = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_2_ID
        )).findFirst().get();
        FrontFolderDto folder2Dto = folder2.getFolder();
        assertNotNull(folder2Dto);
        assertEquals(FolderType.COMMON_DEFAULT_FOR_SERVICE, folder2Dto.getFolderType());

        ExpandedResource ypHddManFolder2 = folder2.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().get().getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                TestResourceTypes.YP_HDD
        )).findFirst().get().getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();
        assertNotNull(ypHddManFolder2.getAllocated());
        assertEquals("0", ypHddManFolder2.getAllocated().getRawAmount());
    }

    @Test
    public void getFoldersByProviderAdminTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_3_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + "," + TEST_FOLDER_2_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertEquals(2, responseBody.getFolders().size());
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_1_ID)));
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_2_ID)));

        assertEquals(6, responseBody.getResources().size());
        assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_HDD_MAN)));
        assertTrue(responseBody.getResources().stream().anyMatch(
                resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)));

        ResourceSegmentationSegmentDto ypSsdVlaResourceLocation = responseBody.getResources().stream()
                .filter(resourceDto -> resourceDto.getId().equals(TestResources.YP_SSD_VLA)).findFirst().get()
                .getResourceSegments().stream()
                .filter(segment -> segment.getSegmentationId().equals(TestSegmentations.YP_LOCATION)).findFirst().get();
        assertEquals("Location", ypSsdVlaResourceLocation.getSegmentationName());
        assertEquals("VLA", ypSsdVlaResourceLocation.getSegmentName());

        assertEquals(1, responseBody.getProviders().size());
        assertEquals(TestProviders.YP_ID, responseBody.getProviders().get(0).getId());

        assertEquals(6, responseBody.getAccountsSpaces().size());

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_1_ID
        )).findFirst().get();
        assertEquals(Set.of(), folder.getPermissions());
        FrontFolderDto folderDto = folder.getFolder();
        assertNotNull(folderDto);
        assertEquals(FolderType.COMMON, folderDto.getFolderType());
        assertEquals("Проверочная папка", folderDto.getDisplayName());

        ExpandedProvider yp = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().get();
        ExpandedProvider ydb = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YDB_ID
        )).findFirst().get();
        assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                yp.getPermissions());
        assertEquals(Set.of(), ydb.getPermissions());
        ExpandedResourceType ypHdd =
                yp.getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                        TestResourceTypes.YP_HDD
                )).findFirst().get();

        assertEquals("100", ypHdd.getSums().getAllocated().getForEditAmount());

        ExpandedResource ypHddMan = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();

        assertEquals("1", ypHddMan.getQuota().getReadableAmount());
        assertEquals("TB", ypHddMan.getQuota().getReadableUnit());
        assertEquals("800", ypHddMan.getBalance().getForEditAmount());
        assertEquals("100000000000", ypHddMan.getAllocated().getRawAmount());

        ExpandedResource ypHddMyt = ypHdd.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MYT
        )).findFirst().get();
        assertEquals("0", ypHddMyt.getQuota().getReadableAmount());
        assertEquals("GB", ypHddMyt.getQuota().getReadableUnit());
        assertEquals("0", ypHddMyt.getQuota().getForEditAmount());
        assertEquals(GIGABYTES, ypHddMyt.getQuota().getForEditUnitId());
        assertEquals("-80", ypHddMyt.getBalance().getReadableAmount());
        assertEquals("GB", ypHddMyt.getBalance().getReadableUnit());
        assertEquals("-80", ypHddMyt.getBalance().getForEditAmount());
        assertEquals(GIGABYTES, ypHddMyt.getBalance().getForEditUnitId());

        ExpandedAccount account = yp.getAccounts().stream().filter(a -> a.getAccount().getId().equals(
                TestAccounts.TEST_ACCOUNT_1_ID
        )).findFirst().get();
        ExpandedAccountResource accountYpHddMan = account.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();

        assertEquals("100", accountYpHddMan.getAllocated().getForEditAmount());

        ExpandedFolder folder2 = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_2_ID
        )).findFirst().get();
        FrontFolderDto folder2Dto = folder2.getFolder();
        assertNotNull(folder2Dto);
        assertEquals(FolderType.COMMON_DEFAULT_FOR_SERVICE, folder2Dto.getFolderType());
        assertEquals(Set.of(), folder2.getPermissions());

        ExpandedProvider yp2 = folder2.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().orElseThrow();
        assertEquals(Set.of(ProviderPermission.CAN_MANAGE_ACCOUNT, ProviderPermission.CAN_UPDATE_PROVISION),
                yp2.getPermissions());
        ExpandedResource ypHddManFolder2 = folder2.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().get().getResourceTypes().stream().filter(t -> t.getResourceTypeId().equals(
                TestResourceTypes.YP_HDD
        )).findFirst().get().getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();
        assertNotNull(ypHddManFolder2.getAllocated());
        assertEquals("0", ypHddManFolder2.getAllocated().getRawAmount());
    }

    /**
     * Get folders should not return completely zero quotas test.
     * @see FrontQuotasController#getFolders
     */
    @Test
    public void getFoldersShouldNotReturnCompletelyZeroQuotasTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_7_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();
        assertNotNull(responseBody);
        assertEquals(1, responseBody.getFolders().size());
        assertEquals(TEST_FOLDER_7_ID, responseBody.getFolders().get(0).getFolder().getId());

        assertEquals(1, responseBody.getResources().size());
        assertEquals(YP_HDD_MAN, responseBody.getResources().get(0).getId());

        assertEquals(1, responseBody.getProviders().size());
        assertEquals(TestProviders.YP_ID, responseBody.getProviders().get(0).getId());

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
    public void folderPermissionsMustBeInResponseBody() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + "," + TEST_FOLDER_2_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertEquals(2, responseBody.getFolders().size());
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_1_ID)));
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_2_ID)));

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_1_ID
        )).findFirst().get();

        assertEquals(Set.of(FolderPermission.CAN_MANAGE_ACCOUNT, FolderPermission.CAN_UPDATE_PROVISION),
                folder.getPermissions());

        responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_2_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + "," + TEST_FOLDER_2_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);
        assertEquals(2, responseBody.getFolders().size());
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_1_ID)));
        assertTrue(responseBody.getFolders().stream().anyMatch(
                expandedFolder -> expandedFolder.getFolder().getId().equals(TEST_FOLDER_2_ID)));

        folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_1_ID
        )).findFirst().get();

        assertEquals(Set.of(), folder.getPermissions());
    }


    /**
     * Folder not found test.
     *
     * @see FrontQuotasController#getFolders
     */
    @Test
    public void getFoldersNotFoundTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + ",bad-folder-id")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .consumeWith(r -> {
                    ErrorCollectionDto errorCollection = r.getResponseBody();
                    Assertions.assertNotNull(errorCollection);
                    Set<String> errorMessage = errorCollection.getFieldErrors().get("folderIds");
                    Assertions.assertEquals(Set.of("Folder not found."), errorMessage);

                    Set<Object> unknownFolderIdsWrapped = errorCollection.getDetails().get("unknownFolderIds");
                    Assertions.assertEquals(1, unknownFolderIdsWrapped.size());
                    Object unknownFolderIdsUnwrapped = List.copyOf(unknownFolderIdsWrapped).get(0);
                    Assertions.assertTrue(List.class.isAssignableFrom(unknownFolderIdsUnwrapped.getClass()));
                    //noinspection unchecked
                    List<String> unknownFolderIds = (List<String>) unknownFolderIdsUnwrapped;
                    Assertions.assertEquals(List.of("bad-folder-id"), unknownFolderIds);
                });
    }

    /**
     * ProvidedAndNotAllocated test.
     *
     * @see FrontQuotasController#getFolders
     */
    @Test
    public void getFoldersReturnedProvidedAndNotAllocatedTest() {
        FrontFolderWithQuotesDto responseBody = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/quotas/_folders?folderIds=" + TEST_FOLDER_1_ID + "," + TEST_FOLDER_2_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderWithQuotesDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(responseBody);

        ExpandedFolder folder = responseBody.getFolders().stream().filter(f -> f.getFolder().getId().equals(
                TEST_FOLDER_1_ID
        )).findFirst().get();

        ExpandedProvider yp = folder.getProviders().stream().filter(p -> p.getProviderId().equals(
                TestProviders.YP_ID
        )).findFirst().get();

        ExpandedAccount account = yp.getAccounts().stream().filter(a -> a.getAccount().getId().equals(
                TestAccounts.TEST_ACCOUNT_1_ID
        )).findFirst().get();
        ExpandedAccountResource accountYpHddMan = account.getResources().stream().filter(r -> r.getResourceId().equals(
                TestResources.YP_HDD_MAN
        )).findFirst().get();

        assertEquals(new ExpandedResourceBuilder()
                .setResourceId(YP_HDD_MAN)
                .setProvided(new AmountDto(
                        "200", "GB",
                        "200000000000", "B",
                        "200", GIGABYTES,
                        "200", GIGABYTES
                ))
                .setProvidedRatio(BigDecimal.ZERO)
                .setAllocated(new AmountDto(
                        "100", "GB",
                        "100000000000", "B",
                        "100", GIGABYTES,
                        "100", GIGABYTES
                ))
                .setAllocatedRatio(BigDecimal.ZERO)
                .setProvidedAndNotAllocated(new AmountDto(
                        "100", "GB",
                        "100000000000", "B",
                        "100", GIGABYTES,
                        "100", GIGABYTES
                ))
                .buildExpandedAccountResource(),
                accountYpHddMan);
    }
}
