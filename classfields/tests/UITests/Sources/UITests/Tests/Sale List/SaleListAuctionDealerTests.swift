import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleList
final class SaleListAuctionDealerTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()

        mocker
            .mock_user()
            .mock_base()
            .mock_searchHistory(
                state: .new,
                accelerationFrom: 123
            )
            .mock_searchCars(
                newCount: 2,
                usedCount: 0,
                isSalon: true,
                dealerAuctions: [
                    0: .init(offersCount: 10)
                ]
            )
            .mock_salon(id: "20135241")
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_snippetWithAuctionPrice() {
        self.launch()

        Step("Проверяем сниппет со ссылкой на дилера и что она ведет в листинге дилера") { }

        let expectation = expectationForRequest { req -> Bool in
            if req.method == "POST", req.uri.starts(with: "/search/cars"), let messageBody = req.messageBody {
                let body = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: messageBody)

                return body.accelerationFrom == 123
                    && body.carsParams.bodyTypeGroup == [.allroad5Doors]
                    && body.catalogFilter.count == 1
                    && body.catalogFilter[0].mark == "VAZ"
                    && body.catalogFilter[0].model == "2131_4X4"
                    && body.catalogFilter[0].generation == 21762355
                    && !body.catalogFilter[0].hasConfiguration
            }

            return true
        }

        let steps = self.mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .checkStockSnippetBodySnapshot(offerId: "0", identifier: "stock_snippet_auction_body")
            .tapOnStockSnippetAuctionLink()

        wait(for: [expectation], timeout: 1.0)

        steps.descriptionTitleExists()
            .checkDealerName("LADA КорсГрупп Тула")
    }

    func test_snippetWithoutAuctionPrice() {
        self.launch()

        Step("Проверяем сниппет без ссылки на дилера") { }

        self.mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .checkStockSnippetBodySnapshot(offerId: "1", identifier: "stock_snippet_without_auction_body")
    }

    // MARK: - Private

}
