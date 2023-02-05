//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

@testable import YxSwissKnife

final class FakeDispatcher: SkDispatcher {

    private(set) var operations = [Operation]()

    func add(one operation: Operation) {
        self.operations.append(operation)
    }

    func add(all operations: [Operation]) {
        self.operations.append(contentsOf: operations)
    }

}
