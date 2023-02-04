import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuStockCard AutoRuComparison
final class ComplectationComparisonTest: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    private static let defaultComplectation = "Active"
    private static let pinnedComplectation = "Ambition"

    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        self.setupServer()
        self.launch()
    }

    func test_openComparisonFromModelInfo() {
        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
    }

    func test_openComparisonFromOptionsPicker() {
        self.openStockCard()
            .tapOnComplectationPicker()
            .tapOnComparisonButton()
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
    }

    func test_pinColumn() {
        self.openStockCard()
            .tapOnComplectationPicker()
            .tapOnComparisonButton()
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
            .checkScreenshotOfComparison(identifier: "complectation_comparison_default")
            .tapOnComplectationHeader(name: Self.pinnedComplectation)
            .checkScreenshotOfComparison(identifier: "complectation_comparison_pinned")
            .scrollLeft()
            .scrollLeft()
            .checkScreenshotOfComparison(identifier: "complectation_comparison_pinned_scrolled")
            .scrollUp()
            .wait(for: 1)
            .checkScreenshotOfHeader(identifier: "complectation_comparison_pinned_collapsed_header")
            .tapOnComplectationHeader(name: Self.pinnedComplectation)
            .scrollRight()
            .scrollRight()
            .scrollDown()
            .checkScreenshotOfComparison(identifier: "complectation_comparison_default")
    }

    func test_collapseGroup() {
        self.openStockCard()
            .tapOnComplectationPicker()
            .tapOnComparisonButton()
            .tapOnOptionSection(name: "Обзор")
            .checkScreenshotOfComparison(identifier: "complectation_comparison_collapsed_section")
            .tapOnOptionSection(name: "Обзор")
            .checkScreenshotOfComparison(identifier: "complectation_comparison_default")
    }

    func test_switchOnlyDifference() {
        self.openStockCard()
            .tapOnComplectationPicker()
            .tapOnComparisonButton()
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
            .checkScreenshotOfComparison(identifier: "complectation_comparison_default")
            .tapOnDifferenceSwitch()
            .checkScreenshotOfComparison(identifier: "complectation_comparison_only_difference")
    }

    func test_openDisclaimerPopupFromOptionsPicker() {
        let optionName = "Омыватель фар"
        let columnIndex = 0

        self.openStockCard()
            .tapOnComplectationPicker()
            .tapOnComparisonButton()
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
            .scrollTo(optionName: optionName, columnIndex: columnIndex)
            .tapOnOption(optionName: optionName, columnIndex: columnIndex)
            .shouldSeeDisclaimerPopup()
    }

    func test_openDisclaimerPopupFromModelInfo() {
        let optionName = "Электрообогрев лобового стекла"
        let columnIndex = 1

        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .shouldSeeComplectationHeader(name: Self.defaultComplectation)
            .scrollTo(optionName: optionName, columnIndex: columnIndex)
            .tapOnOption(optionName: optionName, columnIndex: columnIndex)
            .shouldSeeDisclaimerPopup()
    }

    func test_complectationWithoutPrices() {
        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .scrollLeft()
            .scrollLeft()
            .scrollLeft()
            .checkScreenshotOfComparison(identifier: "complectation_comparison_without_price")
    }

    func test_singleComplectation() {
        self.server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=20898233") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_subtree_skoda_octavia_single_complectation", userAuthorized: false)
        }

        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .checkScreenshotOfComparison(identifier: "complectation_comparison_single")
    }

    func test_individualComplectation() {
        self.server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=20898233") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_subtree_skoda_octavia_individual", userAuthorized: false)
        }

        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .checkScreenshotOfComparison(identifier: "complectation_comparison_individual")
    }

    func test_optionsWithDisclaimer() {
        let option1Name = "Диски 16"
        let column1Index = 1

        let option2Name = "Регулировка передних сидений по высоте"
        let column2Index = 1

        self.openStockCard()
            .tapOnAboutModel()
            .openComplectationTab()
            .as(ComplectationComparisonSteps.self)
            .scrollTo(optionName: option1Name, columnIndex: column1Index)
            .checkScreenshotOfOption(
                optionName: option1Name,
                columnIndex: column1Index,
                identifier: "complectation_comparison_additional_disclaimer"
            )
            .scrollTo(optionName: option2Name, columnIndex: column2Index)
            .checkScreenshotOfOption(
                optionName: option2Name,
                columnIndex: column2Index,
                identifier: "complectation_comparison_base_disclaimer"
            )
    }

    // MARK: - Private

    @discardableResult
    private func openStockCard() -> StockCardSteps {
        let stockOfferID = "1096244096-8efa2818"
        Step("Открываем стоковую карточку оффера '\(stockOfferID)'") {
            self.mainSteps
                .openFilters()
                .showResultsTap()
                .scrollToStockOffer(with: stockOfferID)
                .tapOnStockOffer(with: stockOfferID)
        }

        return self.mainSteps.as(StockCardSteps.self)
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_listing", userAuthorized: false)
        }

        server.addHandler("POST /search/cars?context=listing&group_by=CONFIGURATION&page=1&page_size=20&sort=fresh_relevance_1-desc") { (req, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_listing", userAuthorized: false)
        }

        server.addHandler("POST /search/cars?context=default&group_by=CONFIGURATION&page=1&page_size=1&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_stock_listing", userAuthorized: false)
        }

        server.addHandler("POST /search/cars?context=default&group_by=COMPLECTATION_NAME&page=1&page_size=100&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_complectation_stock_listing", userAuthorized: false)
        }

        server.addHandler("POST /search/cars?context=default&group_by=TECHPARAM&group_by=COMPLECTATION_NAME&page=1&page_size=100&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_model_info_search", userAuthorized: false)
        }

        server.addHandler("POST /search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_group_card_stock_listing", userAuthorized: false)
        }

        server.addHandler("GET /reference/catalog/cars/all-options") { (_, _) -> Response? in
            Response.okResponse(fileName: "catalog_all_options", userAuthorized: false)
        }

        server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=20898233") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_subtree_skoda_octavia", userAuthorized: false)
        }

        server.addHandler("POST /search/CARS/equipment-filters") { (_, _) -> Response? in
            Response.okResponse(fileName: "complectation_comparison_equipment_filters", userAuthorized: false)
        }
        try! server.start()
    }
}
