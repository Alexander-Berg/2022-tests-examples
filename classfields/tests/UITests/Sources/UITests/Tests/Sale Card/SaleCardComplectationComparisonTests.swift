import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuSaleCard AutoRuComparison
final class SaleCardComplectationComparisonTests: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openComplectationComparisonFromNewCarSaleCard() {
        let catalogExpectation = api.reference.catalog.cars.configurations.subtree
            .get(parameters: .parameters([.configurationId(["21398651"])]))
            .expect()

        let optionsExpectation = api.reference.catalog.cars.allOptions
            .get
            .expect()
 
        let searchExpectation = api.search.cars
            .post(
                parameters: [
                    .context("default"),
                    .groupBy(["TECHPARAM", "COMPLECTATION_NAME"]),
                    .page(1),
                    .pageSize(100),
                    .sort("fresh_relevance_1-desc")
                ]
            )
            .expect { req, _ in
                (try? req.jsonUTF8Data()).flatMap { Self.validateSearchRequest(data: $0) } ?? .skip
            }

        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098230510-dd311329")))
            .scrollToCompectation()
            .tapOnCompectationComparisonLink()

        self.wait(for: [catalogExpectation, optionsExpectation, searchExpectation], timeout: Self.requestTimeout)

        steps.checkScreenshotOfComparison(identifier: "complectation_comparison_bmw")
    }

    // MARK: - Helpers

    private static func validateSearchRequest(data: Data) -> ExpectationCheckerVerdict {
        guard let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any] else {
            return .fail(reason: nil)
        }

        if json.count != 8 {
            return .skip
        }

        guard json["hasImage"] as? Bool == false,
           json["inStock"] as? String == "ANY_STOCK",
           json["rid"] as? [Int] == [225],
           json["withDiscount"] as? Bool == true,
           json["stateGroup"] as? String == "NEW",
           json["damageGroup"] as? String == "ANY",
           json["customsStateGroup"] as? String == "DOESNT_MATTER",
           let filter = (json["catalogFilter"] as? [[String: String]])?.first,
           filter["mark"] == "BMW",
           filter["model"] == "3ER",
           filter["generation"] == "21398591",
           filter["configuration"] == "21398651" else {
            return .skip
        }

        return .ok
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        api.device.hello
            .post
            .ok(mock: .file("hello_ok"))

        api.history.last.category(.all)
            .get(parameters: .parameters([.page(1), .pageSize(20)]))
            .ok(mock: .file("history_last_all_characteristic_ok"))

        api.offer.category(.cars).offerID("1098230510-dd311329")
            .get
            .ok(mock: .file("offer_CARS_1098230510-dd311329_ok"))

        api.reference.catalog.cars.allOptions
            .get
            .ok(mock: .file("catalog_all_options"))

        api.reference.catalog.cars.configurations.subtree
            .get(parameters: .parameters([.configurationId(["21398651"])]))
            .ok(mock: .file("complectation_comparison_subtree_bmw_3er"))

        api.search.cars
            .post(parameters: .wildcard)
            .ok(mock: .file("search_cars_new_bmw_3er"))

        try! server.start()
    }
}
