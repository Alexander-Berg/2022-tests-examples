import XCTest
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuGarage AutoRuNewLogin
final class AuthFromGarageTabTests: BackendStatefulTests {
    func testSuccessfulLogIn() {
        state.user.authorized = false

        launchMain()
            .toggle(to: \.garageLanding)
            .tap(.addToGarageHeaderButton)
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(.addToGarageHeaderButton, .be(.hidden))

        XCTAssertTrue(state.user.authorized)
    }

    func testLogInFailedDueInvalidCode() {
        state.user.authorized = false

        launchMain()
            .toggle(to: \.garageLanding)
            .tap(.addToGarageHeaderButton)
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                self.expectedFailures.wrongCode = true

                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen
                    .type("1234", in: .codeInput)
                    .should(.invalidCodeError, .exist)
            }

        XCTAssertFalse(state.user.authorized)
    }
}
