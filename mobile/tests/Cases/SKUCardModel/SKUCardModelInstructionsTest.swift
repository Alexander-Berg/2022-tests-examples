import MarketUITestMocks
import XCTest

final class SKUCardModelInstructionsTest: LocalMockTestCase {

    func testShouldShowSKUSpecsOldSplit() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4540")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем наличие раздела характеристики на КТ")

        var sku: SKUPage!
        var specs: SpecsPage!

        disable(toggles: FeatureNames.newInstruction)

        "Настраиваем стейт".ybm_run { _ in
            setupSKUInfoState(mapper: .pharma)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок характеристик".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader
            )
            XCTAssertEqual(sku.specsHeader.text, "Характеристики")
        }

        "Тапаем по кнопке подробнее".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsDetailsButton.element
            )
            specs = sku.specsDetailsButton.tap()
        }

        "Проверяем открытие экрана характеристик, наличие надписи 'Общие характеристики'".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { specs.element.isVisible })
            XCTAssertTrue(specs.titlesBlocks.contains { $0.staticTexts.element.label == "Общие характеристики" })
        }
    }

    func testShouldShowSKUMedicineSpecs() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4417")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем наличие медицинских разделов в 'коротко о товаре'")

        var sku: SKUPage!

        enable(toggles: FeatureNames.newInstruction)

        "Настраиваем стейт".ybm_run { _ in
            setupSKUInfoState(mapper: .pharma)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок коротко о товаре".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader
            )
            XCTAssertEqual(sku.specsHeader.text, "Коротко о товаре")
        }

        "Проверяем наличие спецификаций".ybm_run { _ in
            XCTAssertNotNil(sku.specLabel(withName: "Тип препарата"))
            XCTAssertNotNil(sku.specLabel(withName: "Действующее вещество"))
            XCTAssertNotNil(sku.specLabel(withName: "Назначение"))
            XCTAssertNotNil(sku.specLabel(withName: "Страна - производитель"))
        }
    }

    func testShouldNotShowInstructionsInNonMedicineSKU() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4418")
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4425")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем отсутвие раздела инструкция в не медецинских товарах")

        var sku: SKUPage!
        var specs: SpecsPage!

        enable(toggles: FeatureNames.newInstruction)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Basic")
        }

        "Настраиваем стейт".run {
            setupSKUInfoState(mapper: .custom(Constants.withSpecs))
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок характеристик".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader
            )
            XCTAssertEqual(sku.specsHeader.text, "Характеристики")
        }

        "Тапаем по кнопке подробнее".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsDetailsButton.element
            )
            specs = sku.specsDetailsButton.tap()
        }

        "Проверяем открытие экрана характеристик, наличие надписи 'Общие характеристики'".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { specs.element.isVisible })
            XCTAssertTrue(specs.titlesBlocks.contains { $0.staticTexts.element.label == "Общие характеристики" })
        }

        "Возвращаемся в карточку товара".ybm_run { _ in
            specs.navigationBar.backButton.tap()
            ybm_wait(forFulfillmentOf: { sku.element.isVisible })
        }

        "Проверяем отсутствие в карточке товара раздела 'Инструкция'".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                to: .up,
                toFullyReveal: sku.specsHeader
            )
            XCTAssertNotEqual(sku.specsHeader.text, "Инструкция")
        }
    }

    func testShouldShowInstructionInMedicineSKU() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4419")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем наличие раздела инструкция и кнопки полная инструкция")

        var sku: SKUPage!
        var instructions: InstructionsPage!

        enable(toggles: FeatureNames.newInstruction)

        "Настраиваем стейт".ybm_run { _ in
            setupSKUInfoState(mapper: .pharmaCms)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок инструкция".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.instructionHeader
            )

            XCTAssertEqual(sku.instructionHeader.text, "Инструкция")
        }

        "Проверяем наличие кнопки полная инструкция".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.instructionDetailsButton.element
            )

            XCTAssertEqual(sku.instructionDetailsButton.element.label, "Полная инструкция")
        }

        "Проверяем что открывается экран инструкций".ybm_run { _ in
            instructions = sku.instructionDetailsButton.tap()
            ybm_wait(forFulfillmentOf: { instructions.element.isVisible })
        }
    }

    func testShouldShowInstructionInMedicineSKUShort() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4424")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем наличие раздела инструкция")

        var sku: SKUPage!

        enable(toggles: FeatureNames.newInstruction)

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState(mapper: .pharmaCms)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок инструкция".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.instructionHeader
            )
            XCTAssertEqual(sku.instructionHeader.text, "Инструкция")
        }
    }

    func testShouldShowBriefSectionInMedicineSKU() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-4426")
        Allure.addEpic("КМ")
        Allure.addFeature("Новая инструкция")
        Allure.addTitle("Проверяем наличие раздела коротко о товаре")

        var sku: SKUPage!

        enable(toggles: FeatureNames.newInstruction)

        "Настраиваем стейт".ybm_run { _ in
            setupSKUInfoState(mapper: .pharma)
        }

        "Открываем карточку товара".ybm_run { _ in
            sku = goToDefaultSKUPage()
            wait(forVisibilityOf: sku.element)
        }

        "Проверяем заголовок коротко о товаре".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader
            )
            XCTAssertEqual(sku.specsHeader.text, "Коротко о товаре")
        }
    }

}

// MARK: - Helper Methods

private extension SKUCardModelInstructionsTest {

    func setupSKUInfoState(mapper: SKUInfoState.SKUInfoMapper) {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: mapper)
        stateManager?.setState(newState: skuState)
    }
}

// MARK: - Nested Types

private extension SKUCardModelInstructionsTest {

    enum Constants {
        static let withSpecs = modify(CustomSKUConfig.default) { $0.specs = .pharma }
    }
}
