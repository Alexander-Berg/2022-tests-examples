import XCTest

class ComparisonTestDeleteItem: ComparisonBaseTest {

    func testDeleteNotSingleItem() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3535")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Удалить товар из списка сравнения")

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
        }

        "Удаляем элемент".ybm_run { _ in
            let model = models[2]
            let button = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete).firstMatch
            button.tap()
        }

        "Проверяем, что осталось 2 элемента".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 2)
        }

        "Проверяем, что показался тост удаления".ybm_run { _ in
            wait(forVisibilityOf: comparison.deleteToast)
            ybm_wait { !comparison.deleteToast.isVisible }
        }

        "Скроллим вправо-влево и проверяем, что карточки видны".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            for model in models {
                comparison.collectionView.collectionView.ybm_swipeCollectionView(to: .right, toFullyReveal: model)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                XCTAssertTrue(photo.firstMatch.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.firstMatch.isVisible)
            }
            for model in models {
                comparison.collectionView.collectionView.ybm_swipeCollectionView(to: .left, toFullyReveal: model)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                XCTAssertTrue(photo.firstMatch.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.firstMatch.isVisible)
            }
        }

        "Скроллим вниз и проверяем, что карточки видны".ybm_run { _ in
            let deleteCell = comparison.collectionView.collectionView.cells.buttons
                .matching(identifier: ComparisonAccessibility.deleteListButton)
            comparison.collectionView.collectionView.ybm_swipeCollectionView(toFullyReveal: deleteCell.element)
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 2)
            for model in models {
                XCTAssertTrue(model.firstMatch.isVisible)
                let photo = model.images.matching(identifier: ComparisonAccessibility.Model.photo)
                XCTAssertTrue(photo.firstMatch.isVisible)
                let name = model.staticTexts.matching(identifier: ComparisonAccessibility.Model.title)
                XCTAssertTrue(name.firstMatch.isVisible)
            }
        }

    }

    func testDeleteAndRestoreNotSingleItem() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3546")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Поп-ап удаления")

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
        }

        var deletedElementTitle: String?
        "Удаляем элемент".ybm_run { _ in
            let model = models[1]
            deletedElementTitle = model.staticTexts.firstMatch.title
            let button = model.buttons.matching(identifier: ComparisonAccessibility.Model.delete).firstMatch
            button.tap()
        }

        "Проверяем, что осталось 2 элемента".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 2)
        }

        "Проверяем, что показался тост удаления и нажимаем Восстановить".ybm_run { _ in
            let toast = comparison.deleteToast
            wait(forVisibilityOf: toast)
            toast.buttons.firstMatch.tap()
        }

        "Проверяем, что теперь снова 2 элемента".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(models.count, 3)
        }

        "Проверяем, что удаленный встал именно на 2 место".ybm_run { _ in
            models = comparison.collectionView.allCellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root)
            XCTAssertEqual(deletedElementTitle, models[1].staticTexts.firstMatch.title)
        }

    }

}
