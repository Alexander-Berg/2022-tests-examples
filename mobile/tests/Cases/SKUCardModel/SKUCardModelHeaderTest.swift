import MarketUITestMocks
import XCTest

final class SKUCardModelHeaderTest: LocalMockTestCase {

    func testBaseBehavior() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-926")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем базовое поведение хедера")

        var sku: SKUPage!

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Проверяем кнопку \"Поделиться\"".ybm_run { _ in
            wait(forVisibilityOf: sku.navigationBar.shareButton)
        }

        "Проверяем кнопку добавления в вишлист".ybm_run { _ in
            wait(forVisibilityOf: sku.navigationBar.wishlistButton)
        }

        "Проверяем надпись в заголовке".ybm_run { _ in
            XCTAssertFalse(sku.navigationBar.title.isVisible)
        }
    }

    func testHeaderScrolling() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-927")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем, что хедер начинает отображать название товара при сролле вниз")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Ждем пока загрузится основная инфа об SKU".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.didFinishLoadingInfo
            })
        }

        "Cкроллим вниз и проверяем надпись в заголовке".ybm_run { _ in
            sku.element.swipe(to: .down, times: 1, until: false)
            XCTAssertEqual(sku.navigationBar.title.label, "Смартфон Apple iPhone 12 256GB, синий")
        }
    }

    func testAddToWishlistWhenUnauthorized() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2759")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем добавление в вишлист неавторизованным пользователем")

        var sku: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setAddWishlistItem(with: .default)
            stateManager?.setState(newState: wishlistState)
        }

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Проверяем кнопку добавления в вишлист".ybm_run { _ in
            let navigationBar = sku.navigationBar

            wait(forVisibilityOf: navigationBar.wishlistButton)
            XCTAssertFalse(navigationBar.wishlistButton.isSelected)

            navigationBar.wishlistButton.tap()

            ybm_wait(forFulfillmentOf: { () -> Bool in
                navigationBar.wishlistButton.isSelected
                    && self.didCallAPIMethod(urlToBeChecked: "addWishlistItem")
            })
        }
    }

    func testBackNavigation() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1034")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем кнопку back навигации")

        var sku: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Ждем появления хэдера, проверяем навигацию".ybm_run { _ in
            let navigationBar = sku.navigationBar

            wait(forVisibilityOf: navigationBar.element)
            XCTAssertTrue(navigationBar.backButton.isVisible)

            navigationBar.backButton.tap()

            wait(forVisibilityOf: MordaPage.current.element) // при успешном тапе видна морда
        }
    }

    func testShareClipboard() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1012")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем что при копировании из шер диалога копируется нужная ссылка")

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Шерим КМ копированием".ybm_run { _ in
            let share = sku.navigationBar.shareButton
            wait(forVisibilityOf: share)

            share.tap()

            let activityListView = ActivityListViewPage.current
            let copyButton = activityListView.copyButton
            wait(forExistanceOf: copyButton)
            copyButton.tap()
        }

        "Проверяем буфер обмена".ybm_run { _ in
            XCTAssertEqual(
                UIPasteboard.general.url?.absoluteString,
                "https://market.yandex.ru/product/722979017?cpa=1&sku=101077347763&offerid=KRAPKDZkKKimNA-4MVm-rA"
            )
        }
    }

    // MARK: - Private

    private func didCallAPIMethod(urlToBeChecked: String) -> Bool {
        mockServer?.handledRequests.contains { $0.contains(urlToBeChecked) } ?? false
    }

    private let skuId = "100324823773"
}
