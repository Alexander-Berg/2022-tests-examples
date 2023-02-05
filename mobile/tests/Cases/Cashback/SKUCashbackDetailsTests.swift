import MarketUITestMocks
import XCTest

final class SKUCashbackDetailsTests: LocalMockTestCase {

    override var user: UserAuthState {
        .loginWithYandexPlus
    }

    func testDefaultCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5605")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("КМ")
        Allure.addTitle("Стандартный кешбэк")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.defaultCashback))
            stateManager?.setState(newState: skuState)

            var сashbackCMSState = CMSState()
            сashbackCMSState.setCMSState(with: CMSState.CMSCollections.defaultCashbackCollections)
            stateManager?.setState(
                newState: сashbackCMSState,
                matchedBy: hasStringInBody("\"type\":\"mp_cashback_description_app\"")
            )
        }

        checkWithAboutPopup(
            cashbackText: " 100 баллов на Плюс",
            popupContent: CashbackAboutPopupContent(
                title: "Кешбэк баллами",
                text: "Вам вернётся до 3 000 баллов.",
                buttonTitle: "Все акции с кешбэком"
            )
        )
    }

    func testExtraCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5606")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("КМ")
        Allure.addTitle("Повышенный кешбэк")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.extraCashback))
            stateManager?.setState(newState: skuState)

            var сashbackCMSState = CMSState()
            сashbackCMSState.setCMSState(with: CMSState.CMSCollections.extraCashbackCollections)
            stateManager?.setState(
                newState: сashbackCMSState,
                matchedBy: hasStringInBody("\"type\":\"mp_cashback_description_app\"")
            )
        }

        checkWithAboutPopup(
            cashbackText: " 300 баллов — повышенный кешбэк",
            popupContent: CashbackAboutPopupContent(
                title: "Повышенный кешбэк баллами",
                text: "Вам вернётся до 3 000 баллов с повышенным кешбэком. Акции с повышенным кешбэком действуют на 7 заказов и ограничены по времени.",
                buttonTitle: "Все акции с кешбэком"
            )
        )
    }

    func testPartnerExtraCashback() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5607")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("КМ")
        Allure.addTitle("Повышенный кешбэк от продавца")

        enable(toggles: FeatureNames.cashbackDetailsButton)

        "Мокаем состояние".ybm_run { _ in
            stateManager?.mockingStrategy = .dtoMock

            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.partnerExtraCashback))
            stateManager?.setState(newState: skuState)

            var сashbackCMSState = CMSState()
            сashbackCMSState.setCMSState(with: CMSState.CMSCollections.partnerExtraCashbackCollections)
            stateManager?.setState(
                newState: сashbackCMSState,
                matchedBy: hasStringInBody("\"type\":\"mp_cashback_description_app\"")
            )
        }

        checkWithAboutPopup(
            cashbackText: " 200 баллов — повышенный кешбэк",
            popupContent: CashbackAboutPopupContent(
                title: "Партнерский кешбэк баллами",
                text: "Вам вернётся до 3 000 баллов с кешбэком от продавца. Акции с повышенным кешбэком действуют на 7 заказов и ограничены по времени.",
                buttonTitle: "Все акции с кешбэком"
            )
        )
    }

    func testDetailsCashback_defaultAndPaymentSystem() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5608")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("КМ")
        Allure.addTitle("Cтандартный кешбэк + кешбэк по платежной системе (детализация)")

        enable(
            toggles:
            FeatureNames.cashbackDetailsButton,
            FeatureNames.mastercard_cashback_2021
        )

        "Мокаем состояние".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.defaultAndMastercardCashback))
            stateManager?.setState(newState: skuState)
        }

        checkWithDetailsPopup(
            cashbackText: " 100 и ещё  150 с картой «Мир»",
            popupContent: CashbackDetailsPopupContent(
                title: "Вернётся на Плюс",
                cashbackItems: [
                    ("Стандартный кешбэк", "Attachment.png, Файл 100"),
                    ("Повышенный кешбэк от Mastercard", "Attachment.png, Файл 150")
                ],
                buttonTitle: "Подробнее об акциях"
            )
        )
    }

    func testDetailsCashback_extraAndPaymentSystem() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/bluemarketapps/testcases/5609")
        Allure.addEpic("Детализация кешбэка")
        Allure.addFeature("КМ")
        Allure.addTitle("Повышенный кешбэк + кешбэк по платежной системе (детализация)")

        enable(
            toggles:
            FeatureNames.cashbackDetailsButton,
            FeatureNames.mastercard_cashback_2021
        )

        "Мокаем состояние".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.extraAndMastercardCashback))
            stateManager?.setState(newState: skuState)
        }

        checkWithDetailsPopup(
            cashbackText: " 300 и ещё  150 с картой «Мир»",
            popupContent: CashbackDetailsPopupContent(
                title: "Вернётся на Плюс",
                cashbackItems: [
                    ("Повышенный кешбэк", "Attachment.png, Файл 300"),
                    ("Повышенный кешбэк от Mastercard", "Attachment.png, Файл 150")
                ],
                buttonTitle: "Подробнее об акциях"
            )
        )
    }

    // MARK: - Private

    private func checkWithAboutPopup(cashbackText: String, popupContent: CashbackAboutPopupContent) {
        var skuPage: SKUPage!
        var popupPage: CashbackAboutPage!

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Нажимаем на кешбэк".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.cashback.element)
            XCTAssertEqual(skuPage.cashback.text.label, cashbackText)
            skuPage.cashback.element.tap()
        }

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, popupContent.title)
            XCTAssertEqual(popupPage.descriptionText(at: 0).label, popupContent.text)
            XCTAssertEqual(popupPage.linkButton.label, popupContent.buttonTitle)
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

    private func checkWithDetailsPopup(cashbackText: String, popupContent: CashbackDetailsPopupContent) {
        var skuPage: SKUPage!
        var popupPage: CashbackDetailsAboutPage!

        "Открываем КМ".ybm_run { _ in
            skuPage = goToDefaultSKUPage()
        }

        "Нажимаем на кешбэк".ybm_run { _ in
            skuPage.collectionView.ybm_swipeCollectionView(toFullyReveal: skuPage.cashback.element)
            XCTAssertEqual(skuPage.cashback.text.label, cashbackText)
            skuPage.cashback.element.tap()
        }

        "Проверяем попап".ybm_run { _ in
            popupPage = CashbackDetailsAboutPage.current
            wait(forVisibilityOf: popupPage.element)
            XCTAssertEqual(popupPage.title.label, popupContent.title)

            for (index, cashbackItem) in popupContent.cashbackItems.enumerated() {
                let detailsItem = popupPage.detailsItem(at: index)
                XCTAssertEqual(detailsItem.title.label, cashbackItem.0)
                XCTAssertEqual(detailsItem.value.label, cashbackItem.1)
            }

            XCTAssertEqual(popupPage.linkButton.label, popupContent.buttonTitle)
            XCTAssertEqual(popupPage.closeButton.label, "Понятно")
            popupPage.linkButton.tap()
        }

        "Проверяем открытие вебвью".ybm_run { _ in
            wait(forVisibilityOf: WebViewPage.current.element)
        }
    }

}

private extension SKUCashbackDetailsTests {

    struct CashbackAboutPopupContent {
        var title: String
        var text: String
        var buttonTitle: String
    }

    struct CashbackDetailsPopupContent {
        var title: String
        var cashbackItems: [(String, String)]
        var buttonTitle: String
    }

}

private extension CustomSKUConfig {
    static var defaultCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithCashback("100")]
        config.cashbackDetails = [.defaultCashback]
        return config
    }

    static var extraCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithExtraCashback("300")]
        config.cashbackDetails = [.extraCashback]
        return config
    }

    static var partnerExtraCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithExtraCashback("200")]
        config.cashbackDetails = [.partnerExtraCashback]

        return config
    }

    static var defaultAndMastercardCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithCashback("100"), .promoWithPaymentSystemCashback("150")]
        config.promoInfoByTag = [.mir]
        config.cashbackDetails = [.defaultCashback, .mastercardExtraCashback]
        return config
    }

    static var extraAndMastercardCashback: CustomSKUConfig {
        var config = CustomSKUConfig(
            productId: 662_551_134,
            skuId: 100_917_481_793,
            offerId: "Zn9sxFl8lf_U9hJHVYK4Sg"
        )
        config.aggregatePromo = [.promoWithExtraCashback("300"), .promoWithPaymentSystemCashback("150")]
        config.promoInfoByTag = [.mir]
        config.cashbackDetails = [.extraCashback, .mastercardExtraCashback]
        return config
    }
}
