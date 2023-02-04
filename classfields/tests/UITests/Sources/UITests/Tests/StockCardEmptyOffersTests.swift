import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuStockCard
final class StockCardEmptyOffersTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    func test_openStockCardEmptyResultsWithoutFilters() {
        self.setupServer(mockFolder: "StockCardEmptyOffers")
        self.launch()

        Step("Проверяем, что открыли стоковую карточку без офферов: есть плейсхолдер и нельзя сбросить фильтры") { }

        self.openFavorites()
            .tapSegment(at: .searches)
            .tapSavedSearch(id: "0e74a10ebc2d150f596428bea5a6d02b6af244fa", index: 0)
            .as(StockCardSteps.self)
            .shouldSeeEmptyResultsPlaceholder()
            .checkEmptyResultsResetButton(isVisible: false)
    }

    func test_openStockCardEmptyResultsWithFilter() {
        self.setupServer(mockFolder: "StockCardEmptyOffersResetFilters")
        self.launch()

        Step("Проверяем, что открыли стоковую карточку с отфильтрованной пустой выдачей: есть плейсхолдер и сброс фильтров") { }

        self.openFavorites()
            .tapSegment(at: .searches)
            .tapSavedSearch(id: "9952d36969da91c4edaf10edc724c043b4a8f3c9", index: 0)
            .as(StockCardSteps.self)
            .shouldSeeEmptyResultsPlaceholder()
            .checkEmptyResultsResetButton(isVisible: true)
            .tapEmptyResultsResetButton()
            .shouldSeeOffer(with: "1101550717-ef8933bc")
    }

    // MARK: - Private

    private func openFavorites() -> FavoritesSteps {
        Step("Открываем Избранное") {
            self.mainSteps
                .openTab(.favorites)
        }

        return self.mainSteps.as(FavoritesSteps.self)
    }

    private func setupServer(mockFolder: String) {
        server.forceLoginMode = .forceLoggedIn

        self.advancedMockReproducer.setup(server: server, mockFolderName: mockFolder)

        try! server.start()
    }
}
