package ru.yandex.intranet.d.services.elements;

import java.util.Locale;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;
import ru.yandex.intranet.d.util.paging.Page;
import ru.yandex.intranet.d.util.result.Result;

import static ru.yandex.intranet.d.TestFolders.TEST_FOLDER_2;
import static ru.yandex.intranet.d.dao.Tenants.DEFAULT_TENANT_ID;

/**
 * FolderServiceElementTest.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 16.09.2020
 */
@IntegrationTest
class FolderServiceElementTest {
    @Autowired
    FolderServiceElement folderServiceElement;

    @Test
    public void getOrCreateDefaultFolderTestExists() {
        Result<FolderModel> result = folderServiceElement.getOrCreateDefaultFolder(
                DEFAULT_TENANT_ID, 1, Locale.getDefault()
        ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        FolderModel folderModel = result.match(Function.identity(), errorCollection -> null);
        Assertions.assertEquals(TEST_FOLDER_2, folderModel);
    }

    @Test
    public void getOrCreateDefaultFolderTestNew() {
        Result<FolderModel> result = folderServiceElement.getOrCreateDefaultFolder(
                DEFAULT_TENANT_ID, 2, Locale.getDefault()
        ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        FolderModel folderModel = result.match(Function.identity(), errorCollection -> null);
        Assertions.assertEquals(2, folderModel.getServiceId());
        Assertions.assertEquals(FolderType.COMMON_DEFAULT_FOR_SERVICE, folderModel.getFolderType());
    }

    @Test
    public void listFoldersByServiceTestExistsDefault() {
        Result<Page<FolderModel>> result = folderServiceElement.listFoldersByService(
                DEFAULT_TENANT_ID, 1, false, 100, null, Locale.getDefault()
        ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Page<FolderModel> page = result.match(Function.identity(), errorCollection -> null);
        Assertions.assertEquals(3, page.getItems().size());
        Assertions.assertEquals(TEST_FOLDER_2, page.getItems().get(0));
    }

    @Test
    public void listFoldersByServiceTestNewDefault() {
        Result<Page<FolderModel>> result = folderServiceElement.listFoldersByService(
                DEFAULT_TENANT_ID, 2, false, 100, null, Locale.getDefault()
        ).block();

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Page<FolderModel> page = result.match(Function.identity(), errorCollection -> null);
        Assertions.assertEquals(3, page.getItems().size());
        Assertions.assertEquals(FolderType.COMMON_DEFAULT_FOR_SERVICE, page.getItems().get(0).getFolderType());
    }
}
