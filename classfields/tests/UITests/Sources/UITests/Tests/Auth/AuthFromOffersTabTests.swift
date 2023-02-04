import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

final class AuthFromOffersTabTests: BackendStatefulTests {
    func testSuccessfulLogIn() {
        state.user.authorized = false

        launchMain()
            .container
            .focus(on: .tabBar, ofType: .tabBar) {
                $0.tap(.tab(.offersAttentions))
            }
            .should(provider: .loginScreen, .exist)
            .focus { screen in
                screen.type("71234567890", in: .phoneInput)
            }
            .should(provider: .codeInputScreen, .exist)
            .focus { screen in
                screen.type("1234", in: .codeInput)
            }
            .should(provider: .userSaleListScreen, .exist)
            .focus({ screen in
                screen.should(.logInLabel, .be(.hidden))
                screen.should(.placeFreeLabel, .exist)
            })

        XCTAssertTrue(state.user.authorized)
    }

    func testLogInFailedDueInvalidCode() {
        state.user.authorized = false

        launchMain()
            .toggle(to: \.offersAttentions)
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
