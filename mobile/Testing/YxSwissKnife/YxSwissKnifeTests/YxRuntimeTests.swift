import XCTest
@testable import YxSwissKnife

class YxRuntimeTest: XCTestCase {

    class Foo {
        var val: Int = 0
        deinit {
            print("deinit")
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

    func testGetHash() {
        let obj = Foo()
        let hash1 = YxRuntime.getReferenceHash(of: obj)
        let hash2 = YxRuntime.getReferenceHash(of: obj)
        XCTAssert(hash1 == hash2)

        let obj2 = Foo()
        let hash3 = YxRuntime.getReferenceHash(of: obj2)
        XCTAssert(hash1 != hash3)

        obj.val = 17
        let hash4 = YxRuntime.getReferenceHash(of: obj)
        XCTAssert(hash1 == hash4)

        let obj3 = obj
        let hash5 = YxRuntime.getReferenceHash(of: obj3)
        XCTAssert(hash1 == hash5)
    }

    func testObjectDeinitEvent() {
        let exp = XCTestExpectation()
        let handler = YxEventHandler<Void> { _ in
            exp.fulfill()
        }
        autoreleasepool {
            var obj: Foo? = Foo()
            YxRuntime.onObjectDeinitEvent(obj!) += handler
            obj = nil
        }
        wait(for: [exp], timeout: 60)
    }
}
