//  Created by Denis Malykh on 26.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkSubscriptionTokenTests: XCTestCase {

    func testToken() {
        var disposerCalled = 0
        var tokenForDisposed: SkSubscriptionToken?
        let token = SkSubscriptionToken(disposer: { token in
            disposerCalled += 1
            tokenForDisposed = token
        })
        token.dispose()
        token.dispose()
        XCTAssertEqual(disposerCalled, 1)
        XCTAssertEqual(token, tokenForDisposed)
    }

}
