package ru.yandex.intranet.d.web.admin.folders

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import ru.yandex.intranet.d.IntegrationTest
import ru.yandex.intranet.d.TestFolders.*
import ru.yandex.intranet.d.TestProviders
import ru.yandex.intranet.d.TestServices.*
import ru.yandex.intranet.d.TestUsers.*
import ru.yandex.intranet.d.model.folders.FolderType.COMMON
import ru.yandex.intranet.d.util.Uuids
import ru.yandex.intranet.d.web.MockUser
import ru.yandex.intranet.d.web.model.ErrorCollectionDto
import ru.yandex.intranet.d.web.model.folders.FrontFolderDto
import ru.yandex.intranet.d.web.model.folders.FrontFolderInputDto
import ru.yandex.intranet.d.web.model.providers.FullProviderDto

@IntegrationTest
class FoldersApiTest(
    @Autowired val webClient: WebTestClient,
) {
    @Test
    fun onlyForDAdminsTest() {
        val createResponse = webClient
            .mutateWith(MockUser.uid(NOT_D_ADMIN_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_a", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(1, createResponse.errors.size)
        assertTrue(createResponse.errors.contains("Access denied."))

        val updateResponse = webClient
            .mutateWith(MockUser.uid(NOT_D_ADMIN_UID))
            .put()
            .uri("/admin/folders/{id}", TEST_FOLDER_1_SERVICE_ID)
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_a", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!
        assertEquals(1, updateResponse.errors.size)
        assertTrue(updateResponse.errors.contains("Access denied."))
    }

    @Test
    fun createFolderTest() {
        val folder = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_a", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderDto::class.java)
            .returnResult()
            .responseBody!!
        assertNotNull(folder)
        assertEquals(COMMON, folder.folderType)
        assertTrue(Uuids.isValidUuid(folder.id))
    }

    @Test
    fun createFolderTestWrongTag() {
        val errorCollection = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_A", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(errorCollection)
        val errors = errorCollection.fieldErrors
        assertTrue(errors.containsKey("tags"))
        val details = errorCollection.details
        val tagsDetails = details["tags"]!!
        assertEquals(1, tagsDetails.size)
        val tagValues = tagsDetails.stream().findFirst().orElseThrow() as Map<*, *>
        assertEquals("tag_A", tagValues["wrong_value"])
        assertEquals("tag_a", tagValues["suggest_value"])
    }

    @Test
    fun createFolderInClosingServiceTest() {
        val errorCollection = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_SERVICE_ID_CLOSING,
                    "Folder name",
                    "Folder description",
                    setOf()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(errorCollection)
        assertNotNull(errorCollection.fieldErrors)
        val errors = errorCollection.fieldErrors["serviceId"]!!
        assertTrue(errors.contains("Current service status is not allowed."))

    }

    @Test
    fun createFolderInNonExportableServiceTest() {
        val errorCollection = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_SERVICE_ID_NON_EXPORTABLE,
                    "Folder name",
                    "Folder description",
                    setOf()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(errorCollection)
        assertNotNull(errorCollection.fieldErrors)
        val errors = errorCollection.fieldErrors["serviceId"]!!
        assertNotNull(errors)
        assertTrue(errors.contains("Services in the sandbox are not allowed."))
    }

    @Test
    fun createFolderInRenamingServiceTest() {
        webClient
            .mutateWith(MockUser.uid(USER_1_UID))
            .post()
            .uri("/admin/folders")
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_SERVICE_ID_RENAMING,
                    "Folder name",
                    "Folder description",
                    setOf()
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
    }

    @Test
    fun updateFolderTest() {
        val folder = webClient
            .mutateWith(MockUser.uid(USER_1_UID))
            .put()
            .uri("/admin/folders/{id}", TEST_FOLDER_1_ID)
            .bodyValue(
                FrontFolderInputDto(
                    0,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_a", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(folder)
        assertEquals(COMMON, folder.folderType)
        assertEquals(1, folder.version)
    }

    @Test
    fun updateFolderTestWrongVersion() {
        val errorCollection = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .put()
            .uri("/admin/folders/{id}", TEST_FOLDER_1_ID)
            .bodyValue(
                FrontFolderInputDto(
                    1,
                    TEST_FOLDER_1_SERVICE_ID,
                    "New folder name",
                    "New folder description",
                    setOf("tag_a", "tag_b")
                )
            )
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.PRECONDITION_FAILED)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        assertNotNull(errorCollection)
        val error = errorCollection.fieldErrors["version"]!!.stream().findFirst()
        assertTrue(error.isPresent)
        assertEquals("Version mismatch.", error.get())

    }

    @Test
    fun deleteFolderTest() {
        val expected = FrontFolderDto.from(
            TEST_FOLDER_1.toBuilder()
                .setDeleted(true)
                .setVersion(1)
                .setNextOpLogOrder(1L)
                .build()
        )
        webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .delete()
            .uri("/admin/folders/{id}", TEST_FOLDER_1_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderDto::class.java)
            .isEqualTo(expected)
    }

    @Test
    fun deleteReserveFolderTest() {
        val provider = webClient
            .mutateWith(MockUser.uid(USER_1_UID))
            .get()
            .uri("/admin/providers/{id}", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FullProviderDto::class.java)
            .returnResult()
            .responseBody
        assertNotNull(provider)
        assertEquals(provider!!.reserveFolderId, TEST_FOLDER_1_RESERVE_ID)

        webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .delete()
            .uri("/admin/folders/{id}", TEST_FOLDER_1_RESERVE_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FrontFolderDto::class.java)
            .returnResult()

        val result = webClient
            .mutateWith(MockUser.uid(USER_1_UID))
            .get()
            .uri("/admin/providers/{id}", TestProviders.YP_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(FullProviderDto::class.java)
            .returnResult()
            .responseBody
        assertNotNull(result)
        assertNull(result!!.reserveFolderId)

        webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .get()
            .uri("/front/folders/{id}", TEST_FOLDER_1_RESERVE_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun deleteFolderDefaultTest() {
        val errorCollection = webClient
            .mutateWith(MockUser.uid(D_ADMIN_UID))
            .delete()
            .uri("/admin/folders/{id}", TEST_FOLDER_2_ID)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.LOCKED)
            .expectBody(ErrorCollectionDto::class.java)
            .returnResult()
            .responseBody!!

        val error = errorCollection.errors.stream().findFirst()
        assertTrue(error.isPresent)
        assertEquals("Cannot delete default folder.", error.get())
    }
}
