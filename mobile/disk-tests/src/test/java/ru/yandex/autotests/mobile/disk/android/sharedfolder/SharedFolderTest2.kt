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
import ru.yandex.autotests.mobile.disk.android.rules.annotations.invite.SharedFolder
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders

@Feature("Shared folders")
@UserTags("share")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class SharedFolderTest2 : SharedFolderTestRunner() {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Test
    @TmsLink("1710")
    @SharedFolder
    @Category(FullRegress::class)
    fun shouldNotSeeSharedFolderAfterKickingFromGroup() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onShareDiskApi.kickFromGroup(folderName, testAccount)
        onFiles.updateFileList()
        onFiles.shouldNotExistFilesOrFolders(folderName)
    }

    @Test
    @TmsLink("1716")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldNotSeeInvitationForAnotherAccount() {
        //share folder in test for avoiding invitation screen after login.
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName)
        onSettings.closeInvitationListIfDisplayed()
        onSettings.logoutOnProfilePage()
        onLogin.shouldSwitchToLoginFormIfAlreadyLogged()
        onLogin.shouldLoginIntoApp(anotherAccount) //use another account
        onLogin.closeWizards()
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldNotSeeInvite(folderName)
    }

    @Test
    @TmsLink("1712")
    @SharedFolder
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldRenameSharedFolderByUser() {
        val folderName = nameHolder.name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.renameFileOrFolder(folderName, FilesAndFolders.ORIGINAL_FOLDER)
        onFiles.shouldFilesOrFoldersExist(FilesAndFolders.ORIGINAL_FOLDER)
        onShareDiskApi.shouldFolderExist(folderName) //TODO: step FolderNotExist
    }

    @Test
    @TmsLink("1714")
    @SharedFolder
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldCopySharedFolderByOwner() {
        val folderName = nameHolder.name
        val targetFolder = nameHolder.generateName().name
        onBasePage.openFiles()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onShareDiskApi.copy(folderName, targetFolder, true)
        onFiles.updateFileList()
        onFiles.shouldFilesOrFoldersExist(folderName)
        onFiles.shouldNotExistFilesOrFolders(targetFolder)
    }

    @Test
    @TmsLink("1687")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldNotSeeInvitesOnAirplaneMode() {
        val folderName = nameHolder.generateName().name
        onSettings.sendInvitation(folderName, testAccount, Rights.RO)
        onBasePage.openSharedFolderInvitesList()
        onSettings.closeInvitationListIfDisplayed()
        onSettings.switchToAirplaneMode()
        onSettings.closeProfile()
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldServiceUnavailableStubBePresented()
    }

    @Test
    @TmsLink("1694")
    @DeleteSharedFolders
    @Category(FullRegress::class)
    fun shouldSeeSettingsWhenCancelSingleInvitationFromSeveral() {
        val folderName1 = nameHolder.generateName().name
        val folderName2 = nameHolder.generateName().name
        onSettings.sendInvitation(folderName1, testAccount, Rights.RO)
        onSettings.sendInvitation(folderName2, testAccount, Rights.RW)
        onSettings.closeInvitationListIfDisplayed()
        onBasePage.openFiles()
        onBasePage.openSharedFolderInvitesList()
        onSettings.shouldSeeInvite(folderName1)
        onSettings.declineInvitation(folderName1)
        onSettings.shouldSeeInvite(folderName2)
        onSettings.shouldNotSeeInvite(folderName1)
    }
}
