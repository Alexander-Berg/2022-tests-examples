import Foundation
import XCTest

@testable import YxSwissKnife

final class YxScreenshotTestTest: XCTestCase {

    func testRecordingMode() {
        let conf = YxScreenshotTestConfiguration(recordMode: true, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = UILabel(frame: CGRect(x: 0, y: 0, width: 320, height: 40))
        view.textAlignment = .center
        view.textColor = .red
        view.backgroundColor = .black
        view.font = UIFont.systemFont(ofSize: 14)
        view.text = "This is a text!"

        var exceptionWasThrown = false
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))", index: 0), tolerance: 0)
        } catch {
            if checkErrorIsRecordMode(error: error) {
                exceptionWasThrown = true
            } else {
                XCTFail("Exception was thrown, but incorrect")
            }
        }
        XCTAssertTrue(exceptionWasThrown, "record mode success")

        exceptionWasThrown = false
        do {
            try view.layer.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))", index: 1), tolerance: 0)
        } catch {
            if checkErrorIsRecordMode(error: error) {
                exceptionWasThrown = true
            } else {
                XCTFail("Exception was thrown, but incorrect")
            }
        }
        XCTAssertTrue(exceptionWasThrown, "record mode success")
    }

    func testVerificationMode() {
        let conf = YxScreenshotTestConfiguration(recordMode: false, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = UILabel(frame: CGRect(x: 0, y: 0, width: 320, height: 40))
        view.textAlignment = .right
        view.textColor = .blue
        view.backgroundColor = .white
        view.font = UIFont.systemFont(ofSize: 12)
        view.text = "This is a very interesting text!"

        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))", index: 0), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }

        do {
            try view.layer.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))", index: 1), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
}

private func checkErrorIsRecordMode(error: Error) -> Bool {
    switch error {
    case YxScreenshotTestError.recordMode:
        return true

    case let YxScreenshotTestError.composite(errors):
        for error in errors {
            if !checkErrorIsRecordMode(error: error) {
                return false
            }
        }
        return true

    default:
        return false
    }
}

private let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
private let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!
