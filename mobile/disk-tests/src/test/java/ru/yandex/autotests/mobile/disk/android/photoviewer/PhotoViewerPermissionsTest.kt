package ru.yandex.autotests.mobile.disk.android.photoviewer

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.DoNotGrantPermissionsAutomatically
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import ru.yandex.autotests.mobile.disk.data.Permissions

@Feature("Photo viewer")
@UserTags("photoviewerPermissions")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@DoNotGrantPermissionsAutomatically
class PhotoViewerPermissionsTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onAdb: AdbSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onFiles: FilesSteps

    @Inject
    lateinit var onFeed: FeedSteps

    @Inject
    lateinit var onPermissions: PermissionsSteps

    @Test
    @TmsLink("6057")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1])
    fun shouldRequestPermissionInFiles() {
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
        onBasePage.openFiles()
        onFiles.shouldFileListBeDisplayed()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.FILE_FOR_VIEWING_1)
        onFiles.openImage(FilesAndFolders.FILE_FOR_VIEWING_1)
        onPermissions.shouldAcceptPermissionRequest()
        onAdb.shouldPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("5766")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.ONE_HOUR_VIDEO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.ONE_HOUR_VIDEO])
    fun shouldRequestPermissionInFeed() {
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPermissions.shouldAcceptPermissionRequest()
        onAdb.shouldPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }

    @Test
    @TmsLink("5767")
    @Category(BusinessLogic::class)
    @UploadFiles(filePaths = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.ONE_HOUR_VIDEO])
    @DeleteFiles(files = [FilesAndFolders.FILE_FOR_VIEWING_1, FilesAndFolders.FILE_FOR_VIEWING_2, FilesAndFolders.ONE_HOUR_VIDEO])
    fun shouldNotGrantPermissionIfDeny() {
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
        onBasePage.openFeed()
        onFeed.openFirstImage()
        onPermissions.shouldDeclinePermissionRequest()
        onAdb.shouldNotPermissionBeGranted(Permissions.READ_EXTERNAL_STORAGE)
    }
}
