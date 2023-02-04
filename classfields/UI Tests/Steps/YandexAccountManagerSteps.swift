//
//  Created by Alexey Aleshkov on 25/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

final class YandexAccountManagerSteps {
    @discardableResult
    func moduleIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что модуль авторизации отображён") { _ in
            let existsPredicate = NSPredicate.yreKeyPathValue(\XCUIElement.exists, .equalTo, true)
            let predicate = NSPredicate.yreClosure({ (object: (XCUIElement, XCUIElement)?, _) -> Bool in
                guard let object = object else { return false }
                let result = existsPredicate.evaluate(with: object.0) || existsPredicate.evaluate(with: object.1)
                return result
            })
            let objects = (self.accountsView, self.formViewParentView)
            let expectation = XCTNSPredicateExpectation(predicate: predicate, object: objects)
            let timeout = Consts.animationTimeout
            let result = predicate.evaluate(with: objects) || XCTWaiter.yreWait(for: [expectation], timeout: timeout)
            XCTAssert(result)
            return self
        }
    }

    @discardableResult
    func moduleIsDismissed() -> Self {
        XCTContext.runActivity(named: "Проверяем, что модуль авторизации не отображается") { _ in
            let notExistsPredicate = NSPredicate.yreKeyPathValue(\XCUIElement.exists, .equalTo, false)
            let predicate = NSPredicate.yreClosure({ (object: (XCUIElement, XCUIElement)?, _) -> Bool in
                guard let object = object else { return false }
                let result = notExistsPredicate.evaluate(with: object.0) && notExistsPredicate.evaluate(with: object.1)
                return result
            })
            let objects = (self.accountsView, self.formViewParentView)
            let expectation = XCTNSPredicateExpectation(predicate: predicate, object: objects)
            let timeout = Consts.animationTimeout
            let result = predicate.evaluate(with: objects) || XCTWaiter.yreWait(for: [expectation], timeout: timeout)
            XCTAssert(result)

            return self
        }
    }

    func isFormPresented() -> Bool {
        let result = self.formViewCompanyLabel
            .yreWaitForExistence(timeout: Constants.timeout)
        return result
    }

    func isAccountsPresented() -> Bool {
        let result = self.accountsViewAddAccountButton
            .yreWaitForExistence(timeout: Constants.timeout)
        return result
    }

    @discardableResult
    func tapOnAddAccount() -> Self {
        self.accountsViewAddAccountButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnShroud() -> Self {
        self.accountsViewShroudView
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func formIsPresented() -> Self {
        self.formViewCompanyLabel
            .yreEnsureExistsWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func formLoginIsPresented() -> Self {
        self.formViewLoginField
            .yreEnsureHittableWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func fillLogin(_ login: String) -> Self {
        self.formViewLoginField
            .yreEnsureExists()
            .yreForceTap()
            .yreTypeText(login)

        return self
    }

    @discardableResult
    func formPasswordIsPresented() -> Self {
        self.formViewPasswordField
            .yreEnsureHittableWithTimeout(timeout: Consts.animationTimeout)
        return self
    }

    @discardableResult
    func fillPassword(_ password: String) -> Self {
        self.formViewPasswordField
            .yreEnsureExists()
            .yreForceTap()
            .yreTypeText(password)

        return self
    }

    @discardableResult
    func tapOnNextButton() -> Self {
        self.formViewNextButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    @discardableResult
    func tapOnFormCloseButton() -> Self {
        self.formViewCloseButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    // MARK: Private

    private enum Identifiers {
        static let chooseAccountLabel = "Выберите аккаунт"
        static let addAccountLabel = "Добавить аккаунт"

        static let formViewCloseButton = "close light"
        static let formViewCompanyLabel = "Яндекс"
        static let loginTextField = "edit_login"
        static let passwordTextField = "edit_password"
        static let nextButton = "button_next"
    }

    private enum Consts {
        static let animationTimeout: TimeInterval = 30
    }

    private lazy var transitionView = XCUIApplication()
        .windows.element
        .children(matching: .other).element(boundBy: 1) // UITransitionView

    private lazy var transitionSubview = self.transitionView
        .children(matching: .other).element

    private lazy var formViewParentView = self.transitionView
        .children(matching: .other).element // UILayoutContainerView
        .children(matching: .other).element // UINavigationTransitionView
        .children(matching: .other).element // UIViewControllerWrapperView
        // .children(matching: .other).element // YALStartViewController.view / YALPasswordViewController.view
        // .children(matching: .other).element(boundBy: 1) // YALStartView / YALPasswordView

    private lazy var formViewCloseButton = XCUIApplication()
        .navigationBars
        .buttons[Identifiers.formViewCloseButton]

    private lazy var formViewCompanyLabel = self.formViewParentView
        .staticTexts[Identifiers.formViewCompanyLabel]

    private lazy var formViewLoginField = self.formViewParentView
        .textFields[Identifiers.loginTextField]

    private lazy var formViewPasswordField = self.formViewParentView
        .secureTextFields[Identifiers.passwordTextField]

    private lazy var formViewNextButton = self.formViewParentView
        .buttons[Identifiers.nextButton]

    private lazy var accountsViewShroudView = self.transitionView
        .children(matching: .other).element // YALAccountsSlideViewController.view
        .children(matching: .other).element // Shroud

    private lazy var accountsView = self.accountsViewShroudView
        .children(matching: .other).element // Screen
        .children(matching: .other).element(boundBy: 2) // YALAccountsSlideView

    private lazy var accountsViewChooseAccountText = self.accountsView
        .staticTexts[Identifiers.chooseAccountLabel]

    private lazy var accountsViewAddAccountButton = self.accountsView
        .buttons[Identifiers.addAccountLabel]
}

extension YandexAccountManagerSteps {
    func makeActivity() -> YandexAccountManagerStepsActivity {
        return .init(self)
    }
}

final class YandexAccountManagerStepsActivity {
    init(_ steps: YandexAccountManagerSteps) {
        self.steps = steps
    }

    @discardableResult
    func fillForm(login: String, password: String) -> Self {
        self.login = login
        self.password = password
        self.isSuccess = true

        return self
    }

    @discardableResult
    func cancel() -> Self {
        self.isSuccess = false

        return self
    }

    @discardableResult
    func run() -> Self {
        let isSuccess = YREUnwrap(self.isSuccess)

        XCTContext.runActivity(named: "Yandex.AccountManager Story", block: { _ -> Void in
            if isSuccess {
                self.performSubmit()
            }
            else {
                self.performCancel()
            }
        })

        return self
    }

    private let steps: YandexAccountManagerSteps

    private var isSuccess: Bool?

    private var login: String?
    private var password: String?

    private func performSubmit() {
        let login = YREUnwrap(self.login)
        let password = YREUnwrap(self.password)

        self.steps
            .moduleIsPresented()

        if self.steps.isAccountsPresented() {
            self.steps
                .tapOnAddAccount()
        }
        self.steps
            .formIsPresented()
            .formLoginIsPresented()
            .fillLogin(login)
            .tapOnNextButton()
            .formPasswordIsPresented()
            .fillPassword(password)
            .tapOnNextButton()
            .moduleIsDismissed()
    }

    private func performCancel() {
        self.steps
            .moduleIsPresented()

        if self.steps.isFormPresented() {
            self.steps
                .tapOnFormCloseButton()
        }
        if self.steps.isAccountsPresented() {
            self.steps
                .tapOnShroud()
        }

        self.steps
            .moduleIsDismissed()
    }
}
