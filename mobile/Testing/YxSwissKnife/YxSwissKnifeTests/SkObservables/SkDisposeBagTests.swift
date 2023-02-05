//  Created by Denis Malykh on 20.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkDisposeBagTests: XCTestCase {

    func testAdditionAndDisposing() {
        let spy = SktDisposableSpy()
        performDisposal(disposable: spy)
        XCTAssertEqual(spy.disposeCalledTimes, 1)
    }

    func testArraysAdditionAndDisposing() {
        let spies = [SktDisposableSpy(), SktDisposableSpy()]
        performDisposal(disposables: spies)
        spies.forEach { XCTAssertEqual($0.disposeCalledTimes, 1) }
    }

    private func performDisposal(disposable: SkDisposable) {
        let bag = SkDisposeBag()
        disposable.disposed(by: bag)
    }

    private func performDisposal(disposables: [SkDisposable]) {
        let bag = SkDisposeBag()
        disposables.disposed(by: bag)
    }

}
