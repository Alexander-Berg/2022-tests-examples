@testable import YREDesignKit
import XCTest

final class SlidingInputViewTextInputFormatterTests: XCTestCase {
    // Deleting
    func testDeletingDigit() {
        let formattedValue = self.formatter.formatInput(
            currentText: "7 654 321",
            range: NSRange(location: 3, length: 1),
            replacementString: ""
        )
        let expectedFomattedValue = SlidingInputView.TextInputFormatter.FormattedTextValue(formattedText: "764 321", caretBeginOffset: 2)

        XCTAssertEqual(formattedValue, expectedFomattedValue)
    }

    func testDeletingDigitAfterWhitespace() {
        let formattedValue = self.formatter.formatInput(
            currentText: "7 654 321",
            range: NSRange(location: 6, length: 1),
            replacementString: ""
        )
        let expectedFomattedValue = SlidingInputView.TextInputFormatter.FormattedTextValue(formattedText: "765 421", caretBeginOffset: 5)

        XCTAssertEqual(formattedValue, expectedFomattedValue)
    }

    func testDeletingWhitespace() {
        let formattedValue = self.formatter.formatInput(
            currentText: "7 654 321",
            range: NSRange(location: 5, length: 1),
            replacementString: ""
        )

        let expectedFomattedValue = SlidingInputView.TextInputFormatter.FormattedTextValue(formattedText: "7 654 321", caretBeginOffset: 5)

        XCTAssertEqual(formattedValue, expectedFomattedValue)
    }

    // MARK: - Private
    private let formatter = SlidingInputView.TextInputFormatter { (text: String) -> String? in
        func uIntValue(from text: String) -> UInt? {
            let digits = CharacterSet.decimalDigits
            let textWithOnlyDigits = text.yre_replaceCharacters(from: digits.inverted, replacementString: "")
            guard textWithOnlyDigits.isEmpty == false else { return nil }
            return UInt(textWithOnlyDigits)
        }

        guard var intValue = uIntValue(from: text) else { return nil }

        let number = NSDecimalNumber(value: intValue)

        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 0
        formatter.usesGroupingSeparator = true
        formatter.groupingSeparator = " "
        formatter.groupingSize = 3
        formatter.notANumberSymbol = ""
        let formattedText = formatter.string(from: number)
        return formattedText
    }
}
