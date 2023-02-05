import OHHTTPStubs
import XCTest

class NetworkingTestCase: XCTestCase {

    override func setUp() {
        super.setUp()

        // Нужно для того, чтобы не ходить в сеть в юнит тестах
        OHHTTPStubs.stubRequests(passingTest: { req -> Bool in
            print(req)
            return true
        }, withStubResponse: { _ -> OHHTTPStubsResponse in
            OHHTTPStubsResponse(
                data: Data(),
                statusCode: 404,
                headers: nil
            )
        })
    }

    override func tearDown() {
        OHHTTPStubs.removeAllStubs()
        super.tearDown()
    }

    func stub(
        requestPartName: String,
        responseFileName: String,
        headers: [String: String] = [:],
        testBlock: OHHTTPStubsTestBlock? = nil
    ) {
        OHHTTPStubs.stubRequests(passingTest: { request in
            guard request.url?.absoluteString.contains(requestPartName) ?? false else {
                return false
            }
            return testBlock?(request) ?? true
        }, withStubResponse: { _ -> OHHTTPStubsResponse in
            if let data = self.dataFromBundle(resourceName: responseFileName) {
                return OHHTTPStubsResponse(
                    data: data,
                    statusCode: 200,
                    headers: headers
                )
            }
            XCTFail("Unable to load response object")
            return OHHTTPStubsResponse(
                data: Data(),
                statusCode: 200,
                headers: headers
            )
        })
    }

    func stubError(
        requestPartName: String,
        code: Int32,
        responseFileName: String? = nil,
        testBlock: OHHTTPStubsTestBlock? = nil
    ) {
        OHHTTPStubs.stubRequests(passingTest: { request in
            guard request.url?.absoluteString.contains(requestPartName) ?? false else {
                return false
            }

            return testBlock?(request) ?? true
        }, withStubResponse: { _ -> OHHTTPStubsResponse in
            let data = responseFileName.flatMap { self.dataFromBundle(resourceName: $0) } ?? Data()

            return OHHTTPStubsResponse(
                data: data,
                statusCode: code,
                headers: nil
            )
        })
    }

    func stubNetworkError(requestPartName: String, with code: URLError.Code) {
        OHHTTPStubs.stubRequests(passingTest: { request in
            request.url?.absoluteString.contains(requestPartName) ?? false
        }, withStubResponse: { _ in
            let error = NSError(domain: NSURLErrorDomain, code: code.rawValue, userInfo: nil)
            return OHHTTPStubsResponse(error: error)
        })
    }

    func makeFAPIJsonBody(params: [AnyHashable: Any]) -> [AnyHashable: Any] {
        ["params": [params]]
    }
}
