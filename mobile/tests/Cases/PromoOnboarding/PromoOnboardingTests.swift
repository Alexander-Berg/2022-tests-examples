import MarketUITestMocks
import XCTest

final class PromoOnboardingTests: LocalMockTestCase {

    // MARK: - Public

    func testOnboardingPopupCrossButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5770")
        Allure.addEpic("Онбординг")
        Allure.addFeature("M онбординг")
        Allure.addTitle("Проверяем закрытие попапа по кнопке крестику")

        let onboardingPage = makeOnboardingShown()

        "Закрываем онбординг по крестику".ybm_run { _ in
            onboardingPage.cross.tap()
            wait(forVisibilityOf: MordaPage.current.element)
            XCTAssertFalse(onboardingPage.element.isVisible)
        }
    }

    func testOnboardingPopupSkipButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5771")
        Allure.addEpic("Онбординг")
        Allure.addFeature("M онбординг")
        Allure.addTitle("Проверяем закрытие попапа по экшин кнопке")

        let onboardingPage = makeOnboardingShown()

        "Закрываем онбординг по нажатию на экшин кнопку".ybm_run { _ in
            onboardingPage.actionButotn.tap()
            wait(forVisibilityOf: MordaPage.current.element)
            XCTAssertFalse(onboardingPage.element.isVisible)
        }
    }

    func testOnboardingPopupLinkButton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5771")
        Allure.addEpic("Онбординг")
        Allure.addFeature("M онбординг")
        Allure.addTitle("Проверяем переход по диплинку по экшин кнопке")

        let onboardingPage = makeOnboardingShown(with: .link)

        "Переходим по диплинку по экшин кнопке".ybm_run { _ in
            onboardingPage.actionButotn.tap()
            wait(forVisibilityOf: TabBarPage.current.catalogPage.element)
        }
    }

    // MARK: - Private

    private func makeOnboardingShown(
        with type: PromoOnboardingState.ButtonType = .skip
    ) -> PromoOnboardingPage {
        var onboardingPage: PromoOnboardingPage!

        setState(with: type)

        "Открываем морду и ждем появления онбрдинга".ybm_run { _ in
            appAfterOnboardingAndPopups()
            onboardingPage = PromoOnboardingPage.currentPopup
            wait(forVisibilityOf: onboardingPage.element)
        }

        return onboardingPage
    }

    private func setState(with type: PromoOnboardingState.ButtonType) {
        "Настраиваем стейты".ybm_run { _ in
            var state = PromoOnboardingState()
            state.setMediumOnboarding(with: type)
            stateManager?.setState(newState: state)
        }
    }
}
