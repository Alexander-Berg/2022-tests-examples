//
//  ReportSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 15.06.2020.
//

import XCTest
import Snapshots

class ReportCreditSteps: BaseSteps {
    func onReportScreen() -> ReportScreen {
        return baseScreen.on(screen: ReportScreen.self)
    }

    func openCredit() -> PreliminaryCreditSteps {
        onReportScreen().creditButton.tap()
        return PreliminaryCreditSteps(context: context)
    }

    func openDraftCredit() -> PreliminaryCreditSteps {
        onReportScreen().draftCreditButton.tap()
        return PreliminaryCreditSteps(context: context)
    }

    func openActiveCredit() -> PreliminaryCreditSteps {
        onReportScreen().draftActiveButton.tap()
        return PreliminaryCreditSteps(context: context)
    }

    @discardableResult
    func swipeUp() -> Self {
        onReportScreen().swipe(.up)
        return self
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16))
            return self
        }
    }

}
