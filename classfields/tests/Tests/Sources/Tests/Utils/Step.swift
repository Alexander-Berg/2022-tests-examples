import XCTest

func Step(_ text: String, step: () -> Void) {
    XCTContext.runActivity(named: text) { _ in step() }
}

func Step(_ text: String) {
    XCTContext.runActivity(named: text) { _ in }
}
