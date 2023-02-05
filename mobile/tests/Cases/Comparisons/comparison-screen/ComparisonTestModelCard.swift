import XCTest

class ComparisonTestModelCard: ComparisonBaseTest {

    func testModelCardBehavour() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3500")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Один товар в списке")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: XCUIElementQuery!

        "Проверяем, что карточки видны".ybm_run { _ in
            models = comparison.collectionView.cellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
                .otherElements
            let model = models.firstMatch
            wait(forVisibilityOf: model)
            XCTAssertTrue(model.isVisible)
            let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
            let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
            XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
            let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
            XCTAssertTrue(name.element.isVisible)
            let trash = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete)
            let pin = model.buttons.matching(identifier: ComparisonAccessibility.Model.pin)
            XCTAssertTrue(trash.element.isVisible)
            XCTAssertTrue(pin.element.isVisible)
        }

        "Скроллим вниз и проверяем, что карточки видны".ybm_run { _ in
            let deleteCell = comparison.collectionView.collectionView.cells.buttons
                .matching(identifier: ComparisonAccessibility.deleteListButton)
            comparison.collectionView.collectionView.swipe(to: .down, until: deleteCell.element.isVisible)
            models = comparison.collectionView.cellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
                .otherElements
            let model = models.firstMatch
            XCTAssertTrue(model.isVisible)
            let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
            let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
            XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
            let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
            XCTAssertTrue(name.element.isVisible)
        }

        "Скроллим вправо-влево и проверяем, что карточки видны".ybm_run { _ in
            comparison.collectionView.collectionView.swipeRight()
            let model = models.firstMatch
            XCTAssertTrue(model.isVisible)
            let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
            let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
            XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
            let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
            XCTAssertTrue(name.element.isVisible)
            comparison.collectionView.collectionView.swipeLeft()
            XCTAssertTrue(model.firstMatch.isVisible)
            XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
            XCTAssertTrue(name.element.isVisible)
        }

    }

    func testModelCardBehavour2() {
        Allure.addTestPalmLink("https://testpalm2.yandex-team.ru/testcase/bluemarketapps-3532")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Два товара в списке сравнения")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_2Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: [XCUIElement] = []

        "Проверяем, что карточки видны".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 2)
            wait(forVisibilityOf: models[0])
            for model in models {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
                let trash = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete)
                let pin = model.buttons.matching(identifier: ComparisonAccessibility.Model.pin)
                XCTAssertTrue(trash.element.isVisible)
                XCTAssertTrue(pin.element.isVisible)
            }
        }

        "Скроллим вниз и проверяем, что карточки видны".ybm_run { _ in
            let deleteCell = comparison.collectionView.collectionView.cells.buttons
                .matching(identifier: ComparisonAccessibility.deleteListButton)
            comparison.collectionView.collectionView.swipe(to: .down, until: deleteCell.element.isVisible)
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 2)
            for model in models {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
        }

        "Скроллим вправо-влево и проверяем, что карточки видны".ybm_run { _ in
            comparison.collectionView.collectionView.swipeRight()
            XCTAssertEqual(models.count, 2)
            for model in models {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
            comparison.collectionView.collectionView.swipeLeft()
            XCTAssertEqual(models.count, 2)
            for model in models {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
        }

    }

    func testModelCardBehavour3() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3534")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Три и более товаров в списке")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_3Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: [XCUIElement] = []

        "Проверяем, что карточки видны".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 3)
            wait(forVisibilityOf: models[0])
            for model in models.prefix(2) {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
                let trash = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete)
                let pin = model.buttons.matching(identifier: ComparisonAccessibility.Model.pin)
                XCTAssertTrue(trash.element.isVisible)
                XCTAssertTrue(pin.element.isVisible)
            }
        }

        "Скроллим вниз и проверяем, что карточки видны".ybm_run { _ in
            let deleteCell = comparison.collectionView.collectionView.cells.buttons
                .matching(identifier: ComparisonAccessibility.deleteListButton)
            comparison.collectionView.collectionView.swipe(to: .down, until: deleteCell.element.isVisible)
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 3)
            for model in models.prefix(2) {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
        }

        "Скроллим вправо-влево и проверяем, что карточки видны".ybm_run { _ in
            comparison.collectionView.collectionView.swipeLeft()
            XCTAssertEqual(models.count, 3)
            for model in models.suffix(2) {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
            comparison.collectionView.collectionView.swipeRight()
            XCTAssertEqual(models.count, 3)
            for model in models.prefix(2) {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
                XCTAssertTrue(photo.element.isVisible || overflow.element.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.element.isVisible)
            }
        }

    }

    func testShowOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3539")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Переход в отзывы КМ")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_2Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        "Переходим в блок отзывов".ybm_run { _ in
            let modelOpinions = comparison.collectionView
                .allCellUniqueElement(withIdentifier: ComparisonAccessibility.Opinion.root)
            wait(
                forExistanceOf: comparison.collectionView
                    .cellUniqueElement(withIdentifier: ComparisonAccessibility.Opinion.root).firstMatch
            )
            XCTAssertEqual(modelOpinions.count, 2)
            wait(forVisibilityOf: modelOpinions[0])
            modelOpinions[0].tap()

            let opinionsElement = XCUIApplication().otherElements[OpinionsAccessibility.container]
            wait(forVisibilityOf: opinionsElement)
        }
    }

    func testOutOfStock() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3537")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Товара нет на Беру")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_5Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: [XCUIElement] = []

        "пиним 3 карточку".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            wait(forVisibilityOf: models[0])
            let pin = models[2].buttons.matching(identifier: ComparisonAccessibility.Model.pin)
            pin.element.tap()
        }

        "Проверяем, что товар виден".ybm_run { _ in
            comparison.collectionView.collectionView.swipeLeft()
            comparison.collectionView.collectionView.swipeLeft()
            comparison.collectionView.collectionView.swipeLeft()
            comparison.collectionView.collectionView.swipeRight()
            XCTAssertTrue(models[2].isVisible)
        }

        "Проверяем, что товар виден".ybm_run { _ in
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            XCTAssertTrue(models[2].isVisible)
        }

        "Проверяем, что карточка видна".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            let model = models[2]
            XCTAssertTrue(model.firstMatch.isVisible)
            let overflow = model.otherElements.matching(identifier: ComparisonAccessibility.Model.outOfStock)
            XCTAssertTrue(overflow.element.isVisible)
            let trash = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete)
            let pin = model.buttons.matching(identifier: ComparisonAccessibility.Model.pin)
            XCTAssertTrue(trash.element.isVisible)
            XCTAssertTrue(pin.element.isVisible)
        }
    }

    func testPin() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3540")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Запин")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_5Items")
        }

        "Запускаем приложение".ybm_run { _ in
            root = appAfterOnboardingAndPopups()
        }

        "Открываем экран списков сравнения".ybm_run { _ in
            comparisonList = goToComparison(root: root)
        }

        "Открываем экран сранения".ybm_run { _ in
            comparison = comparisonList.fistComparisonCell.tap()

            wait(forVisibilityOf: comparison.element)
        }

        var models: [XCUIElement] = []

        "пиним 3 карточку".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            wait(forVisibilityOf: models[0])
            comparison.collectionView.collectionView.swipeLeft()
            let pin = models[2].buttons.matching(identifier: ComparisonAccessibility.Model.pin)
            pin.element.tap()
        }

        "Проверяем, что товар виден".ybm_run { _ in
            comparison.collectionView.collectionView.swipeLeft()
            comparison.collectionView.collectionView.swipeLeft()
            comparison.collectionView.collectionView.swipeLeft()
            XCTAssertTrue(models[2].isVisible)
        }

        "Проверяем, что товар виден".ybm_run { _ in
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            comparison.collectionView.collectionView.swipeRight()
            XCTAssertTrue(models[2].isVisible)
        }

    }

}
