import XCTest
@testable import APIService

final class PlatformTests: XCTestCase {
    let service = makeServiceFactoryForTest().makePlatformService()
    
    func testFullDoctor() {
        let promise = expectation(description: "fetch doctor detail")
        
        self.service.fetchDoctorDetail(doctorID: .init(value: "157"), taxonomyID: .init(value: "25")) { res in
            defer { promise.fulfill() }
            
            switch res {
            case .success(let response):
                XCTAssertEqual(response.fullName, "Имя Отчество Психолог")
            case .failure(let error):
                XCTFail(error.localizedDescription)
            }
        }
        
        wait(for: [promise], timeout: 30)
    }
    
    func testFetchTaxonomyDoctors() {
        
        let promise = expectation(description: "api response")

        self.service.fetchTaxonomyDoctors(taxonomyID: .init(value: "25")) { res in
            defer { promise.fulfill() }
            switch res {
            case .success(let response):
                XCTAssertEqual(response.data.count, 2)
                
            case .failure(let error):
                XCTFail(error.localizedDescription)
            }
        }

        wait(for: [promise], timeout: 30)
    }
}
