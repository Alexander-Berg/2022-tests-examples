//
//  TextInputTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 23.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
@testable import class YREDesignKit.TextInputView
@testable import class YREDesignKit.CodeInputView

final class TextInputTests: XCTestCase {

    // MARK: - TextInputView

    func testTextInputEmpty() {
        let textInput = TextInputView(shouldShowClearButton: false)
        textInput.placeholderText = "Placeholder"
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    func testTextInputEmptyCleanable() {
        let textInput = TextInputView(shouldShowClearButton: true)
        textInput.placeholderText = "Placeholder"
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    func testTextInputEmptyLocked() {
        let textInput = TextInputView(shouldShowClearButton: false)
        textInput.placeholderText = "Placeholder"
        textInput.isDisabled = true
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    func testTextInputFilled() {
        let textInput = TextInputView(shouldShowClearButton: false)
        textInput.placeholderText = "Placeholder"
        textInput.text = "Some text"
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    func testTextInputFilledCleanable() {
        let textInput = TextInputView(shouldShowClearButton: true)
        textInput.placeholderText = "Placeholder"
        textInput.text = "Some text"
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    func testTextInputFilledLocked() {
        let textInput = TextInputView(shouldShowClearButton: false)
        textInput.placeholderText = "Placeholder"
        textInput.text = "Some text"
        textInput.isDisabled = true
        textInput.frame = Self.frame { TextInputView.size(for: $0).height }
        self.assertSnapshot(textInput)
    }

    // MARK: - CodeInputView

    func testCodeInputEmpty() {
        let configuration = CodeInputView.Configuration(length: 4, title: nil)
        let codeInput = CodeInputView()
        codeInput.configure(with: configuration)
        codeInput.frame = Self.frame { CodeInputView.size(for: $0, configuration: configuration).height }
        self.assertSnapshot(codeInput)
    }

    func testCodeInputFilled() {
        let configuration = CodeInputView.Configuration(length: 4, title: nil)
        let codeInput = CodeInputView()
        codeInput.configure(with: configuration)
        codeInput.code = "3478"
        codeInput.frame = Self.frame { CodeInputView.size(for: $0, configuration: configuration).height }
        self.assertSnapshot(codeInput)
    }

    func testCodeInputParticiallyFilled() {
        let configuration = CodeInputView.Configuration(length: 5, title: nil)
        let codeInput = CodeInputView()
        codeInput.configure(with: configuration)
        codeInput.code = "254"
        codeInput.frame = Self.frame { CodeInputView.size(for: $0, configuration: configuration).height }
        self.assertSnapshot(codeInput)
    }
}
