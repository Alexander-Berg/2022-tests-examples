import MarketUITestMocks
import XCTest

final class SKUCardModelGalleryTest: LocalMockTestCase {

    func testGallerySwipe() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1049")
        Allure.addEpic("КМ")
        Allure.addFeature("Галерея")
        Allure.addTitle("Проверяем свайп")

        var sku: SKUPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        func swipeTest<Indexes: BidirectionalCollection>(
            photoGallery: SKUPage.Gallery,
            inRange range: Indexes,
            forDirection swipe: () -> Void
        ) where Indexes.Element == Int {
            for el in range {
                "Проверяем фотографию в галерее #\(el) на свайп и PageIndicator".ybm_run { _ in
                    let cell = photoGallery.cellPage(at: IndexPath(item: el, section: 0))

                    wait(forVisibilityOf: cell.element)

                    wait(forVisibilityOf: photoGallery.pageControl)

                    XCTAssertEqual(photoGallery.pageControl.text, "\(el) страница из \(range.count)")

                    swipe()
                    if range.last == el {
                        // долистали до конца
                        XCTAssertTrue(cell.element.isVisible)
                    } else {
                        // свайп работает корректно и фотография не видна
                        XCTAssertFalse(cell.element.isVisible)
                    }
                }
            }
        }

        "Открываем SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "На КМ отображается фотогалерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        let arrLeftSwipe = Array(0 ... 5)
        let arrRightSwipe = arrLeftSwipe.reversed()

        "Свайпаем до конца влево".ybm_run { _ in
            swipeTest(photoGallery: sku.gallery, inRange: arrLeftSwipe) {
                sku.gallery.collectionView.swipeLeft()
            }
        }

        "Свайпаем до конца вправо".ybm_run { _ in
            swipeTest(photoGallery: sku.gallery, inRange: arrRightSwipe) {
                sku.gallery.collectionView.swipeRight()
            }
        }
    }

    func testFullPhotoSwipeAndClose() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1050")
        Allure.addEpic("КМ")
        Allure.addFeature("Галерея")
        Allure.addTitle("Проверяем свайп фотографий, открытых на полный экран")

        var sku: SKUPage!
        var openedGallery: SKUPage.OpenedGallery!

        func swipeTestFrame<Indexes: BidirectionalCollection>(
            photoGallery: SKUPage.OpenedGallery,
            inRange range: Indexes,
            forDirection swipe: () -> Void
        ) where Indexes.Element == Int {
            for el in range {
                "Проверяем фотографию в галерее #\(el) на свайп и PageIndicator".ybm_run { _ in
                    let cell = photoGallery.image

                    wait(forExistanceOf: cell)
                    wait(forVisibilityOf: photoGallery.pageControl)

                    XCTAssertEqual(photoGallery.pageControl.text, "\(el) страница из \(range.count)")

                    let frameBeforeSwipe = cell.frame

                    swipe()

                    let frameAfterSwipe = cell.frame
                    if range.last == el {
                        // долистали до конца (фотография не меняет фрейм)
                        XCTAssertEqual(frameBeforeSwipe, frameAfterSwipe)
                    } else {
                        // свайп работает корректно и фотография меняет фрейм
                        XCTAssertNotEqual(frameBeforeSwipe, frameAfterSwipe)
                    }
                }
            }
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "На КМ отображается фотогалерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "Открываем фотогалерею на весь экран".ybm_run { _ in
            let firstImage = sku.gallery.cellPage(at: IndexPath(item: 0, section: 0))
            let imageFrameBeforeTap = firstImage.element.frame

            // already checked for existance
            openedGallery = firstImage.tap()

            XCTAssertTrue(openedGallery.element.isVisible)
            XCTAssertTrue(openedGallery.image.exists)

            XCTAssertNotEqual(imageFrameBeforeTap, openedGallery.image.frame)

            XCTAssertTrue(openedGallery.pageControl.isVisible)
        }

        let arrLeftSwipe = Array(0 ... 5)
        let arrRightSwipe = arrLeftSwipe.reversed()

        "Листаем влево".ybm_run { _ in
            swipeTestFrame(photoGallery: openedGallery, inRange: arrLeftSwipe) {
                openedGallery.element.swipeLeft()
            }
        }

        "Листаем вправо".ybm_run { _ in
            swipeTestFrame(photoGallery: openedGallery, inRange: arrRightSwipe) {
                openedGallery.element.swipeRight()
            }
        }

        "Закрываем галерею: Фото закрылось. Отображается КМ".ybm_run { _ in
            XCTAssertTrue(openedGallery.element.isVisible)
            XCTAssertFalse(sku.element.isVisible)

            wait(forVisibilityOf: openedGallery.closeButton)
            openedGallery.closeButton.tap()

            wait(forVisibilityOf: sku.element)
            XCTAssertFalse(openedGallery.element.isVisible)
        }
    }

    func testZoomPhoto() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1051")
        Allure.addEpic("КМ")
        Allure.addFeature("Галерея")
        Allure.addTitle("Проверяем зум фотографии, открытой на полный экран")

        var sku: SKUPage!
        var openedGallery: SKUPage.OpenedGallery!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            _ = appAfterOnboardingAndPopups()
            open(market: .sku(skuId: skuId))
            sku = SKUPage.current
        }

        "На КМ отображается фотогалерея товара".ybm_run { _ in
            wait(forVisibilityOf: sku.gallery.element)
        }

        "Открываем фотогалерею на весь экран".ybm_run { _ in
            let firstImage = sku.gallery.cellPage(at: IndexPath(item: 0, section: 0))
            openedGallery = firstImage.tap()
        }

        "Проверяем двойной тап: фото увеличилось, фото уменьшилось обратно".ybm_run { _ in
            // изначальный фрейм фотографии
            let imageCellFrameBefore2Tap = openedGallery.image.frame

            // приближаем двойным тапом
            openedGallery.element.doubleTap()

            // после тапа фрейм поменялся и стал больше
            ybm_wait {
                imageCellFrameBefore2Tap.height < openedGallery.image.frame.height &&
                    imageCellFrameBefore2Tap.width < openedGallery.image.frame.width
            }

            // отдаляем фото двойным тапом
            openedGallery.element.doubleTap()

            // после двух двойных тапов фрейм не изменился
            ybm_wait {
                imageCellFrameBefore2Tap.height == openedGallery.image.frame.height &&
                    imageCellFrameBefore2Tap.width == openedGallery.image.frame.width
            }
        }
    }

    // MARK: - Constants

    private let skuId = "100235860377"
}
