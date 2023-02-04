//
//  AuthStateObservableMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Arkady Smirnov on 5/11/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREAppState
import YRECoreUtils
@testable import YREServiceLayer

final class AuthStateObservableMock: NSObject, AuthStateObservable {
    class AuthStateObservation: NSObject, StateObservationProtocol {
        func invalidate() {
            // do nothing
        }
    }

    var authState = AuthStateReaderMock()

    var authStateReader: YREAuthStateReader { self.authState }
    var observer: AuthStateObserver? = nil

    func observe(by observer: AuthStateObserver) -> StateObservationProtocol {
        self.observer = observer
        return AuthStateObservation()
    }

    func notifyObserver() {
        self.observer?.authStateObservableDidUpdate(self)
    }
}
