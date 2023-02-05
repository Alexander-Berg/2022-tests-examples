import MarketUITestMocks
import XCTest

final class SmartshoppingCardTokenTest: LocalMockTestCase {

    override var user: UserAuthState {
        .loginNoSubscription
    }

    func testCanOpenCatalogPage() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2800")
        Allure.addEpic("Купоны")
        Allure.addFeature("Карточка-токен")
        Allure.addTitle("Может перейти на страницу \"Каталог\"")

        var root: RootPage!
        var smartshoppingPage: SmartshoppingPage!

        "Открываем приложение, авторизуемся".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Переходим на страницу купонов".ybm_run { _ in
            smartshoppingPage = goToMyBonuses(root: root)
        }

        "Проверяем карусель купонов".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.element.isVisible
            })
        }

        "Проверяем карусель купонов".ybm_run { _ in
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.carouselView.element.isVisible
            })
        }

        "Скроллим и проверям название карточки".ybm_run { _ in
            smartshoppingPage.collectionView.swipe(to: .down, untilVisible: smartshoppingPage.tokenCard.element)
            ybm_wait(forFulfillmentOf: {
                smartshoppingPage.tokenCard.element.isVisible
                    && smartshoppingPage.tokenCard.detailsButton.element.isVisible

            })

            XCTAssertEqual(
                smartshoppingPage.tokenCard.title.label,
                "Хочу купон!"
            )
        }

        "Нажимаем на кнопку \"К покупкам\"".ybm_run { _ in
            let catalogPage = smartshoppingPage.tokenCard.detailsButton.tap()
            ybm_wait(forFulfillmentOf: {
                catalogPage.element.isVisible
            })
        }
    }
}
