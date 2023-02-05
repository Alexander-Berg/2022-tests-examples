import XCTest

final class DSBSOfferCardTests: LocalMockTestCase {

    func testOfferCardInformation() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4139")
        Allure.addEpic("DSBS. КО.")

        var root: RootPage!
        var card: SKUPage!

        mockStateManager?.pushState(bundleName: "DSBSOfferCard")

        "Открываем корзину и переходим в каталог".ybm_run { _ in

            root = appAfterOnboardingAndPopups()

            let feed = goToFeed(root: root, with: "Смартфон Samsung Galaxy A71 128 ГБ серебристый")
            wait(forVisibilityOf: feed.element)

            let snippet = feed.collectionView.cellPage(at: 0)
            feed.element.ybm_swipeCollectionView(toFullyReveal: snippet.imageView)

            card = snippet.tap()
            wait(forExistanceOf: card.element)
        }

        "Проверяем отсутствие кнопок добавления в избранное, сравнение".ybm_run { _ in
            XCTAssertFalse(card.navigationBar.comparisonButton.exists)
            XCTAssertTrue(card.navigationBar.shareButton.exists)
            XCTAssertFalse(card.navigationBar.wishlistButton.exists)
        }

        "Есть фото".ybm_run { _ in
            XCTAssertTrue(card.gallery.element.exists)
            XCTAssertTrue(card.title.exists)
            XCTAssertTrue(card.deliveryOptions.pickup.element.exists)
            XCTAssertTrue(card.deliveryOptions.service.element.exists)

            card.collectionView.swipe(to: .down, until: card.addToCartButton.element.isVisible)

            XCTAssertTrue(card.addToCartButton.element.exists)
            XCTAssertEqual(card.deliveryPartner.label, "Доставка продавца сайт.рф ")
        }

        "Есть ссылка на другие товары в категории".ybm_run { _ in
            card.collectionView.swipe(to: .down, until: card.alternativeOffers.element.isVisible)
            wait(forVisibilityOf: card.alternativeOffers.element)
            XCTAssertTrue(card.alternativeOffers.element.exists)
        }

        "Есть блок описание и характеристики".ybm_run { _ in
            card.collectionView.swipe(to: .down, until: card.description.isVisible)
            wait(forVisibilityOf: card.description)
            XCTAssertTrue(card.description.exists)
        }

        "Есть дисклеймер \"Информация о технических характеристиках ...\"".ybm_run { _ in
            card.collectionView.swipe(to: .down, until: card.disclaimer.isVisible)
            wait(forVisibilityOf: card.disclaimer)
            XCTAssertTrue(card.disclaimer.exists)
        }

    }
}
