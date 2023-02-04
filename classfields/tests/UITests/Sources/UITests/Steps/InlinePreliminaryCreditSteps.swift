//
//  PreliminaryCreditSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 01.06.2020.
//

import XCTest
import Snapshots

class InlinePreliminaryCreditSteps: BaseSteps {

    func onPreliminaryCreditScreen() -> PreliminaryCreditScreen {
        return baseScreen.on(screen: PreliminaryCreditScreen.self)
    }

    func enterFio(_ text: String) -> Self {
        onPreliminaryCreditScreen().fio.tap()
        app.typeText(text)
        return self
    }

    func enterEmail(_ text: String) -> Self {
        onPreliminaryCreditScreen().email.tap()
        app.typeText(text)
        return self
    }
    func enterPhone(_ text: String) -> Self {
        onPreliminaryCreditScreen().phone.tap()
        app.typeText(text)
        return self
    }

    @discardableResult
    func enterLoginCode(_ text: String) -> Self {
        onPreliminaryCreditScreen().login.tap()
        app.typeText(text)
        return self
    }

    func tapDone() -> Self {
        onPreliminaryCreditScreen().done.tap()
        return self
    }

    @discardableResult
    func tapSubmit() -> Self {
        onPreliminaryCreditScreen().submitButton.tap()
        return self
    }

    func tapAgreement() -> Self {
        onPreliminaryCreditScreen().agreement.tap()
        return self
    }

    func tapOnSubAgreement() -> Self {
        let normalized = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let coordinate = normalized.withOffset(CGVector(dx: 32, dy: 763))
        coordinate.tap()
        return self
    }

    @discardableResult
    func tapOnCloseWebView() -> Self {
        let normalized = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let coordinate = normalized.withOffset(CGVector(dx: 395, dy: 75))
        coordinate.tap()
        return self
    }

    func close() -> Self {
        onPreliminaryCreditScreen().closeButton.tap()
        return self
    }

    @discardableResult
    func validateSnapShot(from: String, to: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let fromElement = self.onPreliminaryCreditScreen().cellFor(from)
            let toElement = self.onPreliminaryCreditScreen().cellFor(to)
            let image = Snapshot.screenshotCollectionView(fromCell: fromElement, toCell: toElement, windowInsets: .init(top: 50, left: 0, bottom: 150, right: 0), timeout: 10)
            Snapshot.compareWithSnapshot(image: image, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16))
            return self
        }
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 32, left: 0, bottom: 32, right: 0), file: "PreliminaryCreditTests")
            return self
        }
    }

    @discardableResult
    override func exist(selector: String) -> Self {
        let element = baseScreen.find(by: selector).firstMatch
        element.shouldExist(timeout: Const.timeout)
        return self
    }
}
