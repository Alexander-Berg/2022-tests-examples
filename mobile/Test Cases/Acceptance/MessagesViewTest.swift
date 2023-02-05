//
//  Created by Timur Turaev on 21/03/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class MessagesViewTest: YOTestCase {
    func testReact() {
        Allure.start(feature: .mailView, subFeature: .react, testPalmCaseID: 3072)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser2)
                .then { $0.openMessage(withIndex: 1) }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
