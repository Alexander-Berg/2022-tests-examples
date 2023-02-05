import XCTest
@testable import MedicineLocalization


final class LocalizationTests: XCTestCase {
    func testRatingFormatter() throws {
        let formatter = AppFormatter.rating
        XCTAssertEqual(formatter.string(from: 4.2), "4,2")
        XCTAssertEqual(formatter.string(from: 4.0), "4,0")
        XCTAssertEqual(formatter.string(from: 4), "4,0")
        XCTAssertEqual(formatter.string(from: 4.45), "4,4")
    }
    
    func testYearsCount() throws {
        XCTAssertEqual(L10n.Experience.years(5), "Стаж: 5 лет")
    }
}
