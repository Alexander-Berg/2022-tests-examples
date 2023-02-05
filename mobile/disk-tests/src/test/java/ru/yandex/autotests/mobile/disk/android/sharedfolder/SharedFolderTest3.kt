package ru.yandex.autotests.mobile.disk.android.sharedfolder

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
import ru.yandex.autotests.mobile.disk.android.core.api.data.shared.Rights
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.DeleteSharedFolders
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FileActions
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Shared folders")
@UserTags("share")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SharedFolderTest3 : SharedFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1695")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldChangeAccessPermissionOnInviteWhenChangedOverWeb() {
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onSettings.closeInvitationListIfDisplayed()
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName)
        onSettings.closeInvitationListIfDisplayed()
        onSettings.closeProfile()
        onShareDiskApi.changeRights(folderName, testAccount, Rights.RW)
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldInviteHasFullAccessRights(folderName)
        onSettings.closeInvitationListIfDisplayed()
        onSettings.closeProfile()
        onShareDiskApi.changeRights(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldInviteHasReadOnlyRights(folderName)
        onSettings.closeInvitationListIfDisplayed()
    }

    @Test
    @TmsLink("1689")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldAcceptSharedFolderWithoutSwitchingToFolder() {
        //share folder in test for avoiding invitation screen after login.
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName)
        onSettings.acceptInvitation(folderName)
        onSettings.closeGoToSharedFolderDialog()
        onBasePage.shouldBeOnProfile()
    }

    @Test
    @TmsLink("1691")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldAcceptReadOnlyDirInvitationWithSwitchingToFolder() {
        //share folder in test for avoiding invitation screen after login.
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onShareDiskApi.createFolders(folderName + "/" + FilesAndFolders.ORIGINAL_FOLDER)
        onBasePage.openSharedFolderInvitesList()
        onSettings.acceptInvitation(folderName)
        onSettings.goToSharedFolderFromAcceptDialog()
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldSelectFilesOrFolders(FilesAndFolders.ORIGINAL_FOLDER)
        onGroupMode.shouldShareButtonBeNotPresented()
        onGroupMode.clickMoreOption()
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE)
        onGroupMode.shouldActionNotBePresented(FileActions.DELETE)
        onGroupMode.shouldActionNotBePresented(FileActions.MOVE)
        onGroupMode.pressHardBack() //close more option slider
        onBasePage.closeGroupMode()
        onFiles.navigateUp()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.shouldSeeReadOnlyMark(folderName)
    }

    @Test
    @TmsLink("1707")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldAcceptSharedFolderWhenFolderWithSameNameAlreadyExist() {
        val folderName = nameHolder.generateName().name
        onUserDiskApi.createFolders(folderName)
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName)
        onSettings.acceptInvitation(folderName)
        onSettings.closeGoToSharedFolderDialog()
        onSettings.closeProfile()
        onFiles.shouldFilesOrFoldersExist("$folderName (1)")
    }

    @Test
    @TmsLink("1715")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldSeePublicLinkWhenPublishedByOwner() {
        //Full Access directory
        var folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName!!, testAccount, Rights.RW)
        onBasePage.openSharedFolderInvitesList()
        onSettings.acceptInvitation(folderName)
        onSettings.closeGoToSharedFolderDialog()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onFiles.updateFileList()
        onFiles.shouldNotSeePublicFileMark(folderName)
        onShareDiskApi.publishFile(folderName)
        onFiles.updateFileList()
        onFiles.shouldSeePublicFileMark(folderName)

        //Read only directory
        folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.acceptInvitation(folderName)
        onSettings.closeGoToSharedFolderDialog()
        onSettings.closeProfile()
        onFiles.updateFileList()
        onFiles.shouldNotSeePublicFileMark(folderName)
        onShareDiskApi.publishFile(folderName)
        onFiles.updateFileList()
        onFiles.shouldSeePublicFileMark(folderName)
    }
}
