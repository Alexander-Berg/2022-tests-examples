import Foundation
import XCTest

@testable import YxSwissKnife

final class YxCalendarScreenshotTest: XCTestCase {

    func testDefaultCalendar() {
        let conf = YxScreenshotTestConfiguration(recordMode: recordMode, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = YxCalendarView(frame: CGRect(x: 0, y: 0, width: 320, height: 568))
        view.selectedPeriod = YxCalendarView.CalendarPeriod(
            start: YxCalendarView.DayInfo(year: 2018, month: 6, day: 10),
            end: YxCalendarView.DayInfo(year: 2018, month: 6, day: 25)
        )
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }

    func testOneDaySelectedCalendar() {
        let conf = YxScreenshotTestConfiguration(recordMode: recordMode, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = YxCalendarView(frame: CGRect(x: 0, y: 0, width: 320, height: 568))
        view.selectedPeriod = YxCalendarView.CalendarPeriod(
            start: YxCalendarView.DayInfo(year: 2018, month: 3, day: 8),
            end: YxCalendarView.DayInfo(year: 2018, month: 3, day: 8)
        )
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }

    func testTwoDaySelectedCalendar() {
        let conf = YxScreenshotTestConfiguration(recordMode: recordMode, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = YxCalendarView(frame: CGRect(x: 0, y: 0, width: 320, height: 568))
        view.selectedPeriod = YxCalendarView.CalendarPeriod(
            start: YxCalendarView.DayInfo(year: 2018, month: 4, day: 20),
            end: YxCalendarView.DayInfo(year: 2018, month: 4, day: 21)
        )
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }

    func testNoSelectionCalendar() {
        let conf = YxScreenshotTestConfiguration(recordMode: recordMode, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = YxCalendarView(frame: CGRect(x: 0, y: 0, width: 320, height: 568))
        view.scrollToYear(2018, month: 10)
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }

    func testStyledCalendar() {
        let conf = YxScreenshotTestConfiguration(recordMode: recordMode, useDrawHierarchyInRect: true, referenceDir: referenceDirPath, allowedScales: [1, 2, 3])
        let view = YxCalendarView(frame: CGRect(x: 0, y: 0, width: 320, height: 568))

        view.hPadding = 10.0
        view.config = YxCalendarConfig(
            yearLabelFont: .systemFont(ofSize: 17, weight: .medium),
            yearLabelColor: .black,
            controlsColor: .black,
            separatorsColor: UIColor(red: 229, green: 229, blue: 229),
            weekDaysFont: .systemFont(ofSize: 12, weight: .regular),
            weekDaysColor: .black,
            selectedBoundsColor: UIColor(red: 255, green: 221, blue: 96),
            selectedColor: UIColor(red: 243, green: 241, blue: 237),
            disabledColor: UIColor(red: 153, green: 153, blue: 153),
            monthFont: .systemFont(ofSize: 14, weight: .regular),
            monthColor: UIColor(red: 119, green: 119, blue: 119),
            daysFont: .systemFont(ofSize: 17, weight: .regular),
            daysColor: .black
        )
        view.selectedPeriod = YxCalendarView.CalendarPeriod(
            start: YxCalendarView.DayInfo(year: 2018, month: 1, day: 1),
            end: YxCalendarView.DayInfo(year: 2018, month: 1, day: 10)
        )
        do {
            try view.check(conf: conf, identifier: conf.makeDefaultIdentifier(pack: "\(type(of: self))"), tolerance: 0)
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
}

private let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
private let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!

private let recordMode = false
