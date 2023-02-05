import MarketUITestMocks
import UIUtils
import XCTest

final class WishlistNotificationTestWithoutYandexPlus: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testPlusNotificationIsVisible() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3787")
        Allure.addEpic("Вишлист")
        Allure.addFeature("Плюс")
        Allure.addTitle("Проверяем, что на экране отображается нотификейшн Плюса")

        var profile: ProfilePage!
        var wishlist: WishlistPage!

        enable(
            toggles:
            FeatureNames.plusBenefits,
            FeatureNames.showPlus
        )

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "WishlistPlusNotification")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoProductOffersWithHyperIdState(
                with: .init(
                    results: .several(SKUFAPIResults.default, SKUFAPIResults.protein),
                    collections: .several(.default, .protein)
                )
            )
            stateManager?.setState(newState: skuState)

            var wishlistState = WishlistState()
            wishlistState.setWishlistItems(items: [.default, .protein])
            stateManager?.setState(newState: wishlistState)

            var userAuthState = UserAuthState()
            userAuthState.setPlusBalanceState(.withMarketCashback_5)
            stateManager?.setState(newState: userAuthState)
        }

        "Запускаем приложение и авторизуемся".ybm_run { _ in
            profile = goToProfile()
        }

        "Переходим в вишлист".ybm_run { _ in
            wait(forVisibilityOf: profile.wishlist.element)
            wishlist = profile.wishlist.tap()
        }

        "Проверяем наличие блока Плюса и переходим на него".ybm_run { _ in
            let notification = wishlist.notificationCell
            wait(forVisibilityOf: notification.element)

            let popup = notification.tap()
            wait(forVisibilityOf: popup.element)
        }
    }
}
