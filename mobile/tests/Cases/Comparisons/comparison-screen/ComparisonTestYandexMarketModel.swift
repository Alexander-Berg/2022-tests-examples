import XCTest

class ComparisonTestYandexMarketModel: ComparisonBaseTest {

    func testMarketPricesModel() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3555")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("КМ, которой нет на Беру")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_YMPModel")
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
            wait(
                forExistanceOf: comparison.collectionView
                    .cellUniqueElement(withIdentifier: ComparisonAccessibility.Model.root).firstMatch
            )
            XCTAssertEqual(models.count, 1)
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
    }

    func testShowOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-3557")
        Allure.addEpic("Сравнение")
        Allure.addFeature("Экран сравнения")
        Allure.addTitle("Переход в отзывы КМ, которой нет на Беру")

        var root: RootPage!
        var comparison: ComparisonPage!
        var comparisonList: ComparisonListPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "Comparisons_YMPModel")
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
            XCTAssertEqual(modelOpinions.count, 1)
            wait(forVisibilityOf: modelOpinions[0])
            modelOpinions[0].tap()

            let opinionsElement = XCUIApplication().otherElements[OpinionsAccessibility.container]
            ybm_wait(forVisibilityOf: [opinionsElement])
        }
    }

}
