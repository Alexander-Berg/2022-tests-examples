// Copyright 2022 Yandex LLC. All rights reserved.

@testable import IoT
import XCTest

class AuthStateTests: XCTestCase {
  func testIOTSchemesState() {
    XCTAssert((try XCTUnwrap(Bundle.main.iotSchemes)).count > 0)
  }

  func testAuthNilState() {
    let state = AuthState()
    XCTAssertNil(state.currentAccount)
  }

  func testAuthUpdateState() {
    let state = AuthState()
    let account = Account.stubAccount
    state.send(.updateAccount(currentAccount: account))
    XCTAssertEqual(try XCTUnwrap(state.currentAccount), account)
  }

  func testAuthLogoutState() {
    let state = AuthState()
    let account = Account.stubAccount
    state.send(.updateAccount(currentAccount: account))
    XCTAssertEqual(try XCTUnwrap(state.currentAccount), account)
    state.send(.logout(promiseID: nil))
    XCTAssertNil(state.currentAccount)
  }

  func testAuthLoginState() {
    let state = AuthState()
    state.send(.login(promiseID: nil))
    XCTAssertNil(state.currentAccount)
  }
}
