package ru.yandex.autotests.mobile.disk.android.sharedfolder

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadToSharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.InviteUser
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Shared folders")
@UserTags("share")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SharedFolderTest : SharedFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1692")
    @DeleteSharedFolders
    @Category(Regression::class)
    fun shouldAcceptFullAccessDirInvitationWithSwitchingToFolder() {
        //share folder in test for avoiding invitation screen after login.
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RW)
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openSharedFolderInvitesList()
        onSettings.acceptInvitation(folderName)
        onSettings.goToSharedFolderFromAcceptDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.shouldShareButtonBeEnabled()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionBePresented(FileActions.DELETE)
        onGroupMode.shouldActionBePresented(FileActions.MOVE)
        onGroupMode.shouldActionBePresented(FileActions.RENAME)
        onGroupMode.pressHardBack() //close more option slider
        onBasePage.closeGroupMode()
        onFiles.navigateUp()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.shouldNotSeeReadOnlyMark(folderName)
    }

    @Test
    @TmsLink("1696")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldChangeBehaviourWhenSharedFolderRightsBeChanged() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.switchToListLayout()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.shouldNotSeeReadOnlyMark(folderName)
        onShareDiskApi.changeRights(folderName, testAccount, Rights.RO)
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.updateFileList()
        onFiles.shouldSeeReadOnlyMark(folderName)
        onFiles.openFolder(folderName)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.shouldShareButtonBeNotPresented()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE)
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE)
        onGroupMode.shouldActionNotBePresented(FileActions.MOVE)
        onGroupMode.pressHardBack() //close more option slider
        onBasePage.closeGroupMode()
    }

    @Test
    @TmsLink("1779")
    @UploadToSharedFolder(filePaths = [FilesAndFolders.PHOTO])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RO))
    @Category(FullRegress::class)
    fun shouldSeeFabAndEditButtonsAfterChangingRightToFullAccessOnReadOnlyDir() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.shouldNotSeeFabButton()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.shouldNotEditorActionBePresented()
        onPreview.pressHardBack() //close more option menu
        onPreview.closePreview()
        onShareDiskApi.changeRights(folderName, testAccount, Rights.RW)
        onFiles.navigateUp()
        onFiles.updateFileList()
        onFiles.openFolder(folderName)
        onFiles.shouldSeeFabButton()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.shouldEditorActionBePresented()
    }

    @Test
    @TmsLink("1708")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotIncreaseUsedSpaceForMemberWhenUsedSpaceQuotaNotReachedOnMember() {
        val sharedFolderName = nameHolder.name
        val usedSpaceOnShareMasterBeforeUpload = onShareDiskApi.currentUsedSpace
        onBasePage.openProfile()
        val availableMemberDiskSpaceBeforeOperation = onSettings.getCurrentAvailableDiskSpace()
        onSettings.closeProfile()
        onShareDiskApi.uploadFileToFolder(FilesAndFolders.BIG_FILE, sharedFolderName)
        val currentUsedSpaceOnShareMaster = onShareDiskApi.currentUsedSpace
        onBasePage.openProfile()
        val currentAvailableMemberDiskSpace = onSettings.getCurrentAvailableDiskSpace()
        MatcherAssert.assertThat(
            currentUsedSpaceOnShareMaster,
            Matchers.greaterThan(usedSpaceOnShareMasterBeforeUpload)
        )
        MatcherAssert.assertThat(
            currentAvailableMemberDiskSpace,
            Matchers.equalTo(availableMemberDiskSpaceBeforeOperation)
        )
    }

    @Test
    @TmsLink("1711")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotRenameSharedFolderForMemberWhenRenamedByOwner() {
        val sharedFolderName = nameHolder.name
        nameHolder.generateName()
        val targetName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(sharedFolderName)
        onShareDiskApi.rename(sharedFolderName, targetName, true)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(sharedFolderName)
    }

    @Test
    @TmsLink("1713")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotmoveSharedFolderForMemberWhenMovedByOwner() {
        val sharedFolderName = nameHolder.name
        nameHolder.generateName()
        val targetName = nameHolder.name
        onShareDiskApi.createFolders(targetName)
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(sharedFolderName)
        onShareDiskApi.moveFileToFolder(sharedFolderName, targetName, true)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(sharedFolderName)
    }

    @Test
    @TmsLink("3399")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldSeeSettingsWhenCancelSingleInvitation() {
        val folderName1 = nameHolder.generateName().name
        onSettings.sendInvitation(folderName1, testAccount, Rights.RO)
        onSettings.closeInvitationListIfDisplayed()
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName1)
        onSettings.declineInvitation(folderName1)
        onSettings.shouldNotSeeInvite(folderName1)
        onBasePage.shouldBeOnProfile()
    }

    @Test
    @TmsLink("3413")
    @UploadToSharedFolder(filePaths = [FilesAndFolders.PHOTO])
    @SharedFolder(inviteUser = InviteUser(rights = Rights.RW))
    @Category(FullRegress::class)
    fun shouldSeeFabAndEditButtonsAfterChangingRightToReadOnlyOnFullAccessDir() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.openFolder(folderName)
        onFiles.shouldSeeFabButton()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.shouldEditorActionBePresented()
        onPreview.pressHardBack() //close more option menu
        onPreview.closePreview()
        onShareDiskApi.changeRights(folderName, testAccount, Rights.RO)
        onFiles.navigateUp()
        onFiles.updateFileList()
        onFiles.openFolder(folderName)
        onFiles.shouldNotSeeFabButton()
        onFiles.shouldOpenImageIntoViewer(FilesAndFolders.PHOTO)
        onPreview.shouldNotEditorActionBePresented()
    }
}
