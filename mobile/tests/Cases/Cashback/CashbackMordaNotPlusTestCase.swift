import MarketUITestMocks
import XCTest

final class CashbackMordaNotPlusTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCashbackBadgeWithoutScores() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3793")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4040")
        Allure.addEpic("Кешбэк")
        Allure.addFeature("Кешбэк для неплюсовика")
        Allure.addTitle("Отображение Бейджа Плюса на Морде")

        enable(toggles: FeatureNames.showPlus)

        var root: RootPage!
        var morda: MordaPage!

        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withZeroMarketCashback)
            stateManager?.setState(newState: authState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем морду".ybm_run { _ in
            morda = goToMorda(root: root)
        }

        "Проверяем переход в Дом Плюса".ybm_run { _ in
            wait(forExistanceOf: morda.plusButton.element)

            morda.plusButton.element.tap()

            wait(forExistanceOf: HomePlusPage.current.element)
        }
    }

}
