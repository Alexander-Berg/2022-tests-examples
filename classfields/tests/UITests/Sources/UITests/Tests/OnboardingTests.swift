import Foundation
import XCTest

final class OnboardingTests: BaseTest {
    func test_closeOnboarding() {
        launch()
            .tap(.closeButton)
            .should(provider: .onboardingScreen, .be(.hidden))
    }

    func test_buyerRole_credits() {
        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.buyer))
            }
            .tap(.nextButton)
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .creditsTabScreen, .exist)
    }

    func test_buyerRole() {
        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.buyer))
            }
            .focus(on: .slide(index: 0), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Никаких сюрпризов"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("Какую выбрать?"))
                    .tap()
            }
            .focus(on: .slide(index: 1), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Выбирайте любую"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("Как купить безопасно?"))
                    .tap()
            }
            .focus(on: .slide(index: 2), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Безопасная сделка"))
            }
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .transportScreen, .exist)
    }

    func test_sellerRole() {
        mocker.server.forceLoginMode = .forceLoggedOut
        mocker.startMock()

        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.seller))
            }
            .focus(on: .slide(index: 0), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Объявление без усилий"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("Как продать быстрее?"))
                    .tap()
            }
            .focus(on: .slide(index: 1), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Добавить объём"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("А можно ещё быстрее?"))
                    .tap()
            }
            .focus(on: .slide(index: 2), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Выделиться"))
            }
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .loginScreen, .exist)
    }

    func test_ownerRole() {
        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.owner))
            }
            .focus(on: .slide(index: 0), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Заезжайте в Гараж"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("Круто, а что ещё?"))
                    .tap()
            }
            .focus(on: .slide(index: 1), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Персональная лента"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("А ещё?"))
                    .tap()
            }
            .focus(on: .slide(index: 2), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Держим в курсе"))
            }
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .garageScreen, .exist)
    }

    func test_readerRole() {
        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.reader))
            }
            .focus(on: .slide(index: 0), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Мы разбираемся в машинах"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("Где читать?"))
                    .tap()
            }
            .focus(on: .slide(index: 1), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Журнал Авто.ру"))
            }
            .focus(on: .nextButton, ofType: .onboardingNavigationButton) { button in
                button
                    .should(.title, .match("А что ещё?"))
                    .tap()
            }
            .focus(on: .slide(index: 2), ofType: .onboardingSlide) { slide in
                slide
                    .should(.title, .match("Настоящие отзывы"))
            }
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .reviewsTabScreen, .exist)
    }

    func test_readerRole_journal() {
        launch()
            .focus(on: .roleSelectionSlide, ofType: .onboardingRoleSelectionSlide) { slide in
                slide.tap(.roleButton(.reader))
            }
            .tap(.nextButton)
            .tap(.actionButton)
            .should(provider: .onboardingScreen, .be(.hidden))
            .should(provider: .journalTabScreen, .exist)
    }

    private func launch() -> OnboardingScreen {
        launch(
            on: .onboardingScreen,
            options: .init(overrideAppSettings: ["alwaysShowOnboarding": true])
        )
    }
}
