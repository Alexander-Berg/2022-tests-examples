import MarketUITestMocks
import XCTest

class RetailPopupTest: RetailTestCase {

    typealias PopupPage = RetailInformerPopupPage

    func testAddToCart() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-5923")
        Allure.addEpic("КМ")
        Allure.addFeature("Ритейл")
        Allure.addTitle("Попап")

        var feedPage: FeedPage!
        var skuPage: SKUPage!
        var popupPage: PopupPage!

        "Мокаем состояние".ybm_run { _ in
            setupFeed()
            setupSku()
            setupUserAddress()
        }

        "Открываем выдачу".ybm_run { _ in
            feedPage = goToFeed(with: "iphone")
        }

        "Переходим на КМ".ybm_run { _ in
            let firstSnippet = feedPage.collectionView.cellPage(at: 0)
            skuPage = firstSnippet.tap()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                skuPage.didFinishLoadingInfo
            })
        }

        "Добавлям в корзину".run {
            let button = skuPage.addToCartButton.element
            skuPage.collectionView.ybm_swipeCollectionView(to: .down, toFullyReveal: button)
            button.tap()
            popupPage = PopupPage.currentPopup
            ybm_wait(forVisibilityOf: [popupPage.informerShopTitle])
        }

        "Проверяем попап".run {
            XCTAssertEqual(
                popupPage.informerShopTitle.label,
                "Продолжайте покупки в магазине  Яндекс.Маркет"
            )
        }

    }

    // MARK: - Private

    func setupUserAddress() {
        var userState = UserAuthState()
        userState.setContactsState(contacts: [.basic])
        userState.setAddressesState(addresses: [.default])
        stateManager?.setState(newState: userState)
    }

}
