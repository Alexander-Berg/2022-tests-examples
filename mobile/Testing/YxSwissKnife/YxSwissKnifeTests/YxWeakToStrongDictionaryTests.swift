import XCTest
@testable import YxSwissKnife

class YxWeakToStrongDictionaryTests: XCTestCase {

    class DeinitNotifiable {
        var closure: (() -> Void)?
        init(_ closure: (() -> Void)?) { self.closure = closure }
        deinit {
            closure?()
        }
    }

    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }

    func testConstructors() {    }

    func testCount() {
    }

    func testSequence() {
    }

    func testFunctions() {
    }

    func testWeakness() {
        let exp = XCTestExpectation()
        //var to =

        var dic = YxWeakToStrongDictionary<NSObject, DeinitNotifiable>()
        autoreleasepool {
            var from: NSObject? = NSObject()
            _ = dic.updateValue(DeinitNotifiable({
                exp.fulfill()
            }), forKey: from!)
            from = nil
        }
        wait(for: [exp], timeout: 60)
    }
}
