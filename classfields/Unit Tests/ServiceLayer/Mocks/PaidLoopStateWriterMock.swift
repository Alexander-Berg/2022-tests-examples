//
//  PaidLoopStateWriterMock.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 10.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREModel
import YREAppState

final class PaidLoopStateWriterMock: NSObject, YREPaidLoopStateWriter {
    enum Event: String {
        case updatePaidLoopAdSourceCall
    }

    private(set) var paidLoopInfo: PaidLoopInfo?
    private(set) var events: [Event] = []

    func update(_ paidLoopInfo: PaidLoopInfo?) {
        self.paidLoopInfo = paidLoopInfo
        self.events.append(.updatePaidLoopAdSourceCall)
    }
}
