import MarketUITestMocks
import XCTest

final class SKUCardModelOpinionsTest: LocalMockTestCase {

    override func setUp() {
        super.setUp()
        setEmptyBanners()
    }

    func testThreeOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-950")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем отображение SKU когда есть ровно 3 отзыва")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        var sku: SKUPage!

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.three))
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем отзывы".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
        }

        "Проверяем количество ячеек с отзывами".ybm_run { _ in
            var prevOpinions: [String] = []

            func testOpinion() {
                let opinion = sku.opinion(after: prevOpinions)

                if !opinion.cell.isVisible {
                    sku.element.ybm_swipeCollectionView(
                        toFullyReveal: opinion.cell,
                        inset: sku.stickyViewInset
                    )
                }
                XCTAssertTrue(opinion.cell.isVisible)

                prevOpinions.append(opinion.cell.identifier)
            }

            for _ in 0 ..< 3 { testOpinion() }
            XCTAssertEqual(prevOpinions.count, 3)
        }

        "Проверяем отсутствие ячейки с кнопкой перехода на экран отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.disclaimer)
            XCTAssertFalse(sku.seeAllOpinionsButton.exists)
        }
    }

    func testMoreThanThreeOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-949")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем отображение SKU когда отзывов больше 3")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем SKU с большим количеством отзывов".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_Basic")
        }

        "Настраиваем стейт".ybm_run { _ in
            var defaultState = DefaultState()
            defaultState.setReviewsWithNotEmptySummary(
                with: FactorsState.productFactor,
                percentRecommend: FactorsState.percentRecommend
            )
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.moreThanThree))
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Тестируем заголовок секции отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
            sku.opinionsHeader.tap()

            XCTAssertTrue(sku.opinionsHeader.isVisible)
            XCTAssertEqual(sku.opinionsHeader.text, "Отзывы")

            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.recommendedRatio,
                inset: sku.stickyViewInset
            )
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.rating.rating)
            XCTAssertTrue(sku.rating.rating.isVisible)
            XCTAssertEqual(sku.rating.rating.label, "Рейтинг: 5.0 из 5")

            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.rating.grade)
            XCTAssertTrue(sku.rating.grade.isVisible)
            XCTAssertEqual(sku.rating.grade.label, "5.0")

            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.rating.gradeCount)
            XCTAssertTrue(sku.rating.gradeCount.isVisible)
            XCTAssertEqual(sku.rating.gradeCount.label, "25 отзывов 1\u{00a0}091 оценка")
        }

        "Тестируем статистику отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.recommendedRatio,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(
                sku.recommendedRatio.text,
                "\(FactorsState.recommendedRatio)% покупателей рекомендуют этот товар"
            )

            var prevFactTitles: [String] = []
            var prevFactGrades: [String] = []

            func testStatisticsRow(at index: Int, title: String) {
                let factGrade = sku.factGrade(after: prevFactGrades)
                sku.element.ybm_swipeCollectionView(
                    toFullyReveal: factGrade.target,
                    inset: sku.stickyViewInset
                )
                XCTAssertEqual(factGrade.target.text, "\(FactorsState.factorValue)")
                XCTAssertTrue(factGrade.target.isVisible)
                prevFactGrades.append(factGrade.cell.identifier)

                let factTitle = sku.factTitle(after: prevFactTitles)
                XCTAssertEqual(factTitle.target.text, title)
                XCTAssertTrue(factTitle.target.isVisible)
                prevFactTitles.append(factTitle.cell.identifier)

                let factProgress = sku.factProgresses(after: prevFactGrades)
                XCTAssertTrue(factProgress.target.isVisible)
                prevFactGrades.append(factProgress.cell.identifier)
            }

            FactorsState.factorTitles.enumerated().forEach(testStatisticsRow)
        }

        "Тестируем кнопку \"Написать отзыв\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.leaveOpinionButton,
                inset: sku.stickyViewInset
            )
            XCTAssertTrue(sku.leaveOpinionButton.exists)
            XCTAssertEqual(sku.leaveOpinionButton.label, "Написать отзыв")
        }

        "Проверяем популярные отзывы".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinion(after: []).cell,
                inset: sku.stickyViewInset
            )
        }

        "Проверяем кнопку \"Показать все отзывы\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.seeAllOpinionsButton,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.seeAllOpinionsButton.label, "Смотреть все отзывы")
        }
    }

    func testLessThanThreeOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-951")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем отображение SKU когда есть менее трех отзывов (но хотя бы один)")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем мало отзывов в SKU".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_FewOpinions")
        }

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем блок отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
            sku.opinionsHeader.tap()
        }

        "Проверяем количество ячеек с отзывами".ybm_run { _ in
            var prevOpinions: [String] = []

            func testOpinion() {
                let opinion = sku.opinion(after: prevOpinions)
                sku.element.ybm_swipeCollectionView(
                    toFullyReveal: opinion.target,
                    inset: sku.stickyViewInset
                )
                XCTAssertTrue(opinion.target.isVisible)
                prevOpinions.append(opinion.cell.identifier)
            }

            for _ in 0 ..< 1 { testOpinion() }
        }

        "Проверяем отсутствие ячейки с кнопкой перехода на экран отзывов".ybm_run { _ in
            XCTAssertFalse(sku.seeAllOpinionsButton.exists)
        }
    }

    func testNoOpinions() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-953")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем отображение SKU, когда нет отзывов, но есть рейтинг")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем мало отзывов в SKU".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NoOpinions")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.noReviews28Rating))
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Тестируем заголовок секции отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
            sku.opinionsHeader.tap()
            XCTAssertTrue(sku.opinionsHeader.isVisible)
            XCTAssertEqual(sku.opinionsHeader.text, "Отзывы")

            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.rating.element,
                inset: sku.stickyViewInset
            )

            XCTAssertTrue(sku.rating.rating.isVisible)
            XCTAssertEqual(sku.rating.rating.label, "Рейтинг: 5.0 из 5")

            XCTAssertTrue(sku.rating.grade.isVisible)
            XCTAssertEqual(sku.rating.grade.label, "5.0")

            XCTAssertTrue(sku.rating.gradeCount.isVisible)
            XCTAssertEqual(sku.rating.gradeCount.label, "Нет отзывов 28 оценок")

            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.noOpinions)
            XCTAssertEqual(
                sku.noOpinions.text,
                "Пока у этого товара нет отзывов, станьте первым! Помогите покупателям сделать правильный выбор."
            )
        }

        "Тестируем кнопку \"Написать отзыв\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.leaveOpinionButton,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.leaveOpinionButton.label, "Написать отзыв")
        }
    }

    func testNoOpinionsNoRating() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-952")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем отображение SKU, когда нет ни отзывов, ни рейтинга")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        "Мокаем мало отзывов в SKU".ybm_run { _ in
            mockStateManager?.pushState(bundleName: "SKUCardSet_NoOpinionsNoRating")
        }

        "Настраиваем стейт".ybm_run { _ in
            var skuState = SKUInfoState()
            skuState.setSkuInfoState(with: .custom(.noReviewNoRating))
            stateManager?.setState(newState: skuState)
        }

        var sku: SKUPage!

        "Открываем SKU".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Проверяем отображение рейтинга и количества отзывов".ybm_run { _ in
            wait(forVisibilityOf: sku.opinionsFastLink.element)

            XCTAssertTrue(sku.opinionsFastLink.opinionsCount.isVisible)
            XCTAssertEqual(sku.opinionsFastLink.opinionsCount.label, "Нет отзывов")

            XCTAssertTrue(sku.opinionsFastLink.rating.isVisible)
            XCTAssertEqual(sku.opinionsFastLink.rating.label, "Рейтинг: 0.0 из 5")
        }

        "Раскрываем блок отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.opinionsHeader,
                inset: sku.stickyViewInset
            )
            sku.opinionsHeader.tap()
        }

        "Тестируем кнопку \"Написать отзыв\"".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.leaveOpinionButton,
                inset: sku.stickyViewInset
            )
            XCTAssertEqual(sku.leaveOpinionButton.label, "Написать отзыв")
        }

        "Тестируем отсутствие рейтинга и звезд".ybm_run { _ in
            XCTAssertFalse(sku.rating.element.isVisible)
        }
    }

    func testRewiewPopUp() {
        Allure.addTestPalmLink("https://testpalm.yandex-team.ru/testcase/bluemarketapps-2774")
        Allure.addEpic("КМ")
        Allure.addFeature("Отзывы")
        Allure.addTitle("Проверяем, что появилась оценка товара")

        app.launchEnvironment[TestLaunchEnvironmentKeys.waitForExperiments] = String(true)

        var sku: SKUPage!

        var skuState = SKUInfoState()

        "Настраиваем стейт".ybm_run { _ in
            skuState.setSkuInfoState(with: .default)
            stateManager?.setState(newState: skuState)
        }

        "Открываем SKU с отзывами".ybm_run { _ in
            sku = goToDefaultSKUPage()
        }

        "Раскрываем блок отзывов".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(toFullyReveal: sku.opinionsHeader)
            sku.opinionsHeader.tap()
        }

        "Тестируем кнопку \"Написать отзыв\": появление оценки товара".ybm_run { _ in
            sku.element.ybm_swipeCollectionView(
                toFullyReveal: sku.leaveOpinionButton,
                inset: sku.stickyViewInset
            )

            wait(forVisibilityOf: sku.leaveOpinionButton)
            XCTAssertEqual(sku.leaveOpinionButton.label, "Написать отзыв")

            sku.leaveOpinionButton.tap()
            let opinionGrade = OpinionGradePage.current
            wait(forVisibilityOf: opinionGrade.element)
        }
    }

    private func setEmptyBanners() {
        var defaultState = DefaultState()

        "Убираем баннеры, чтобы не скроллить лишний раз".ybm_run { _ in
            defaultState.setBanners(banners: [])
            stateManager?.setState(newState: defaultState)
        }
    }
}

private extension SKUCardModelOpinionsTest {

    typealias ProductFactor = ResolveProductReviews.ProductFactor
    typealias PercentRecommend = ResolveProductReviews.PercentRecommend

    enum FactorsState {
        static let factorTitles = [
            "Количество дополнительных клавиш",
            "Качество сборки",
            "Простота подключения",
            "Удобство управления"
        ]
        static let factorValue = 4.8
        static let recommendedRatio = 95

        static let productFactor: [ProductFactor] = [
            ProductFactor(
                factorId: 23_423,
                value: factorValue,
                title: factorTitles[0],
                count: 4
            ),
            ProductFactor(
                factorId: 23_324,
                value: factorValue,
                title: factorTitles[1],
                count: 4
            ),
            ProductFactor(
                factorId: 23_423,
                value: factorValue,
                title: factorTitles[2],
                count: 4
            ),
            ProductFactor(
                factorId: 2_343,
                value: factorValue,
                title: factorTitles[3],
                count: 4
            )
        ]

        static let percentRecommend: [PercentRecommend] = [
            PercentRecommend(value: recommendedRatio)
        ]
    }
}

private extension CustomSKUConfig {

    static let skuConfig = CustomSKUConfig(
        productId: 1_728_622_897,
        skuId: 100_237_612_726,
        offerId: "7kDqm7y2vSA0YRwXq5bUVA"
    )

    static let three = modify(skuConfig) {
        $0.rating.rating = 5
        $0.rating.reviewsCount = 3
        $0.rating.overviewsCount = 25
        $0.rating.ratingCount = 1_091
        $0.rating.preciseRating = 5
    }

    static let moreThanThree = modify(skuConfig) {
        $0.rating.rating = 5
        $0.rating.reviewsCount = 25
        $0.rating.overviewsCount = 25
        $0.rating.ratingCount = 1_091
        $0.rating.preciseRating = 5
    }

    static let noReviews28Rating = modify(skuConfig) {
        $0.rating.rating = 5
        $0.rating.reviewsCount = 0
        $0.rating.overviewsCount = 0
        $0.rating.ratingCount = 28
        $0.rating.preciseRating = 5
    }

    static let noReviewNoRating: CustomSKUConfig = modify(skuConfig) {
        $0.rating.rating = 0
        $0.rating.reviewsCount = 0
        $0.rating.overviewsCount = 0
        $0.rating.ratingCount = 0
        $0.rating.preciseRating = 0
    }
}
