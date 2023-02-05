import Foundation
import YxSwissKnife
import XCTest

final class GlobalTests: XCTestCase {

    var flag = false

    func testWith() throws {
        flag = false
        try with(1, do: `do`)
        XCTAssertTrue(flag)
    }

    private func `do`(x: Int) throws {
        flag = true
    }
}
