//
//  NetworkLayerTests.swift
//  NetworkLayerTests
//
//  Created by Timur Turaev on 21.06.2021.
//

import XCTest
@testable import NetworkLayer

internal final class BuildRequestTargetURLTest: XCTestCase {
    fileprivate static let baseURL = URL(string: "https://mail.yandex.test")!

    func testGivenEmptyLoginWhenConstructRequestURLThenReturnBaseURL() {
        let targetPath = "test_path"
        let request = TestRequest()
        request.targetPathValue = targetPath

        let params = TestHTTPClientVaryingParameters()

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)

        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath, constructedPath.absoluteString)
    }

    func testGivenNonNilLoginWhenConstructRequestURLThenReturnBaseURL() {
        let targetPath = "test_path"
        let request = TestRequest()
        request.targetPathValue = targetPath

        let params = TestHTTPClientVaryingParameters()

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: "test@yandex.ru")
        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath, constructedPath.absoluteString)
    }

    func testGivenCustomBaseURLWhenConstructRequestURLThenReturnCustomURL() {
        let targetPath = "test_path"
        let baseURL = URL(string: "https://yandex.ru")!
        let request = TestRequest()
        request.targetPathValue = targetPath
        request.baseURLValue = baseURL

        let params = TestHTTPClientVaryingParameters()

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(baseURL.absoluteString + "/v1/" + targetPath, constructedPath.absoluteString)
    }

    func testGivenV2APIVersionWhenConstructRequestURLThenReturnURLWithV2() {
        let targetPath = "test_path"
        let request = TestRequest()
        request.apiVersionValue = .V2
        request.targetPathValue = targetPath

        let params = TestHTTPClientVaryingParameters()

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(Self.baseURL.absoluteString + "/v2/" + targetPath, constructedPath.absoluteString)
    }

    func testGivenTargetPathWithoutQueryWhenConstructRequestURLThenReturnURLWithUUID() {
        let targetPath = "test_path"
        let uuid = "ASDF-1234-GHJK-5678"
        let request = TestRequest()
        request.targetPathValue = targetPath

        let params = TestHTTPClientVaryingParameters()
        params.uuid = uuid

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath + "?uuid=" + uuid, constructedPath.absoluteString)
    }

    func testGivenTargetPathQueryWhenConstructRequestURLThenReturnURLWithUUIDAndQuery() {
        let targetPath = "test_path"
        let uuid = "ASDF-1234-GHJK-5678"
        let request = TestRequest()
        request.targetPathValue = targetPath
        request.queryParametersValue = ["param1": "1"]

        let params = TestHTTPClientVaryingParameters()
        params.uuid = uuid

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath + "?uuid=" + uuid + "&" + "param1=1", constructedPath.absoluteString)
    }

    func testGivenRequestHasExtensionWhenConstructRequestURLThenReturnURLWithExtension() {
        let targetPath = "test_path"
        let uuid = "ASDF-1234-GHJK-5678"
        let request = TestRequest()
        request.targetPathValue = targetPath

        let params = TestHTTPClientVaryingParameters()
        params.uuid = uuid
        params.appState = "foreground"

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath + "?app_state=foreground&uuid=" + uuid,
                       constructedPath.absoluteString)
    }

    func testGivenRequestHasClientExtensionWhenConstructRequestURLThenReturnURLWithClientExtension() {
        let targetPath = "test_path"
        let uuid = "ASDF-1234-GHJK-5678"
        let request = TestRequest()
        request.targetPathValue = targetPath
        request.shouldExtendPathValue = true

        let params = TestHTTPClientVaryingParameters()
        params.uuid = uuid
        params.client = "iphone"

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: nil)
        XCTAssertEqual(Self.baseURL.absoluteString + "/v1/" + targetPath + "?uuid=" + uuid + "&" + "client=iphone",
                       constructedPath.absoluteString)
    }

    func testGivenRequestWithAllParamsWhenConstructRequestURLThenReturnFullURL() {
        let login = "test@yandex.ru"
        let targetPath = "test_path"
        let uuid = "ASDF-1234-GHJK-5678"
        let clientExtensionValue = "client=iphone"
        let baseURL = URL(string: "https://yandex.ru")!
        let request = TestRequest()
        request.targetPathValue = targetPath
        request.shouldExtendPathValue = true
        request.apiVersionValue = .V2
        request.queryParametersValue = ["param": "1"]
        request.baseURLValue = baseURL

        let params = TestHTTPClientVaryingParameters()
        params.uuid = uuid
        params.appState = "foreground"
        params.client = "iphone"

        let constructedPath = request.buildTargetURL(withHTTPClientParameters: params, login: login)
        XCTAssertEqual(baseURL.absoluteString + "/v2/" + targetPath
                        + "?app_state=foreground"
                        + "&uuid=" + uuid
                        + "&param=1"
                        + "&" + clientExtensionValue,
                       constructedPath.absoluteString)
    }
}

private final class TestRequest: YORequest {
    var targetPathValue = ""
    var apiVersionValue = YOAPIVersion.V1
    var shouldExtendPathValue = false
    var queryParametersValue: [String: Any] = [:]
    var baseURLValue: URL?

    override func targetPath() -> String {
        return self.targetPathValue
    }

    override func apiVersion() -> YOAPIVersion {
        return self.apiVersionValue
    }

    override func queryParameters() -> [String: Any] {
        return self.queryParametersValue
    }

    override func customBaseURL() -> URL? {
        return self.baseURLValue
    }

    override func shouldExtendPathWithClient() -> Bool {
        return self.shouldExtendPathValue
    }
}

private final class TestHTTPClientVaryingParameters: HTTPClientVaryingParameters {
    var appState: String? = ""
    var uuid: String? = ""
    var userAgent = "test_ua"
    var client = ""

    func baseURLFor(_ login: String?) -> URL {
        return BuildRequestTargetURLTest.baseURL
    }

    func tabsAvailableForLogin(_ login: String?) -> Bool {
        return false
    }
}
