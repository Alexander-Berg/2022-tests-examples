import MarketUITestMocks
import XCTest

final class PushSubscriptionsTests: LocalMockTestCase {

    func testEnabledPushSubscriptions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5117")
        Allure.addEpic("Профиль")
        Allure.addFeature("Настройка пушей")
        Allure.addTitle("Проверяем переход по вкладке \"Уведомления\" при включенных пушах")

        setState()
        goToPushSettings()

        "Проверяем, что все подписки отображены".ybm_run { _ in
            let pushSubscriptionsPage = PushSubscriptionsPage.current
            wait(forVisibilityOf: pushSubscriptionsPage.switches.firstMatch)
            checkingThatTogglesTurnOn(with: pushSubscriptionsPage.switches)
        }
    }

    func testPushNotificationsLackOfInternet() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5127")
        Allure.addEpic("Профиль")
        Allure.addFeature("Настройка пушей")
        Allure.addTitle("Проверяем переход по вкладке \"Уведомления\" при возникновении ошибок")

        var pushSubscriptionsPage: PushSubscriptionsPage!

        goToPushSettings()

        "Видим ошибку. Обновляем".ybm_run { _ in
            pushSubscriptionsPage = PushSubscriptionsPage.current
            let barrierViewPage = pushSubscriptionsPage.errorView
            wait(forVisibilityOf: barrierViewPage.actionButton)
            barrierViewPage.actionButton.tap()
        }

        setState()

        "Видим, что обновилось успешно. Проверяем, что все подписки отображены".ybm_run { _ in
            wait(forVisibilityOf: pushSubscriptionsPage.switches.firstMatch)
            checkingThatTogglesTurnOn(with: pushSubscriptionsPage.switches)
        }
    }

    func testDisabledPushNotifications() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5116")
        Allure.addEpic("Профиль")
        Allure.addFeature("Настройка пушей")
        Allure.addTitle("Проверяем переход по вкладке \"Уведомления\" при выключенных пушах")

        app.launchEnvironment[TestLaunchEnvironmentKeys.isRemoteNotificationsAllowed] = String(false)

        setState()
        goToPushSettings()

        "Проверяем, что все тогглы активны. Есть кнопка с переходом в системные настройки".ybm_run { _ in
            let pushSubscriptionsPage = PushSubscriptionsPage.current
            ybm_wait(forVisibilityOf: [
                pushSubscriptionsPage.disabledSectionButton,
                pushSubscriptionsPage.switches.firstMatch
            ])
            for switcher in pushSubscriptionsPage.switches.allElementsBoundByIndex {
                XCTAssertTrue(switcher.isEnabled)
            }
        }
    }

    // MARK: - Private

    private func goToPushSettings() {
        "Переходим в настройку уведомлений".ybm_run { _ in
            let profile = goToProfile()
            wait(forVisibilityOf: profile.collectionView)
            profile.collectionView.ybm_swipeCollectionView(toFullyReveal: profile.pushNotifications.element)
            profile.pushNotifications.element.tap()
        }
    }

    private func setState() {
        "Настраиваем стейт".ybm_run { _ in
            var pushSubscriptionsState = PushSubscriptionsState()
            pushSubscriptionsState.setDefaultPushSubscriptionsItems()
            stateManager?.setState(newState: pushSubscriptionsState)
        }
    }

    private func checkingThatTogglesTurnOn(with switches: XCUIElementQuery) {
        for switcher in switches.allElementsBoundByIndex {
            XCTAssertTrue(switcher.isEnabled)
            XCTAssertEqual(switcher.value as? String, "1")
        }
    }

}
