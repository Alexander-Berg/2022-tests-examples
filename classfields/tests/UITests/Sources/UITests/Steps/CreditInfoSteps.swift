//
//  CreditInfoSteps.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 05.06.2020.
//

import XCTest
import Snapshots

class CreditInfoSteps: BaseSteps {
    func onCreditInfoScreen() -> CreditInfoScreen {
        return baseScreen.on(screen: CreditInfoScreen.self)
    }

    @discardableResult
    func validateSnapShot(accessibilityId: String, snapshotId: String, message: String = "") -> Self {
        return XCTContext.runActivity(named: message.isEmpty ? "Validate snapshot \(snapshotId)" : message) { _ in
            let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
            Snapshot.compareWithSnapshot(
                image: screenshot,
                identifier: snapshotId,
                overallTolerance: 0.005,
                ignoreEdges: UIEdgeInsets(top: 0, left: 0, bottom: 0, right: 16)
            )
            return self
        }
    }

    @discardableResult
    override func exist(selector: String) -> Self {
        let element = baseScreen.find(by: selector).firstMatch
        element.shouldExist(timeout: Const.timeout)
        return self
    }

    func tapOfferButton() -> PreliminaryCreditSteps {
        onCreditInfoScreen().offerButton.tap()
        return PreliminaryCreditSteps(context: context)
    }

    func openPreliminarySteps() -> PreliminaryCreditSteps {
        return PreliminaryCreditSteps(context: context)
    }
}
