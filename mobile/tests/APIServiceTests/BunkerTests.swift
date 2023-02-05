import XCTest
@testable import APIService

final class BunkerTests: XCTestCase {
    
    let provider = makeServiceFactoryForTest().makeBunkerService()

    func testBunkerCovidLandingAPI() {
        let promise = expectation(description: "api response")
        
        self.provider.fetchCovidLanding { res in
            defer { promise.fulfill() }
            switch res {
            case .success(let response):
                XCTAssertFalse(response.doctors.isEmpty)
            case .failure:
                XCTAssertTrue(false)
            }
        }
        
        wait(for: [promise], timeout: 5)
    }
    
    func testBunkerRemoteConfig() {
        let promise = expectation(description: "api response")
        
        self.provider.fetchRemoteConfig { res in
            defer { promise.fulfill() }
            switch res {
            case .success(let response):
                XCTAssertEqual(false, response.payments?.applePayEnabled)
            case .failure:
                XCTAssertTrue(false)
            }
        }
        
        wait(for: [promise], timeout: 5)
    }
}
