import MarketUITestMocks
import XCTest

final class CashbackInYandexPlusTestCase: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testProductWithCashback() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3581")
        Allure.addEpic("КМ")
        Allure.addFeature("Кешбэк")
        Allure.addTitle("Бейдж на КМ")

        var root: RootPage!
        var skuPage: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.cashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.cashback.text)
            XCTAssertEqual(skuPage.cashback.text.label, " 28 баллов на Плюс")
        }

        "Ищем другие офферы".ybm_run { _ in
            skuPage.element.ybm_swipeCollectionView(
                toFullyReveal: skuPage.supplierInfo.element,
                inset: skuPage.stickyViewInset
            )
            XCTAssertEqual(
                skuPage.alternativeOffers.generic.cashback.label.trimmingCharacters(in: .whitespacesAndNewlines),
                "﻿﻿ 28 баллов на Плюс"
            )
        }
    }

    func testProductWithMastercardAndRegularCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5002")
        Allure.addEpic("MasterCard + обычный кешбэк")
        Allure.addFeature("Плюсовик")
        Allure.addTitle("Бейдж на КМ")

        var root: RootPage!
        var skuPage: SKUPage!
        var cashback: SKUPage.CashbackItem!

        enable(toggles: FeatureNames.mastercard_cashback_2021)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.mastercardAndGeneralCashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            cashback = skuPage.cashback
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: cashback.text)
            XCTAssertEqual(cashback.text.label, " 28 и ещё  100 с Mastercard")
        }

        "Нажимаем на кешбэк и проверяем открытие условий акции".ybm_run { _ in
            cashback.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testProductWithMastercardCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5003")
        Allure.addEpic("MasterCard кешбэк")
        Allure.addFeature("Плюсовик")
        Allure.addTitle("Бейдж на КМ")

        var root: RootPage!
        var skuPage: SKUPage!
        var cashback: SKUPage.CashbackItem!

        enable(toggles: FeatureNames.mastercard_cashback_2021)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.mastercardCashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            cashback = skuPage.cashback
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: cashback.text)
            XCTAssertEqual(cashback.text.label, " 100 с Mastercard")
        }

        "Нажимаем на кешбэк и проверяем открытие условий акции".ybm_run { _ in
            cashback.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testProductWithMirAndRegularCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6470")
        Allure.addEpic("МИР + обычный кешбэк")
        Allure.addFeature("Плюсовик")
        Allure.addTitle("Бейдж на КМ")

        var root: RootPage!
        var skuPage: SKUPage!
        var cashback: SKUPage.CashbackItem!

        enable(toggles: FeatureNames.mastercard_cashback_2021)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.mirAndGeneralCashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            cashback = skuPage.cashback
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: cashback.text)
            XCTAssertEqual(cashback.text.label, " 28 и ещё  100 с картой «Мир»")
        }

        "Нажимаем на кешбэк и проверяем открытие условий акции".ybm_run { _ in
            cashback.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testProductWithMirCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6471")
        Allure.addEpic("МИР кешбэк")
        Allure.addFeature("Плюсовик")
        Allure.addTitle("Бейдж на КМ")

        var root: RootPage!
        var skuPage: SKUPage!
        var cashback: SKUPage.CashbackItem!

        enable(toggles: FeatureNames.mastercard_cashback_2021)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.mirCashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            cashback = skuPage.cashback
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: cashback.text)
            XCTAssertEqual(cashback.text.label, " 100 с картой «Мир»")
        }

        "Нажимаем на кешбэк и проверяем открытие условий акции".ybm_run { _ in
            cashback.element.tap()
            let conditionsPopup = CashbackAboutPage.current
            wait(forExistanceOf: conditionsPopup.element)
        }
    }

    func testProductWithMirCashbackLimitExceeded() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/6473")
        Allure.addEpic("МИР кешбэк")
        Allure.addFeature("Плюсовик")
        Allure.addTitle("КМ после достижения лимита по акции")

        var root: RootPage!
        var skuPage: SKUPage!

        enable(toggles: FeatureNames.mastercard_cashback_2021)

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.cashback))
            stateManager?.setState(newState: skuState)
        }

        "Открываем приложение и авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Проверка наличия кешбэка на КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage(root: root)
            skuPage.element.ybm_swipeCollectionView(toFullyReveal: skuPage.cashback.text)
            XCTAssertEqual(skuPage.cashback.text.label, " 28 баллов на Плюс")
        }
    }

    func testCashbackBalanceToastFullCover() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4185")
        Allure.addEpic("КМ")
        Allure.addFeature("Плюсовик с баллами")
        Allure.addTitle("Тост про накопленные баллы, полное покрытие стоимости товара")

        testCashbackBalanceToast(
            bundleName: "CashbackBalanceToastFullCover",
            titleLabel: "Купите этот товар за баллы"
        )
    }

    func testCashbackBalanceToastPartialCover() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4772")
        Allure.addEpic("КМ")
        Allure.addFeature("Плюсовик с баллами")
        Allure.addTitle("Тост про накопленные баллы, частичное покрытие стоимости товара")

        testCashbackBalanceToast(
            bundleName: "CashbackBalanceToastPartialCover",
            titleLabel: "Спишите баллы при покупке"
        )
    }

    private var toggleInfo: String {
        let name = FeatureNames.plusBenefits.lowercased()
        let toggleAdditionalInfo = [
            name: [
                "spendCashbackInfoAllowed": true
            ]
        ]
        guard let toggleInfosData = try? JSONSerialization.data(
            withJSONObject: toggleAdditionalInfo,
            options: .prettyPrinted
        )
        else {
            return ""
        }
        return String(data: toggleInfosData, encoding: .utf8) ?? ""
    }

    private func testCashbackBalanceToast(bundleName: String, titleLabel: String) {
        enable(toggles: FeatureNames.plusBenefits)
        app.launchEnvironment[TestLaunchEnvironmentKeys.enabledTogglesInfo] = toggleInfo

        var root: RootPage!
        var skuPage: SKUPage!
        var onboarding: PlusOnboardingPage!

        setState()

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

            XCTAssertEqual(toast.titleLabel.label, titleLabel)
            XCTAssertEqual(toast.detailsLabel.label, "У вас 1 458 баллов ")

            toast.element.tap()
        }

        "Ждем открытия онбординга, проверяем текст".ybm_run { _ in
            onboarding = PlusOnboardingPage.current
            wait(forExistanceOf: onboarding.element)

            XCTAssertEqual(onboarding.title.label, "Ура, вы накопили 1 458 баллов")

            onboarding.element.swipeDown()
        }
    }

    private func setState() {
        "Настраиваем стейт".ybm_run { _ in
            var authState = UserAuthState()
            authState.setPlusBalanceState(.withBalance(1_458))
            stateManager?.setState(newState: authState)
        }
    }
}

private extension CustomSKUConfig {
    static var cashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithCashback("28")]
        config.alternativeOfferIds = ["bNolttGchovLo4iQr7Q6cA"]
        return config
    }

    static var mastercardCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithPaymentSystemCashback("100")]
        config.promoInfoByTag = [.mastercard]
        return config
    }

    static var mirCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithPaymentSystemCashback("100")]
        config.promoInfoByTag = [.mir]
        return config
    }

    static var mastercardAndGeneralCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithCashback("28"), .promoWithPaymentSystemCashback("100")]
        config.promoInfoByTag = [.mastercard]
        return config
    }

    static var mirAndGeneralCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithCashback("28"), .promoWithPaymentSystemCashback("100")]
        config.promoInfoByTag = [.mir]
        return config
    }

}
