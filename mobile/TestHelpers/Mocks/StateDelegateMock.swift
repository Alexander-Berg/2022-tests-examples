//
//  StateDelegateMock.swift
//  YandexDiskTests
//
//  Created by Mariya Kachalova on 25/12/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

@testable import YandexDisk

final class StateDelegateMock: NSObject, YXStateDelegate {
    private(set) var finishedEventName: String?
    private(set) var finishedError: Error?
    private(set) var startCalled = false
    private(set) var cancelCalled = false

    func stateStarted(_: YXState) {
        startCalled = true
    }

    func stateFinished(_: YXState, eventName: String, error: Error?) {
        finishedEventName = eventName
        finishedError = error
    }

    func stateCancelled(_: YXState) {
        cancelCalled = true
    }
}
