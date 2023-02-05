//swiftlint:disable file_length

import Foundation
import XCTest
import Swifter

@testable import YxSwissKnife

//swiftlint:disable type_body_length
class YxNetworkingTests: XCTestCase {

    static var server: HttpServer!
    static var service: YxNetworkService!

    override class func setUp() {
        super.setUp()
        server = HttpServer()
        server.listenAddressIPv6 = "0:0:0:0:0:0:0:1"
        server.listenAddressIPv4 = "127.0.0.1"

        server.GET["/check1"] = { req in
            var json = [String: Any]()
            json["x"] = 1
            json["y"] = "One"
            json["z"] = true

            return .ok(.json(json as AnyObject))
        }

        server.POST["/check2"] = { req in
            let data = Data(bytes: req.body)
            guard let js = (try? JSONSerialization.jsonObject(with: data, options: [])) as? [String: Any] else {
                return .badRequest(.text("invalid json"))
            }

            var json = [String: Any]()
            json["req"] = js["req"]
            json["res"] = "ok"

            return .ok(.json(json as AnyObject))
        }

        server.GET["/long"] = { req in
            Thread.sleep(forTimeInterval: TimeInterval(60))
            return .ok(.text("DONE"))
        }

        server.GET["/redirect"] = { req in
            return .raw(304, "TEST", nil, nil)
        }

        do {
            try server.start(8888)
        } catch {
            XCTAssert(false)
        }
        service = YxNetworkService()
    }

    // MARK: GET

    class YxTestGetRequest: YxNetworkRequest {
        override class var humanDescription: String {
            return "yx test get request"
        }

        override class var method: YxHTTPMethod {
            return .get
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override var apiEndpoint: String {
            return "check1"
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return try? JSONSerialization.jsonObject(with: data, options: [])
        }
    }

    func testGETRequest() {
        let req = YxTestGetRequest()

        var ok: Bool = false
        let excp = expectation(description: "YxTestGetRequest")

        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        req.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case .success(let object):
                guard let obj = object as? [String: AnyObject] else {
                    drop()
                    return
                }

                guard let x = obj["x"] as? Int, x == 1 else {
                    drop()
                    return
                }

                guard let y = obj["y"] as? String, y == "One" else {
                    drop()
                    return
                }

                guard let zObj = obj["z"] as? Bool, zObj == true else {
                    drop()
                    return
                }

                ok = true
                excp.fulfill()

            case .redirected, .cancelled, .failure:
                drop()
                return
            }
        }
        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)
    }

    final class YxTestGetRequestOperation: YxNetworkOperation<YxTestGetRequest> {
        private(set) var ok = true
        private let expect: XCTestExpectation

        init(
            expect: XCTestExpectation,
            request: YxTestGetRequest,
            service: YxNetworkService,
            dispatcher: YxDispatcher
        ) {
            self.expect = expect
            super.init(
                request: request,
                service: service,
                logger: nil,
                dispatcher: dispatcher,
                writeOpLog: false
            )
        }

        private func drop() {
            ok = false
            expect.fulfill()
        }

        override func request(_ request: YxNetworkingTests.YxTestGetRequest, didFinishWith result: YxNetworkRequest.Result) {
            switch result {
            case let .success(object):
                guard let obj = object as? [String: AnyObject] else {
                    drop()
                    return
                }

                guard let x = obj["x"] as? Int, x == 1 else {
                    drop()
                    return
                }

                guard let y = obj["y"] as? String, y == "One" else {
                    drop()
                    return
                }

                guard let zObj = obj["z"] as? Bool, zObj == true else {
                    drop()
                    return
                }

                ok = true
                expect.fulfill()

            case .redirected, .cancelled, .failure:
                drop()
                return
            }
        }
    }

    func testGETRequestOperation() {
        let req = YxTestGetRequest()
        let dispatcher = YxRootDispatcher(
            logger: nil,
            queueProvider: YxOpcodeBasedQueueProvider(logger: nil),
            exclusivityController: YxExclusivityController()
        )

        let excp = expectation(description: "YxTestGetRequestOperation")

        let operation = YxTestGetRequestOperation(
            expect: excp,
            request: req,
            service: YxNetworkingTests.service,
            dispatcher: dispatcher
        )

        dispatcher.add(one: operation)
        wait(for: [excp], timeout: 30)
        XCTAssert(operation.ok == true)
    }

    // MARK: POST

    class YxTestPostRequest: YxNetworkRequest {

        override class var humanDescription: String {
            return "yx test post request"
        }

        override class var method: YxHTTPMethod {
            return .post
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override var apiEndpoint: String {
            return "check2"
        }

        override func mapRequestObject() -> Data? {
            let json: [String: Any] = [
                "req": "privet"
            ]
            return try? JSONSerialization.data(withJSONObject: json, options: [])
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return try? JSONSerialization.jsonObject(with: data, options: [])
        }
    }

    func testPOSTRequest() {
        let req = YxTestPostRequest()

        var ok: Bool = false
        let excp = expectation(description: "YxTestPostRequest")

        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        req.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case .success(let object):
                guard let obj = object as? [String: AnyObject] else {
                    drop()
                    return
                }

                guard let req = obj["req"] as? String, req == "privet" else {
                    drop()
                    return
                }

                guard let res = obj["res"] as? String, res == "ok" else {
                    drop()
                    return
                }

                ok = true
                excp.fulfill()

            case .redirected, .cancelled, .failure:
                drop()
                return
            }
        }

        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)
    }

    // MARK: Activity Indicator

    class YxTestLongRequest: YxNetworkRequest {

        override class var humanDescription: String {
            return "yx test long request"
        }

        override class var method: YxHTTPMethod {
            return .get
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override var apiEndpoint: String {
            return "long"
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return String(data: data, encoding: .utf8)
        }
    }

    final class YxTestBadUrlRequest: YxNetworkRequest {

        override class var humanDescription: String {
            return "yx test bad url"
        }

        override class var method: YxHTTPMethod {
            return .get
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override func urlParams() throws -> [(String, String)]? {
            throw YxNetworkErrors.urlParameterMissing(name: "parameterName")
        }

        override var apiEndpoint: String {
            return "badEndpoint"
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return String(data: data, encoding: .utf8)
        }
    }

    func testBadUrlRequest() {
        let request = YxTestBadUrlRequest()
        let delegate = YxNetworkServiceDumbDelegate()
        delegate.didPrepared = { req, url in XCTFail("didPrepared should not be called") }

        YxNetworkingTests.service.delegate = delegate

        var ok: Bool = false
        let excp = expectation(description: "YxTestBadUrl")

        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        request.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case .failure:
                ok = true
                excp.fulfill()

            case .success, .redirected, .cancelled:
                drop()
                return
            }
        }

        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)

        YxNetworkingTests.service.delegate = nil
    }

    // NOTE: testing indicator and cancellation
    func testLongRequest() {
        let req = YxTestLongRequest()
        let delegate = YxNetworkServiceDumbDelegate()
        delegate.willPerform = { req in
            DispatchQueue.main.async {
                XCTAssert(!UIApplication.shared.isNetworkActivityIndicatorVisible)
            }
        }
        delegate.didCheckedReachability = { req, reachable in
            DispatchQueue.main.async {
                XCTAssert(UIApplication.shared.isNetworkActivityIndicatorVisible)
            }
        }
        delegate.didFinish = { req in
            DispatchQueue.main.async {
                XCTAssert(!UIApplication.shared.isNetworkActivityIndicatorVisible)
            }
        }

        YxNetworkingTests.service.delegate = delegate

        var ok: Bool = false
        let excp = expectation(description: "YxTestLongRequest")

        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        req.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case .cancelled:
                ok = true
                excp.fulfill()

            case .success, .redirected, .failure:
                drop()
                return
            }
        }

        DispatchQueue.global().asyncAfter(deadline: .now() + 10) {
            req.cancel()
        }
        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)

        YxNetworkingTests.service.delegate = nil
    }

    // MARK: - Timeout

    class YxTestTimeoutRequest: YxNetworkRequest {

        override class var humanDescription: String {
            return "yx test long request"
        }

        override class var method: YxHTTPMethod {
            return .get
        }

        override class var timeout: TimeInterval {
            return 3.0
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override var apiEndpoint: String {
            return "long"
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return String(data: data, encoding: .utf8)
        }
    }

    func testTimeoutRequest() {
        let req = YxTestTimeoutRequest()

        var ok: Bool = false
        let excp = expectation(description: "YxTestTimeoutRequest")
        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        req.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case let .failure(error):
                XCTAssert((error as NSError)._code == NSURLErrorTimedOut)
                ok = true
                excp.fulfill()

            case .success, .redirected, .cancelled:
                drop()
                return
            }
        }

        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)
    }

    // MARK: Redirects
    class YxTestRedirectRequest: YxNetworkRequest {

        override class var humanDescription: String {
            return "yx test redirect request"
        }
        override class var method: YxHTTPMethod {
            return .get
        }

        override var baseUrl: String {
            return "http://localhost:8888"
        }

        override var apiEndpoint: String {
            return "redirect"
        }

        override func mapResponseObject(data: Data?) -> Any? {
            guard let data = data else { return nil }
            return String(data: data, encoding: .utf8)
        }
    }

    // NOTE: testing indicator and cancellation
    func testRedirectRequest() {
        let req = YxTestRedirectRequest()

        var ok: Bool = false
        let excp = expectation(description: "YxTestRedirectRequest")
        let drop: () -> Void = {
            ok = false
            excp.fulfill()
        }

        req.perform(in: YxNetworkingTests.service) { result in
            switch result {
            case .redirected(let statusCode, _):
                XCTAssert(statusCode == 304)
                ok = true
                excp.fulfill()

            case .success, .cancelled, .failure:
                drop()
                return
            }
        }
        wait(for: [excp], timeout: 30)
        XCTAssert(ok == true)
    }
}
