import MarketUITestMocks
import XCTest

final class CashbackSKUWithoutYandexPlusTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testProductWithCashbackWithoutYandexPlus() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4202")
        Allure.addEpic("КМ")
        Allure.addFeature("Кешбэк для неплюсовика")
        Allure.addTitle("Бейдж Плюса на КМ")

        var root: RootPage!

        setState(with: 0)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "CashbackBadgeWithoutYandexPlus")
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переход на КМ, проверка наличия бейджа Плюса на КМ".ybm_run { _ in
            let skuPage = goToDefaultSKUPage(root: root)

            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.cashback.text)
            XCTAssertEqual(skuPage.cashback.text.label, " 1 410 баллов на Плюс")
        }
    }

    func testCashbackBalanceToastFullCover() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4186")
        Allure.addEpic("КМ")
        Allure.addFeature("Неплюсовик с баллами")
        Allure.addTitle("Тост про накопленные баллы у неплюсовика, предложение полного покрытия стоимости товара")

        testCashbackBalanceToast(
            bundleName: "CashbackBalanceToastFullCoverWithoutYandexPlus",
            titleLabel: "Купите этот товар за баллы"
        )
    }

    func testCashbackBalanceToastPartialCover() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4994")
        Allure.addEpic("КМ")
        Allure.addFeature("Неплюсовик с баллами")
        Allure.addTitle("Тост про накопленные баллы у неплюсовика, предложение частичного покрытия стоимости товара")

        testCashbackBalanceToast(
            bundleName: "CashbackBalanceToastPartialCoverWithoutYandexPlus",
            titleLabel: "Спишите баллы при покупке"
        )
    }

    private func testCashbackBalanceToast(bundleName: String, titleLabel: String) {
        enable(
            toggles:
            FeatureNames.plusBenefits,
            FeatureNames.showPlus
        )

        var root: RootPage!
        var skuPage: SKUPage!
        var onboarding: PlusOnboardingPage!

        setState(with: 1_561)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: bundleName)
        }

        "Открываем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переход на товар".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
        }

        "Проверка тоста, переход на онбординг Плюса".ybm_run { _ in
            skuPage.element.swipeUp()

            let toast = BuyForCashbackPopupPage.currentPopup
            wait(forVisibilityOf: toast.element)

            XCTAssertEqual(toast.titleLabel.label, titleLabel)
            XCTAssertEqual(
                toast.detailsLabel.label,
                "У вас 1 561 балл "
            )

            toast.element.tap()
        }

        "Ждем открытия онбординга, проверяем текст".ybm_run { _ in
            onboarding = PlusOnboardingPage.current
            wait(forExistanceOf: onboarding.element)

            XCTAssertEqual(onboarding.title.label, "Ура, вы накопили 1 561 балл")

            onboarding.element.swipeDown()
        }
    }

    private func setState(with balance: Int) {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withBalance(balance))
            stateManager?.setState(newState: authState)
        }
    }
}
