import AutoMate
import MarketUITestMocks
import XCTest

final class SKUCardModelAllOpinionsGalleryTest: LocalMockTestCase {

    /*
     Объединила тесты на галерею фотографий в отзывах
     */
    func testFullScreenOpen() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3710")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3711")
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3712")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("КАРТОЧКА МОДЕЛИ. Отзывы. Галерея всех фото. Свайп")

        var sku: SKUPage!
        var opinions: OpinionsPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Переходим в отзывы".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsFastLink.element,
                inset: sku.stickyViewInset
            )
            opinions = sku.opinionsFastLink.tap()
            wait(forExistanceOf: opinions.element)
        }

        "Проверяем открытие и закрытие фуллскрин галлереи, свайп влево".ybm_run { _ in
            opinions.element.ybm_swipeCollectionView(
                toFullyReveal: opinions.allOpinionsGallery.element,
                inset: sku.stickyViewInset
            )

            let firstImage = opinions.allOpinionsGallery.cellPage(at: IndexPath(item: 1, section: 0))
            let openedGallery = firstImage.tap()

            wait(forExistanceOf: openedGallery.element)
            XCTAssertFalse(opinions.element.exists)

            openedGallery.closeButton.tap()
            wait(forExistanceOf: opinions.element)
            XCTAssertFalse(openedGallery.element.exists)

            wait(forExistanceOf: firstImage.element)

            opinions.allOpinionsGallery.collectionView.swipeLeft()

            wait(forInvisibilityOf: firstImage.element)
        }

        "Проверяем открытие и закрытие грида с фотками".ybm_run { _ in
            let grid = opinions.showAllPhotosButton.tap()
            wait(forExistanceOf: grid.element)
            XCTAssertFalse(opinions.element.exists)

            NavigationBarPage.current.backButton.tap()
            wait(forExistanceOf: opinions.element)
            XCTAssertFalse(grid.element.exists)
        }
    }
}
