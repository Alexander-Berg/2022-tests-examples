//  Created by Denis Malykh on 20.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkDisposeOneTests: XCTestCase {

    func testDisposal() {
        let spies = [SktDisposableSpy(), SktDisposableSpy()]
        performDisposal(spies)
        spies.forEach { XCTAssertEqual($0.disposeCalledTimes, 1) }
    }

    private func performDisposal(_ disposables: [SkDisposable]) {
        let one = SkDisposeOne()
        disposables.forEach { one.value = $0 }
    }

}
