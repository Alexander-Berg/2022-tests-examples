import MarketUITestMocks
import XCTest

final class SKUCardModelSpecsTest: LocalMockTestCase {

    struct SpecsCell {
        let title: String
        let description: String
    }

    func testShouldShowSKUSpecs() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-933")
        Allure.addEpic("КМ")
        Allure.addFeature("Характеристики")
        Allure.addTitle("Проверяем товар с характеристиками (больше одной секции) без описания")

        var sku: SKUPage!

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.manySpecs))
            stateManager?.setState(newState: skuState)
        }

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Basic")
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем заголовок характеристик".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.specsHeader.text, "Характеристики")
        }

        "Проверяем количество ячеек характеристик".ybm_run { _ in
            var prevSpecs: [String] = []
            func testSpec() {
                let spec = sku.spec(after: prevSpecs)
                sku.element.ybm_swipeCollectionView(
                    toFullyReveal: spec.target,
                    inset: sku.stickyViewInset
                )
                prevSpecs.append(spec.cell.identifier)
            }

            for _ in 0 ..< 7 { testSpec() }
        }

        "Проверяем наличие кнопки \"Подрбнее\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsDetailsButton.element,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(sku.specsDetailsButton.element.isVisible)
            XCTAssertEqual(sku.specsDetailsButton.element.label, "Подробнее")
        }
    }

    func testShouldNotShowSpesButtonWhenTooFewSpecs() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-936")
        Allure.addEpic("КМ")
        Allure.addFeature("Характеристики")
        Allure
            .addTitle("Проверяем, что кнопка \"Показать все характеристики\" не отображается, если характеристик мало")

        var sku: SKUPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FewSpecs")
        }

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.fewScpecs))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем, что кнопка \"Показать все характеристики\" не отобразилась".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsHeader,
                inset: sku.stickyViewInset
            )
            XCTAssertFalse(sku.specsDetailsButton.element.exists)
        }
    }

    func testShouldShowDescripton() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-934")
        Allure.addEpic("КМ")
        Allure.addFeature("Характеристики")
        Allure.addTitle("Проверяем отображение и расширение по тапу блока с описанием")

        var sku: SKUPage!

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Disclaimer")
        }

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.fewScpecs))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        "Проверяем заголовок блока описания".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.descriptionHeader,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.descriptionHeader.text, "Описание")
        }

        "Проверяем отображение блока описания".ybm_run { _ in
            XCTAssertTrue(sku.descriptionHeader.isVisible)
        }
    }

    func testShowAllSpecs() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-991")
        Allure.addEpic("КМ")
        Allure.addFeature("Характеристики")
        Allure.addTitle("Проверяем кнопку \"Подробнее\"")

        var sku: SKUPage!

        "Мокаем состояние".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_ShowAllSpecs")
        }

        "Настраиваем стейт".run {
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.manySpecs))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Ждем завершения загрузки информации".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { sku.didFinishLoadingInfo })
        }

        "Проверяем отображение кнопки \"Подробнее\" на КМ под характеристиками".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsDetailsButton.element,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(sku.specsDetailsButton.element.isVisible)
            XCTAssertEqual(sku.specsDetailsButton.element.label, "Подробнее")
        }

        "Проверяем переход по тапу и экран полных характеристик, переход обратно на КМ в то же место".ybm_run { _ in
            let specsPage = sku.specsDetailsButton.tap()

            ybm_wait(forFulfillmentOf: {
                !sku.element.isVisible
                    && NavigationBarPage.current.title.label == "Характеристики"
            })

            ybm_wait { specsPage.titlesBlocks.count == 2 } // количество блоков характеристик
            NavigationBarPage.current.backButton.tap() // уходим обратно на КМ

            // остались в том же месте после возвращения на КМ
            wait(forVisibilityOf: sku.element)
        }
    }

    func testWebview() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-1022")
        Allure.addEpic("КМ")
        Allure.addFeature("Характеристики")
        Allure.addTitle("Переход в webview")

        var sku: SKUPage!
        var specsPage: SpecsPage!

        "Мокаем webview".ybm_run { _ in
            let url = (app.launchEnvironment[TestLaunchEnvironmentKeys.capiUrl] ?? "") + "/webview"
            app.launchEnvironment[TestLaunchEnvironmentKeys.webViewPagesUrl] = url
        }

        "Мокаем состояние".ybm_run { _ in
            setupSKUInfoState()
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Тапаем на кнопку характеристик".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.specsDetailsButton.element,
                inset: sku.stickyViewInset
            )
            specsPage = sku.specsDetailsButton.tap()
        }

        "Проверяем страницу характеристик, наличие ссылки на Ростест, работу ссылок".ybm_run { _ in
            ybm_wait(forFulfillmentOf: { specsPage.element.isVisible })

            // ячейка с информацией про Роскачество
            let roskachestvo = specsPage.specsCellEntries.first {
                $0.textViews.firstMatch.text == "Оценка Роскачества"
            }!

            specsPage.element.ybm_swipeCollectionView(toFullyReveal: roskachestvo)

            roskachestvo.tap()

            // достаю линки по лейблу
            let iphoneLink = roskachestvo.links["3.639"].firstMatch
            let generalLink = roskachestvo.links["Роскачества"].firstMatch

            "Мокаем 1 веб страницу".ybm_run { _ in
                mockStateManager?.pushState(bundleName: "SKUCardSet_testWebview_roskachestvo")
            }

            openLinkAndCloseWebview(
                generalLink,
                expectedTitle: "Российская система качества (Роскачество) | Cайт АНО «Российская система качества»"
            )

            self.ybm_wait { iphoneLink.isHittable }

            "Мокаем 2 веб страницу".ybm_run { _ in
                mockStateManager?.pushState(bundleName: "SKUCardSet_testWebview_iphone")
            }

            openLinkAndCloseWebview(
                iphoneLink,
                expectedTitle: "Смартфон Apple iPhone 7 (32 GB) | Исследование товара от Роскачества"
            )

            self.ybm_wait { specsPage.element.isVisible }
        }
    }

    private func generateSpecsCells() -> [SpecsCell] {
        let specsCells = [
            SpecsCell(title: "Тип контроллера", description: "беспроводной геймпад"),
            SpecsCell(title: "Тип поддерживаемого API", description: "DirectInput / XInput"),
            SpecsCell(title: "Виброотдача", description: "есть"),
            SpecsCell(title: "Совместимость", description: "ПК, PS4"),
            SpecsCell(title: "Акселерометр", description: "есть"),
            SpecsCell(title: "Гироскоп", description: "есть"),
            SpecsCell(title: "Разъем для стереогарнитуры", description: "есть"),
            SpecsCell(title: "Страна производства", description: "Китай")
        ]

        return specsCells
    }
}

private extension CustomSKUConfig {
    static let fewScpecs = modify(CustomSKUConfig.default) { $0.specs = .few }
    static let manySpecs = modify(CustomSKUConfig.default) { $0.specs = .pharma }
}

// MARK: - Helper Methods

private extension SKUCardModelSpecsTest {

    func setupSKUInfoState() {
        var skuState = SKUInfoState()
        skuState.setSkuInfoState(with: .phone)
        stateManager?.setState(newState: skuState)
    }
}
