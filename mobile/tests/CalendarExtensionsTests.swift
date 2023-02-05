import Foundation
import XCTest
import YandexMapsUtils

class CalendarExtensionsTests: XCTestCase {

    let calendar = Locale(identifier: "ru_RU").calendar

    func testIsSameWeekWhenDatesContainsDateOfSameWeekThenReturnsTrue() {
        let today = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 14, hour: 5))!

        let holidays = [
            // same week
            calendar.date(from: DateComponents(year: 2_019, month: 3, day: 17, hour: 5))!
        ]
        let actual = calendar.isDate(today, inSameWeekAsOneOf: holidays)
        XCTAssert(actual == true)
    }

    func testIsSameWeekWhenDatesContainsDateOfSameWeekAndDateOfAnotherWeekThenReturnsTrue() {
        let today = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 14, hour: 5))!

        let holidays = [
            // same week
            calendar.date(from: DateComponents(year: 2_019, month: 3, day: 17, hour: 5))!,
            // another week
            calendar.date(from: DateComponents(year: 2_019, month: 3, day: 18, hour: 5))!
        ]
        let actual = calendar.isDate(today, inSameWeekAsOneOf: holidays)
        XCTAssert(actual == true)
    }

    func testIsSameWeekWhenDatesContainsOnlyDatesOfAnotherWeeksThenReturnsFalse() {
        let today = calendar.date(from: DateComponents(year: 2_019, month: 3, day: 14, hour: 5))!

        let holidays = [
            // prev week
            calendar.date(from: DateComponents(year: 2_019, month: 3, day: 6, hour: 5))!,
            // another week
            calendar.date(from: DateComponents(year: 2_019, month: 3, day: 18, hour: 5))!
        ]
        let actual = calendar.isDate(today, inSameWeekAsOneOf: holidays)
        XCTAssert(actual == false)
    }
}
