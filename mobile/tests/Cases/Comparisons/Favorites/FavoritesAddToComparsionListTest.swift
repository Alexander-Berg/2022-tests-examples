import MarketUITestMocks
import XCTest

class FavoritesAddToComparsionListTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testAddAndRemoveFromComparsionList() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3544")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Избранное")
        Allure.addTitle("Удаление из избранного")

        var profile: ProfilePage!
        var wishlist: WishlistPage!
        var cell: FeedSnippetPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparsions_FavoritesAddToList")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .default,
                    collections: .default
                )
            )
            stateManager?.setState(newState: skuState)
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forExistanceOf: profile.wishlist.element)

            wishlist = profile.wishlist.tap()
            wait(forExistanceOf: wishlist.collectionView)
        }

        "Убираем товар из списка сравнения".ybm_run { _ in
            cell = wishlist.wishlistItem(at: 0)

            wait(forVisibilityOf: cell.comparsionButton)
            XCTAssertTrue(cell.comparsionButton.isSelected)
            cell.comparsionButton.tap()

            let popup = RemoveFromComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар удалён")

            popup.actionButton.tap()
        }

        "Проверяем, что товар вернулся".ybm_run { _ in
            let popup = AddToComparsionToastPopupPage.currentPopup
            wait(forExistanceOf: popup.element)
            XCTAssertEqual(popup.titleLabel.label, "Товар теперь в списке сравнения")

            XCTAssertTrue(cell.comparsionButton.isSelected)
        }
    }
}
