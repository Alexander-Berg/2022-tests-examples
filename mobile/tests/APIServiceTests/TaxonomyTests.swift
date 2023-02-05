import XCTest
@testable import APIService

final class TaxonomyTests: XCTestCase {
    let service = makeServiceFactoryForTest().makeTaxonomyService()

    func testFetchTelemedServices() {

        let promise = expectation(description: "api response")

        self.service.telemedServices(taxonomyID: .init(value: "1")) { res in
            defer { promise.fulfill() }
            switch res {
            case .success(let response):
                XCTAssertEqual(response.data.count, 3)
                XCTAssertEqual(1, response.included.compactMap({ $0.asTaxonomy }).count)
                XCTAssertEqual(1, response.data.compactMap({ $0.asSubscription }).count)
                XCTAssertEqual(2, response.data.compactMap({ $0.asTelemedService }).count)
                
            case .failure(let error):
                XCTFail(error.localizedDescription)
            }
        }

        wait(for: [promise], timeout: 30)
    }
}
