import XCTest
import VoxImplantSDK
@testable import AutoRuCallsCore
import Foundation

final class AuthStateTests: BaseUnitTest {
    private var currentIteration = 0
    private var state = AuthState.notDetermined

    private var userID: VoxImplantUserID!
    private var loginKey: String!
    private var ott: String!
    private var authParams: AuthParams!
    private let unknownError = NSError(domain: "test", code: 1111, userInfo: nil)

    private var ottAuthRequest: AuthState.AuthRequest {
        AuthState.AuthRequest(userID: userID, kind: .ott(ott))
    }

    override func setUp() {
        super.setUp()

        currentIteration = 0
        state = AuthState.notDetermined
        userID = VoxImplantUserID(name: "test", app: "app")
        loginKey = "key"
        ott = "one time token"
        authParams = AuthParams(
            accessToken: "accessToken",
            accessExpire: Date(),
            refreshToken: "refreshToken",
            refreshExpire: Date()
        )
    }

    func testNormalAuth() {
        react(on: .authStatusChanged(authorized: true), expectedCommands: [.fetchUserID])
        react(on: .voxUserIDFetched(userID, currentIteration), expectedCommands: [.fetchLoginKey(userID: userID)])
        react(on: .loginKeyFetched(loginKey, currentIteration), expectedCommands: [.fetchOneTimeToken(loginKey: loginKey)])
        react(on: .oneTimeTokenFetched(ott, currentIteration), expectedCommands: [.authorizeVoxUser(ottAuthRequest)])
        react(on: .voxUserAuthorized(authParams: authParams, currentIteration),
              expectedCommands: [.setLogInResult(AuthState.LogInResult.loggedIn(userID: userID, authParams))]
        )

        if case .authorized(userID: userID, authParams) = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testAutoRuUserLogInDuringLoggingOut() {
        moveToAuthorizedState()
        react(on: .authStatusChanged(authorized: false))

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }

        react(on: .authStatusChanged(authorized: true),
              expectedCommands: [.logOutVoxUser(userID: userID), .fetchUserID]
        )

        react(on: .prepareToLogOutCompleted(currentIteration), expectedCommands: [])

        if case .waitingUserID = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testAutoRuUserAlreadyAuthorized() {
        moveToAuthorizedState()

        react(on: .authStatusChanged(authorized: true), expectedCommands: [])
    }

    // MARK: log out tests

    func testLogOut() {
        moveToAuthorizedState()
        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )
        react(on: .prepareToLogOutCompleted(currentIteration), expectedCommands: [.logOutVoxUser(userID: userID)])
        react(on: .voxUserLoggedOut(currentIteration), expectedCommands: [])

        if case .notAuthorized = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringWaitingLoginKey() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutAfterLoginKeyFailure() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .fetchLoginKeyFailed(.networkError, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringWaitingUserID() {
        react(on: .authStatusChanged(authorized: true))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized))]
        )

        if case .notAuthorized = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringWaitingOTT() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutAfterFetchingOTTFailure() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .fetchOneTimeTokenFailed(.networkError, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringWaitingVoxAuthorization() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringAuthorized() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))
        react(on: .voxUserAuthorized(authParams: authParams, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutAfterAuthorizationFailure() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))
        react(on: .voxUserAuthorizationFailed(.networkError, currentIteration))

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized)), .prepareToLogOut(userID: userID)]
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringLoggingOut() {
        moveToAuthorizedState()
        react(on: .authStatusChanged(authorized: false))

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: []
        )

        if case .preparingToLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogOutDuringWaitingForLogOut() {
        moveToAuthorizedState()
        react(on: .authStatusChanged(authorized: false))
        react(on: .prepareToLogOutCompleted(currentIteration))

        if case .waitingForLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }

        react(on: .authStatusChanged(authorized: false),
              expectedCommands: []
        )

        if case .waitingForLogOut = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    // MARK: test network become available

    func testNetworkBecomeAvailableDuringWaitingUserID() {
        react(on: .authStatusChanged(authorized: true))

        let expectedIteration = currentIteration

        react(on: .networkBecomeAvailable, expectedCommands: [])

        XCTAssertTrue(state.retryIfNeeded)

        react(on: .fetchUserIDFailed(.networkError, expectedIteration), expectedCommands: [.fetchUserID])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingUserID = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableAfterFetchingUserIDFailed() {
        react(on: .authStatusChanged(authorized: true))

        let expectedIteration = currentIteration

        react(on: .fetchUserIDFailed(.networkError, expectedIteration),
              expectedCommands: [.setLogInResult(.authFailure(.fetchingUserIDFailed(.networkError)))]
        )

        react(on: .networkBecomeAvailable, expectedCommands: [.fetchUserID])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingUserID = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableDuringWaitingLoginKey() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))

        let expectedIteration = currentIteration

        react(on: .networkBecomeAvailable, expectedCommands: [])

        XCTAssertTrue(state.retryIfNeeded)

        react(on: .fetchLoginKeyFailed(.networkError, expectedIteration), expectedCommands: [.fetchLoginKey(userID: userID)])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingLoginKey = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableAfterFetchingLoginKeyFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))

        let expectedIteration = currentIteration

        react(on: .fetchLoginKeyFailed(.networkError, expectedIteration),
              expectedCommands: [.setLogInResult(.authFailure(.fetchingLoginKeyFailed(.networkError)))]
        )

        react(on: .networkBecomeAvailable, expectedCommands: [.fetchLoginKey(userID: userID)])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingLoginKey = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableDuringWaitingOtt() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))

        let expectedIteration = currentIteration

        react(on: .networkBecomeAvailable, expectedCommands: [])

        XCTAssertTrue(state.retryIfNeeded)

        react(on: .fetchOneTimeTokenFailed(.networkError, expectedIteration), expectedCommands: [.fetchOneTimeToken(loginKey: loginKey)])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingOneTimeToken = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableAfterFetchingOttFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))

        let expectedIteration = currentIteration

        react(on: .fetchOneTimeTokenFailed(.networkError, expectedIteration),
              expectedCommands: [.setLogInResult(.authFailure(.fetchingOneTimeTokenFailed(.networkError)))]
        )

        react(on: .networkBecomeAvailable, expectedCommands: [.fetchOneTimeToken(loginKey: loginKey)])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingOneTimeToken = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableDuringWaitingVoxUserAuthorization() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))

        let expectedIteration = currentIteration

        react(on: .networkBecomeAvailable, expectedCommands: [])

        XCTAssertTrue(state.retryIfNeeded)

        react(on: .voxUserAuthorizationFailed(.networkError, expectedIteration),
              expectedCommands: [.authorizeVoxUser(ottAuthRequest)]
        )

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingVoxUserAuthorization = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testNetworkBecomeAvailableAfterVoxUserAuthorizationFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))

        let expectedIteration = currentIteration

        react(on: .voxUserAuthorizationFailed(.networkError, expectedIteration),
              expectedCommands: [.setLogInResult(.authFailure(.voxUserAuthorizationFailed(.networkError)))]
        )

        react(on: .networkBecomeAvailable, expectedCommands: [.authorizeVoxUser(ottAuthRequest)])

        XCTAssertFalse(state.retryIfNeeded)

        if case .waitingVoxUserAuthorization = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    // MARK: test log in request handling

    func testLogInRequestWhenAuthorized() {
        moveToAuthorizedState()

        react(on: .logInRequest,
              expectedCommands: [.authorizeVoxUser(.init(userID: userID, kind: .authParams(authParams)))]
        )

        if case .waitingVoxUserAuthorization = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenFetchLoginKeyFailedDueInvalidUsername() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))

        react(on: .fetchLoginKeyFailed(.invalidUsername, currentIteration))

        var counter = 0
        loop: while(true) {
            if counter > 100 {
                XCTFail("infinite loop detected")
                break
            }

            counter += 1

            switch state.state {
            case .waitingUserID:
                react(on: .voxUserIDFetched(userID, currentIteration))

            case .waitingLoginKey:
                react(on: .fetchLoginKeyFailed(.invalidUsername, currentIteration))

            case .fetchLoginKeyFailed:
                break loop

            default:
                break
            }
        }

        react(on: .logInRequest,
              expectedCommands: [.fetchUserID]
        )

        if case .waitingUserID = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenFetchLoginKeyFailedDueConnectionError() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))

        react(on: .fetchLoginKeyFailed(.connectionError, currentIteration))

        var counter = 0
        loop: while(true) {
            if counter > 100 {
                XCTFail("infinite loop detected")
                break
            }

            counter += 1

            switch state.state {
            case .waitingUserID:
                react(on: .voxUserIDFetched(userID, currentIteration))

            case .waitingLoginKey:
                react(on: .fetchLoginKeyFailed(.connectionError, currentIteration))

            case .fetchLoginKeyFailed:
                break loop

            default:
                break
            }
        }

        react(on: .logInRequest, expectedCommands: [.fetchLoginKey(userID: userID)])

        if case .waitingLoginKey = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenFetchOttFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))

        react(on: .fetchOneTimeTokenFailed(.connectionError, currentIteration))

        var counter = 0
        loop: while(true) {
            if counter > 100 {
                XCTFail("infinite loop detected")
                break
            }

            counter += 1

            switch state.state {
            case .waitingUserID:
                react(on: .voxUserIDFetched(userID, currentIteration))

            case .waitingLoginKey:
                react(on: .loginKeyFetched(loginKey, currentIteration))

            case .waitingOneTimeToken:
                react(on: .fetchOneTimeTokenFailed(.connectionError, currentIteration))

            case .fetchOneTimeTokenFailed:
                break loop

            default:
                break
            }
        }

        react(on: .logInRequest, expectedCommands: [.fetchLoginKey(userID: userID)])

        if case .waitingLoginKey = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenVoxAuthorizationFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))

        react(on: .voxUserAuthorizationFailed(.connectionError, currentIteration))

        var counter = 0
        loop: while(true) {
            if counter > 100 {
                XCTFail("infinite loop detected")
                break
            }

            counter += 1

            switch state.state {
            case .waitingUserID:
                react(on: .voxUserIDFetched(userID, currentIteration))

            case .waitingLoginKey:
                react(on: .loginKeyFetched(loginKey, currentIteration))

            case .waitingOneTimeToken:
                react(on: .oneTimeTokenFetched(ott, currentIteration))

            case .waitingVoxUserAuthorization:
                react(on: .voxUserAuthorizationFailed(.connectionError, currentIteration))

            case .voxUserAuthorizationFailed:
                break loop

            default:
                break
            }
        }

        react(on: .logInRequest, expectedCommands: [.fetchLoginKey(userID: userID)])

        if case .waitingLoginKey = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenFetchingUserIdFailed() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .fetchUserIDFailed(.unknown(unknownError), currentIteration))

        react(on: .logInRequest, expectedCommands: [.fetchUserID])

        if case .waitingUserID = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    func testLogInRequestWhenUserIsNotAuthorized() {
        moveToAuthorizedState()
        react(on: .authStatusChanged(authorized: false))
        react(on: .prepareToLogOutCompleted(currentIteration))
        react(on: .voxUserLoggedOut(currentIteration))

        react(on: .logInRequest, expectedCommands: [.setLogInResult(.authFailure(.userIsNotAuthorized))])
    }

    func testLoggingInAfterAutoRuUserAuthorizationByCachedAuth() {
        state = .authorized(userID: userID, authParams)

        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserAuthorized(authParams: authParams, currentIteration), expectedCommands: [
            .setLogInResult(.loggedIn(userID: userID, authParams))
        ])

        if case .authorized = state.state {
        } else {
            XCTFail("unexpected state")
        }
    }

    private func react(on event: AuthState.Event, expectedCommands: [AuthState.Command]) {
        let commands = state.react(on: event, currentIteration: increaseCurrentIteration())
        XCTAssertEqual(expectedCommands, commands)
    }

    private func react(on event: AuthState.Event) {
        _ = state.react(on: event, currentIteration: increaseCurrentIteration())
    }

    private func moveToAuthorizedState() {
        react(on: .authStatusChanged(authorized: true))
        react(on: .voxUserIDFetched(userID, currentIteration))
        react(on: .loginKeyFetched(loginKey, currentIteration))
        react(on: .oneTimeTokenFetched(ott, currentIteration))
        react(on: .voxUserAuthorized(authParams: authParams, currentIteration))
    }

    private func increaseCurrentIteration() -> Int {
        currentIteration += 1
        return currentIteration
    }
}
