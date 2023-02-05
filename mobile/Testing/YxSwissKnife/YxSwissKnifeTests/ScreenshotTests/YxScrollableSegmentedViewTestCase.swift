import XCTest
import UIKit

@testable import YxSwissKnife

final class YxScrollableSegmentedViewTestCase: XCTestCase {
    
    func testNoConfigScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: ["testtesttesttesttesttesttesttest", "test1test1test1test1test1test1test1"], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }

    func testSmallScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: ["test", "test1", "test2"], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
    
    func testSelectedScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: ["test", "test1", "test2"], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        view.selectedSegmentIndex = 0
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
    
    func testConfiguratedScrollableSegmentedView() {
        let config = YxScrollableSegmentedView.Configuration(
            segmentsBackgroundColor: .white,
            segmentsSelectedBackgroundColor: .yellow,
            borderColor: .yellow,
            titlesTextColor: .yellow,
            titlesSelectedTextColor: .white,
            cornerRadius: 0,
            hasBorder: true,
            labelPadding: 10,
            hasInnerBorders: false
        )
        let view = YxScrollableSegmentedView(titles: ["test", "test1", "test2"], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70), configuration: config)
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
    
    func testEmptyScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: [], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
    
    func testOneSegmentScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: ["test"], frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
    
    func testTenSegmentsScrollableSegmentedView() {
        let view = YxScrollableSegmentedView(titles: Array(repeating: "testtest", count: 10), frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 70))
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
}

private let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
private let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!
private let conf = YxScreenshotTestConfiguration(recordMode: false, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
