import MarketUITestMocks
import XCTest

final class SKUCardModelHeaderAuthTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testHeaderInWishlistAppearance() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-931")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем, что кнопка добавления вишлиста отображается выделенной")

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_InWishlist")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            let config = CustomSKUConfig(
                productId: 13_621_355,
                skuId: Int(skuId),
                offerId: "J6zCIjgXkqgppGtMDBpsrQ"
            )
            skuState.setSkuInfoState(with: .custom(config))
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем экран SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
            ybm_wait(forFulfillmentOf: { () -> Bool in
                sku.didFinishLoadingInfo
            })
        }

        "Проверяем кнопку добавления в вишлист".ybm_run { _ in
            let navigationBar = sku.navigationBar

            wait(forVisibilityOf: navigationBar.wishlistButton)
            XCTAssertTrue(navigationBar.wishlistButton.isVisible)
            XCTAssertTrue(navigationBar.wishlistButton.isSelected)
        }
    }

    func testAddToWishList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1003")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем, что кнопка добавления вишлиста нажимается при добавлении, появляется анимация")

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
            open(market: .sku(skuId: "101077347763"))
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

    func testDeleteFromWishList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1003")
        Allure.addEpic("КМ")
        Allure.addFeature("Хедер")
        Allure.addTitle("Проверяем удаление из вишлиста")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_InWishlist")
        }

        "Открываем экран SKU c добавленным в вишлист товаром".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_DeleteFromWishList")
        }

        "Проверяем удаление из вишлиста".ybm_run { _ in
            let navigationBar = sku.navigationBar

            wait(forVisibilityOf: navigationBar.wishlistButton)
            XCTAssertTrue(navigationBar.wishlistButton.isSelected) // товар уже добавлен в вишлист

            navigationBar.wishlistButton.tap() // удаление из вишлиста

            ybm_wait(forFulfillmentOf: { () -> Bool in
                !navigationBar.wishlistButton.isSelected
                    && self.didCallAPIMethod(urlToBeChecked: "deleteWishlistItem")
            })
        }
    }

    // MARK: - Private

    private func didCallAPIMethod(urlToBeChecked: String) -> Bool {
        mockServer?.handledRequests.contains { $0.contains(urlToBeChecked) } ?? false
    }

    private let skuId = "100324823773"
}
