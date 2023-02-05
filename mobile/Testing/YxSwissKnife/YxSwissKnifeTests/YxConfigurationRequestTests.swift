//swiftlint:disable file_length

import Foundation
import XCTest
import Swifter

@testable import YxSwissKnife

// NOTE: this tests are run by hand, because on TARS'es it requires puncher rights to MDS-S3 testing

//swiftlint:disable type_body_length
class YxConfigurationRequestTests: XCTestCase, URLSessionDelegate {

    var service: YxNetworkService!

    override func setUp() {
        super.setUp()
        service = YxNetworkService(session: URLSession(configuration: .default, delegate: self, delegateQueue: nil))
    }

    // MARK: GET

    func testGETRequest() {
        let req = YxConfigurationRequest(source: YxConfigurationSource(
            s3: .testing,
            applicationName: "test",
            configName: "config.json"
        ))

        var ok: Bool = false
        let expect = expectation(description: "YxTestGetRequest")

        let drop: () -> Void = {
            ok = false
            expect.fulfill()
        }

        req.perform(in: service) { result in
            switch result {
            case .success(let object):
                guard let obj = object as? [String: Any] else {
                    drop()
                    return
                }

                guard let str = obj["test_flag_str"] as? String, str == "A" else {
                    drop()
                    return
                }

                guard let intVal = obj["test_flag_int"] as? Int, intVal == 1 else {
                    drop()
                    return
                }

                guard let dblVal = obj["test_flag_float"] as? Double, abs(dblVal - 1.5) < Double.ulpOfOne else {
                    drop()
                    return
                }

                guard let boolVal = obj["test_flag_bool"] as? Bool, boolVal else {
                    drop()
                    return
                }

                guard let timestampVal = obj["test_flag_timestamp"] as? Int, timestampVal == 234242 else {
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
        wait(for: [expect], timeout: 30)
        XCTAssert(ok == true)
    }

    func urlSession(_ session: URLSession, didReceive challenge: URLAuthenticationChallenge, completionHandler: @escaping (URLSession.AuthChallengeDisposition, URLCredential?) -> Void) {
        if challenge.protectionSpace.authenticationMethod == NSURLAuthenticationMethodServerTrust {
            if challenge.protectionSpace.host == "mobile-configs.s3.mdst.yandex.net" {
                let credentials = URLCredential(trust: challenge.protectionSpace.serverTrust!)
                completionHandler(.useCredential, credentials)
            }
        }
    }
}
