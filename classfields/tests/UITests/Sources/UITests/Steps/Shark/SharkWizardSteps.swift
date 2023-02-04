//
//  SharkWizardSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 10.02.2021.
//

import XCTest
import Snapshots

class SharkWizardSteps: BaseSteps {
    enum SuggestAction {
        case selectAndNext(index: Int)
        case select(index: Int)
    }

    var screen: SharkWizardScreen {
        return baseScreen.on(screen: SharkWizardScreen.self)
    }

    func fillField(_ id: SharkWizardScreen.FieldID, value: String, suggestAction: SuggestAction? = nil, skipScreenValidation: Bool = false, prefix: String = "", file: StaticString = #filePath) -> Self {
        return step("Заполняем поле \(prefix + (prefix.isEmpty ? "" : "_"))\(id.rawValue) значением - \(value)") {
            screen.scrollTo(element: screen.field(id))
            screen.field(id).tap()

            if !value.isEmpty {
                screen.fieldTextView(id).clearAndEnterText("\(value)")
            }
            func tapNext() {
                if screen.nextButton.exists {
                    screen.nextButton.tap()
                } else {
                    screen.saveButton.tap()
                }
            }
            if let suggestAction = suggestAction {
                switch suggestAction {
                case .selectAndNext(let suggestIndex):
                    screen.find(by: "suggest_\(suggestIndex)").firstMatch.tap()
                    tapNext()
                case .select(let suggestIndex):
                    screen.find(by: "suggest_\(suggestIndex)").firstMatch.tap()
                }
            } else {
                tapNext()
            }
            if !skipScreenValidation {
                validateSnapshot(of: screen.field(id),
                                 ignoreEdges: .init(top: 0, left: 0, bottom: 16, right: 0),
                                 file: file,
                                 snapshotId: "Field \(prefix)_\(id.rawValue) with value - \(value)")
            }
        }
    }

    func selectSuggest(_ suggestIndex: Int) -> Self {
        screen.find(by: "suggest_\(suggestIndex)").firstMatch.tap()
        return self
    }

    @discardableResult
    func select(_ option: SharkWizardScreen.SelectableOption, forceTap: Bool = false) -> Self {
        if forceTap {
            screen.find(by: option.rawValue).firstMatch.forceTap()
        } else {
            screen.find(by: option.rawValue).firstMatch.tap()
        }
        return self
    }

    @discardableResult
    func checkScreen(_ screenName: String) -> Self {
        validateSnapshot(of: "WizardViewController", ignoreEdges: .init(top: 64, left: 0, bottom: 0, right: 0), snapshotId: screenName)
        return self
    }

    @discardableResult
    func pressNext() -> Self {
        screen.nextButton.tap()
        return self
    }

    @discardableResult
    func pressSave() -> Self {
        screen.saveButton.tap()
        return self
    }

    func pressSkip() -> Self {
        screen.find(by: "Позже").firstMatch.tap()
        return self
    }

    func scrollTo(_ selector: String) -> Self {
        screen.scrollTo(element: screen.find(by: selector).firstMatch, windowInsets: .zero)
        return self
    }

    func swipe() -> Self {
        screen.swipe(.up)
        return self
    }
}
