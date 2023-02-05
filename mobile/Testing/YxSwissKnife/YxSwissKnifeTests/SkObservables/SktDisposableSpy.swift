//  Created by Denis Malykh on 20.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation
import YxSwissKnife

final class SktDisposableSpy: SkDisposable {

    public private(set) var disposeCalledTimes = 0

    func dispose() {
        disposeCalledTimes += 1
    }
}
