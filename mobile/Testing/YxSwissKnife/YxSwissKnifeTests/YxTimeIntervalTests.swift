import Foundation
import XCTest
import YxSwissKnife

final class YxTimeIntervalTests: XCTestCase {

    func testIntervals() {
        XCTAssertEqual(TimeInterval.from(milliseconds: 1), 0.001, accuracy: 0.0001)
        XCTAssertEqual(TimeInterval.from(seconds: 1), 1.0, accuracy: 0.1)
        XCTAssertEqual(TimeInterval.from(minutes: 1), 60.0, accuracy: 1)
        XCTAssertEqual(TimeInterval.from(hours: 1), 3600.0, accuracy: 1)
        XCTAssertEqual(TimeInterval.from(days: 1), 86400.0, accuracy: 1)
        XCTAssertEqual(TimeInterval.from(weeks: 1), 604800.0, accuracy: 1)
        XCTAssertEqual(TimeInterval.from(hz: 10), 0.1, accuracy: 0.01)
    }

}
