package ru.yandex.intranet.d;

import java.util.Set;

import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.model.folders.FolderModel;
import ru.yandex.intranet.d.model.folders.FolderType;

/**
 * TestFolders.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 */
public class TestFolders {
    public static final String TEST_FOLDER_1_ID = "f714c483-c347-41cc-91d0-c6722f5daac7";
    public static final String SECOND_TEST_FOLDER_OF_SERVICE_1 = "19e94a88-e993-4d69-9399-99042337f5eb";
    public static final long TEST_FOLDER_1_SERVICE_ID = 1;
    public static final FolderModel TEST_FOLDER_1 = new FolderModel(
            TEST_FOLDER_1_ID,
            Tenants.DEFAULT_TENANT_ID,
            TEST_FOLDER_1_SERVICE_ID,
            0,
            "Проверочная папка",
            "Папка для проверки",
            false,
            FolderType.COMMON,
            Set.of("testing", "red"),
            0L
    );
    public static final String TEST_FOLDER_1_RESERVE_ID = "5f73c44f-7569-43d6-980d-7ee663c9789d";

    public static final String TEST_FOLDER_2_ID = "11d1bcdb-3edc-4c21-8a79-4570e3c09c21";
    public static final FolderModel TEST_FOLDER_2 = new FolderModel(
            TEST_FOLDER_2_ID,
            Tenants.DEFAULT_TENANT_ID,
            TEST_FOLDER_1_SERVICE_ID,
            0,
            "Проверочная папка",
            "Папка для проверки",
            false,
            FolderType.COMMON_DEFAULT_FOR_SERVICE,
            Set.of("testing", "red"),
            0L
    );
    public static final String TEST_FOLDER_2_RESERVE_ID = "3d25e184-b1be-47ca-8ba2-10232f4c2f6c";

    public static final String TEST_FOLDER_3_ID = "aa6a5d64-5b94-4057-8d43-e65812475e73";

    public static final String TEST_FOLDER_4_ID = "81c764b2-ba16-4aaa-8016-38cd37507ef6";

    public static final String TEST_IMPORT_FOLDER_ID = "2e8f9b1c-7b3a-41ba-9067-2dee22b046a1";

    public static final String TEST_FOLDER_SERVICE_D = "f2fe5d5d-b19f-44f6-9bf7-b5a232b81846";
    public static final String TEST_FOLDER_IN_CLOSING_SERVICE = "57610b2b-4539-41b5-a952-c9a17c264649";
    public static final String TEST_FOLDER_IN_NON_EXPORTABLE_SERVICE = "b83d4d70-81f8-40bb-a15a-935626c86d4e";
    public static final String TEST_FOLDER_IN_RENAMING_SERVICE = "61270433-24cd-4cb1-9e00-1ccc596227ed";

    public static final String TEST_FOLDER_5_ID = "c631fd62-e739-486e-b8b1-cd68550bd3cf";

    public static final String TEST_FOLDER_6_ID = "a38cc4b6-02b8-4425-b4e3-232016a6569f";
    public static final long TEST_FOLDER_6_SERVICE_ID = 6;

    public static final String TEST_FOLDER_17_ID = "da42ad45-6e9d-4ede-b935-bd851366bcc3";
    public static final long TEST_FOLDER_17_SERVICE_ID = 17;

    public static final String TEST_FOLDER_7_ID = "b2872163-18fb-44ef-9365-b66f7756d636";

    public static final String TEST_FOLDER_9_ID = "32d293e9-5887-4159-bdf1-53bdf4272c0f";
    public static final String TEST_FOLDER_WITH_VIRTUAL_RESOURCES_ID = "7d4745b7-6d80-e6d6-84f4-2510bbca38e8";
    public static final String TEST_FOLDER_WITH_UNMANAGED_PROVIDER_ID = "d509854c-95a0-67ff-194c-2e361c985de4";
    public static final String TEST_FOLDER_10_ID = "ff87caf5-0a03-7d49-dc72-4d8a08e99c0f";

    public static final String TEST_MARKET_DEFAULT_FOLDER_ID = "cd73b2bf-ad97-4252-b65d-3742de39b04c";

    private TestFolders() {
    }
}
