// Copyright 2022 Yandex LLC. All rights reserved.

@testable import IoT
import XCTest

class ActionTests: XCTestCase {
  func testActionLogin() {
    let state = AuthState()
    state.send(.login(promiseID: nil))
    let tmp: Loadable<Account?, AuthState.AuthRequest> = .pending(.login(promiseID: nil))
    XCTAssertEqual(state.currentAccountState.hash, tmp.hash)
  }

  func testActionInfo() {
    let state = AuthState()
    state.send(.login(promiseID: nil))
    state.send(.requestAccountInfo(promiseID: nil))
    let tmp: Loadable<Account?, AuthState.AuthRequest> = .pending(.info(promiseID: nil))
    XCTAssertEqual(state.currentAccountState.hash, tmp.hash)
  }

  func testActionLoginFailed() {
    let state = AuthState()
    state.send(.login(promiseID: nil))
    state.send(.loginFailed)
    let tmp: Loadable<Account?, AuthState.AuthRequest> = .loaded(nil)
    XCTAssertEqual(state.currentAccountState.hash, tmp.hash)
  }

  func testActionLogout() {
    let state = AuthState()
    state.send(.login(promiseID: nil))
    state.send(.logout(promiseID: nil))
    let tmp: Loadable<Account?, AuthState.AuthRequest> = .loaded(nil)
    XCTAssertEqual(state.currentAccountState.hash, tmp.hash)
  }
}
