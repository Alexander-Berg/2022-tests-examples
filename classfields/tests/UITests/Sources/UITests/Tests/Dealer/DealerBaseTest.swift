import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

class DealerBaseTest: BaseTest {
    static let requestTimeout: TimeInterval = 10.0

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: - Interface

    func setupServer() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }
        server.forceLoginMode = .forceLoggedIn

        try! server.start()
    }
}
