import XCTest
@testable import YxSwissKnife

class YxWeakSetTests: XCTestCase {

    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }

    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }

    func testConstructors() {
        // empty constructor
        let set1 = YxWeakSet<NSObject>()
        XCTAssert(set1.count == 0)

        // arrayLiteral
        let set2: YxWeakSet<NSObject> = []
        XCTAssert(set2.count == 0)

        // sequence
        let seq = [NSObject]()
        let set3 = YxWeakSet(seq)
        XCTAssert(set3.count == 0)
    }

    func testCount() {
        let obj1 = NSObject()
        let obj2 = NSObject()
        let set1 = YxWeakSet([obj1, obj1, obj2])
        XCTAssert(set1.count == 2)
    }

    func testSequence() {
        let obj1 = NSObject()
        let obj2 = NSObject()

        let set1 = YxWeakSet([obj1, obj1, obj2])
        var counter = 0
        for obj in set1 {
            XCTAssert(obj === obj1 || obj === obj2)
            counter += 1
        }
        XCTAssert(counter == 2)
    }

    func testFunctions() {
        let obj1 = NSObject()

        var set1 = YxWeakSet<AnyObject>()
        XCTAssert(set1.count == 0)
        XCTAssert(set1.insert(obj1))
        XCTAssertFalse(set1.insert(obj1))
        XCTAssert(set1.count == 1)
        XCTAssert(set1.remove(obj1) === obj1)
        XCTAssert(set1.remove(obj1) === nil)
        XCTAssert(set1.count == 0)

        set1.update(obj1)
        XCTAssert(set1.contains(obj1))
        XCTAssert(set1.count == 1)
        set1.removeAll()
        XCTAssert(set1.count == 0)

        autoreleasepool {
            XCTAssert(set1.insert(NSObject()))
        }
        XCTAssert(set1.count == 0)
        XCTAssert(set1.isEmpty)

        XCTAssert(set1.insert(obj1))
        let set2 = YxWeakSet(set1)
        let set3 = set2
        XCTAssert(set1 == set2)
        XCTAssert(set1 == set3)
        XCTAssert(set3 == set2)
    }
}
