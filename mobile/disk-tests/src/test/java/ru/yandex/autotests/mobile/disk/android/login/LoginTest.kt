package ru.yandex.autotests.mobile.disk.android.login

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.LoginDiskTest
import ru.yandex.autotests.mobile.disk.android.StepsLocator
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.core.module.RuleChainModule.LOGIN
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Quarantine
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.DeleteFiles
import ru.yandex.autotests.mobile.disk.android.rules.annotations.files.UploadFileSpec
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.DeleteFilesOnDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.mobile.PushFileToDevice
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.RequireOnboarding
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.UploadFileType
import ru.yandex.autotests.mobile.disk.data.AccountConstants.BANNED_USER
import ru.yandex.autotests.mobile.disk.data.AccountConstants.TEST_USER
import ru.yandex.autotests.mobile.disk.data.DeviceFilesAndFolders
import ru.yandex.autotests.mobile.disk.data.FilesAndFolders
import java.util.concurrent.TimeUnit

@Feature("Authorization")
@UserTags("login")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@AuthorizationTest
class LoginTest : LoginDiskTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    @Named(LOGIN)
    lateinit var ruleChain: RuleChain

    @Inject
    @Named(TEST_USER)
    lateinit var account: Account

    @Inject
    @Named(BANNED_USER)
    lateinit var banned: Account

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("5025")
    @Category(Acceptance::class)
    fun shouldLoginToDiskFromExistedAccount() {
        onLogin {
            shouldLoginIntoApp(account) {
                shouldSeeTabs()
            }
        }
    }

    @Test
    @TmsLink("1367")
    @Category(Acceptance::class)
    fun shouldViewAccountListAfterLogout() {
        onLogin {
            shouldLoginIntoApp(account) {
                logout()
                wait(10, TimeUnit.SECONDS)
            }
            shouldSeeAccount(account.login)
        }
    }

    @Test
    @TmsLink("1349")
    @Category(Quarantine::class)
    fun shouldNotSeeDiskContentWhenLoginByBannedAccount() {
        onLogin {
            shouldLoginIntoApp(banned) {
                openFiles {
                    withBadCarma {
                        shouldSeeBadKarmaAlert()
                        pressCancelButtonOnBadKarmaAlert()
                    }
                    shouldUnableToFileListStubBePresented()
                }
            }
        }
    }

    @Test
    @TmsLink("3204")
    @Category(Quarantine::class)
    fun shouldNotSeeDiskContentWhenLoginByBannedAccountAfterRefresh() {
        onLogin {
            shouldLoginIntoApp(banned) {
                openFiles {
                    withBadCarma {
                        shouldSeeBadKarmaAlert()
                        pressRefreshButtonOnBadKarmaAlert()
                        shouldSeeBadKarmaAlert()
                    }
                }
            }
        }
    }

    @Test
    @TmsLink("1368")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    fun shouldLogoutIfOffline() {
        onLogin {
            shouldLoginIntoApp(account) {
                switchToAirplaneMode()
                logout()
            }
            shouldSeeAccount(account)
        }
    }

    @Test
    @TmsLink("1359")
    @Category(BusinessLogic::class)
    fun shouldNotAuthWithWrongPassword() {
        onLogin {
            enterLoginAndClickNext(account.login)
            enterPasswordAndClickNext("wrong_password")
            shouldSeeIncorrectPasswordMessage()
        }
    }

    @Test
    @TmsLink("1358")
    @Category(Regression::class)
    fun shouldNotAuthWithWrongLogin() {
        onLogin {
            enterLoginAndClickNext("wronglogin1")
            shouldSeeIncorrectLoginMessage()
        }
    }

    @Test
    @TmsLink("1360")
    @Category(Regression::class)
    fun shouldNotAuthInAirplaneMode() {
        onLogin {
            switchToAirplaneMode()

            enterLoginAndClickNext(account.login)
            shouldSeeInternetAbsentMessage()
            switchToData()
            wait(10, TimeUnit.SECONDS)
            shouldClickNextButton()
            waitForPassword()
            switchToAirplaneMode()
            enterPasswordAndClickNext(account.password)
            shouldSeeInternetAbsentMessage()
        }
    }

    @Test
    @TmsLink("1361")
    @Category(Regression::class)
    fun shouldNotAuthFromAccountListInAirplaneMode() {
        onLogin {
            shouldLoginIntoApp(account) {
                logout()
            }
            wait(10, TimeUnit.SECONDS)
            switchToAirplaneMode()
            shouldAutologinToAccount(account.login)
            shouldSeeToastWithMessage("No internet")
        }
    }

    @Test
    @TmsLink("4426")
    @Category(Regression::class)
    fun shouldNotAuthWithEmptyLogin() {
        onLogin {
            enterLoginAndClickNext("")
            shouldSeeEnterYourUsernameMessage()
        }
    }

    @Test
    @TmsLink("4427")
    @Category(Regression::class)
    fun shouldNotAuthWithEmptyPassword() {
        onLogin {
            enterLoginAndClickNext(account.login)
            enterPasswordAndClickNext("")
            shouldSeeEnterYourPasswordMessage()
        }
    }

    @Test
    @TmsLink("6264")
    @Category(BusinessLogic::class)
    @RequireOnboarding
    fun shouldAutouploadOnboardingDisplay() {
        onLogin {
            shouldLoginIntoApp(account)
            shouldDisplayPhotonUnlimWizard()
        }
    }

    @Test
    @TmsLink("6014")
    @Category(Regression::class)
    fun shouldAuthWithAccountFromList() {
        onLogin {
            shouldLoginIntoApp(account) {
                logout()
            }
            wait(10, TimeUnit.SECONDS)
            switchToAirplaneMode()
            shouldAutologinToAccount(account.login)
        }
    }

    @Test
    @TmsLink("7494")
    @Category(BusinessLogic::class)
    @PushFileToDevice(fileSpecs = [UploadFileSpec(type = UploadFileType.IMAGE, count = 30)], targetFolder = DeviceFilesAndFolders.DCIM_FULL_PATH)
    @DeleteFilesOnDevice(filesPath = [DeviceFilesAndFolders.DCIM_FULL_PATH])
    @DeleteFiles(files = [FilesAndFolders.CAMERA_UPLOADS])
    fun shouldLogoutIfAutouploadingInProgress() {
        onLogin {
            shouldLoginIntoApp(account) {
                switchToWifi()
                enablePhotoAutoupload()
                openPhotos {
                    shouldAutouploadingBeInprogress()
                }
                logout()
            }
        }
    }

    @Test
    @TmsLink("1362")
    @Category(BusinessLogic::class)
    fun shouldAppOpenAfterForceStop() {
        onLogin {
            shouldLoginIntoApp(account) {
                shouldBeOnFeed()
                withAdb {
                    forceStopApplication()
                    launchApp()
                }
                shouldBeOnFeed()
            }
        }
    }
}
