import XCTest
import Moya
import Alamofire
@testable import APIService

func makeServiceFactoryForTest() -> ServiceFactory {
    let token = ""
    let interceptor = AuthInterceptor(authHeaderProvider: { token })

    let session: Session = {
        let manager = ServerTrustManager(evaluators: ["staging.telemed.yandex.ru": DisabledTrustEvaluator()])
        let configuration = URLSessionConfiguration.af.default
        return Session(configuration: configuration, serverTrustManager: manager)
    }()
    
    return ServiceFactory(authInterceptor: interceptor, session: session)
}

final class MedcardTests: XCTestCase {
    let service = makeServiceFactoryForTest().makeMedcardService()

    func testSomeRequest() {

        let promise = expectation(description: "api response")

        self.service.fetchAppointmentHistory { res in
            defer { promise.fulfill() }
            switch res {
            case .success(let response):
                XCTAssertEqual(response.data.type, "medcard")
            case .failure(let error):
                if let moyaError = error as? MoyaError {
                    print("moyaError", moyaError)

                    if case MoyaError.objectMapping(let error, _) = moyaError {
                        print("objectMapping error", error)
                    }
                }
                XCTAssertTrue(false)
            }
        }

        wait(for: [promise], timeout: 30)
    }
}
