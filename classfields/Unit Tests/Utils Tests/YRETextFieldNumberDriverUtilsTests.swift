//
//  YRETextFieldNumberDriverUtilsTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 9/25/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRELegacyFiltersCore

final class YRETextFieldNumberDriverUtilsTests: XCTestCase {
    func testSafeRangeForString() {
        let string = "0123"
        let stringLength = string.count

        // goodRange, i.e. "[0]123"
        let goodRange = NSRange(location: 0, length: 1)
        XCTAssertEqual(goodRange, utils.safeRange(goodRange, for: string))

        // bad range, location beyound string, i.e.  "0123"  [  ]
        let badRangeOver = NSRange(location: stringLength + 1, length: 3)
        let manualBadRangeOverSanitized = NSRange(location: stringLength, length: 0)
        XCTAssertEqual(manualBadRangeOverSanitized, utils.safeRange(badRangeOver, for: string))

        // bad range, location inside string, length too big, i.e. "[0123"     ]
        let badRangeLong = NSRange(location: 0, length: stringLength + 1)
        let manualBadRangeLongSanitized = NSRange(location: 0, length: stringLength)
        XCTAssertEqual(manualBadRangeLongSanitized, utils.safeRange(badRangeLong, for: string))
    }

    func testStringContainsNumeral() {
        XCTAssertTrue(self.utils.stringContainsNumeral("0123456789"))
        XCTAssertFalse(self.utils.stringContainsNumeral(""))
        XCTAssertFalse(self.utils.stringContainsNumeral("one two three shitty ditty deee"))
        // we don't support Eastern Arabic numerals or Devanagari
        XCTAssertFalse(self.utils.stringContainsNumeral("٠, ١, ٢, ٣, ٤, ٥, ٦, ٧, ٨, ٩,०.१.२.३.४.५.६.७.८.९"))
    }

    func testRemovesNonNumeralsFromString() {
        XCTAssertEqual(self.utils.removeAllNonNumeral(from: "0123456789l;dD"), "0123456789")
        XCTAssertEqual(self.utils.removeAllNonNumeral(from: "0p1q2w3e4r5t6y7u8i9o"), "0123456789")
        XCTAssertEqual(self.utils.removeAllNonNumeral(from: "1"), "1")
        XCTAssertEqual(self.utils.removeAllNonNumeral(from: ""), "")
        XCTAssertEqual(self.utils.removeAllNonNumeral(from: "sdfsdf"), "")
    }

    func testProvidesNumericalCharacterSet() {
        // so it at least must include all the arabic numerals
        let minimalNumericalCharacterSet = NSCharacterSet(charactersIn: "0123456789")
        guard let numericalCharacterSet = self.utils.numericalCharacterSet() else { return XCTFail("Set should not be nil.") }
        let isSuperset = minimalNumericalCharacterSet.isSuperset(of: numericalCharacterSet)
        XCTAssertTrue(isSuperset)
    }

    func testShouldCountNumberOfNonNimericCharacters() {
        // position out of bounds
        XCTAssertEqual(self.utils.numberOfNonNumericComposedCharacters(in: "", beforePosition: 500), 0)

        let allNumerical = "0123456789"
        XCTAssertEqual(self.utils.numberOfNonNumericComposedCharacters(in: allNumerical, beforePosition: UInt(allNumerical.count)), 0)

        let oneNonNumerical = "0123y456789"
        // all length
        XCTAssertEqual(self.utils.numberOfNonNumericComposedCharacters(in: oneNonNumerical, beforePosition: UInt(oneNonNumerical.count)), 1)
        // before 3
        XCTAssertEqual(self.utils.numberOfNonNumericComposedCharacters(in: oneNonNumerical, beforePosition: 3), 0)
    }

    func testShouldRecognizeItsOwnBadRanges() {
        XCTAssertTrue(self.utils.isBadRange(self.utils.badRange()))
    }

    func testShouldCalculateCursorPosition() {
        let unformattedText = "123,56"
        let unformattedTextCursorPosition = 4 // i.e. cursor before 5
        let formattedText = "12,356"

        XCTAssertEqual(self.utils.correctedCursorPosition(
            UInt(unformattedTextCursorPosition),
            unformattedText: unformattedText,
            formattedText: formattedText
        ), 4)
    }

    // MARK: - Extending replacement range
    func testShouldExtendBehindCursorDeletionReplacementRange() {
        //
        //  Here we emulate deletion via backspace. User has it's cursor after non-numeral "," and hits backspace.
        //  We need to extend replacement range so it includes 3 as well, representing "3,".
        //
        let text = "123,456"
        let selectedRange = NSRange(location: 4, length: 0) // before 4
        let replacementRange = NSRange(location: 3, length: 1) // ","
        let replacementString = ""

        let extendedReplacementRange = self.utils.extendedReplacementRange(
            forText: text,
            range: replacementRange,
            selectedRange: selectedRange,
            replacementString: replacementString
        )
        let manualExtendedReplacementRange = NSRange(location: 2, length: 2)

        XCTAssertEqual(extendedReplacementRange, manualExtendedReplacementRange)
    }

    func testShouldExtendBeyoundCursorDeletionReplacementRange() {
        //
        //  Here we emulate front deletion beyound cursor. User has it's cursor before non-numeral "," and hits "front delete".
        //  We need to extend replacement range so it includes 4 as well, representing ",4".
        //
        let text = "123,456"
        let selectedRange = NSRange(location: 3, length: 0) // before ,
        let replacementRange = NSRange(location: 3, length: 1) // ","
        let replacementString = ""

        let extendedReplacementRange = self.utils.extendedReplacementRange(
            forText: text,
            range: replacementRange,
            selectedRange: selectedRange,
            replacementString: replacementString
        )
        let manualExtendedReplacementRange = NSRange(location: 3, length: 2)

        XCTAssertEqual(extendedReplacementRange, manualExtendedReplacementRange)
    }

    func testShouldNotExtendIfReplacedPartContainsNumeral() {
        //
        //  Here we emulate deletion behind cursor. User has it's cursor before "5" and hits backspace.
        //  We need don't need to extend replacement range because it already includes a numeral.
        //
        let text = "123,456"
        let selectedRange = NSRange(location: 5, length: 0) // before 5
        let replacementRange = NSRange(location: 4, length: 1) // "4"
        let replacementString = ""

        let extendedReplacementRange = self.utils.extendedReplacementRange(
            forText: text,
            range: replacementRange,
            selectedRange: selectedRange,
            replacementString: replacementString
        )
        let manualExtendedReplacementRange = NSRange(location: 4, length: 1)

        XCTAssertEqual(extendedReplacementRange, manualExtendedReplacementRange)
    }

    func testShouldNotExtendIfSelectedRangeIsNonEmpty() {
        //
        //  Here we emulate deletion of a selected range. User has "," selected and hits backspace.
        //  We need don't need to extend replacement range because user had selection (not single caret).
        //
        let text = "123,456"
        let selectedRange = NSRange(location: 3, length: 1) // selected ","
        let replacementRange = NSRange(location: 3, length: 1) // ","
        let replacementString = ""

        let extendedReplacementRange = self.utils.extendedReplacementRange(
            forText: text,
            range: replacementRange,
            selectedRange: selectedRange,
            replacementString: replacementString
        )
        let manualExtendedReplacementRange = NSRange(location: 3, length: 1)

        XCTAssertEqual(extendedReplacementRange, manualExtendedReplacementRange)
    }

    private let utils = YRETextFieldNumberDriverUtils()
}
