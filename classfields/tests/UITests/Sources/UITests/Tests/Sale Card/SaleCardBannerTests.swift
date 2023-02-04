//
//  SaleCardBannerTests.swift
//  UITests
//
//  Created by Alexander Malnev on 2/3/21.
//

import XCTest
import Snapshots

/// @depends_on AutoRuSaleCard
final class SalecardDriveBannerTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override var launchEnvironment: [String: String] {
        var env = super.launchEnvironment
        env["DRIVE_HIDE_KEY"] = "drive_sale_card_hide"
        return env
    }

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: - Helpers

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        try! server.start()
    }

    // MARK: - Drive banners

    func test_closeDriveBanner() {
        api.search.cars.post(parameters: .wildcard)
            .ok(mock: .file("history_drive_banners_audi_a3"))

        server.addHandler("GET /offer/CARS/1099557904-ae455935") { (_, _) -> Response? in
            return Response.okResponse(fileName: "sale_card_drive_banner_audi_a3", userAuthorized: true)
        }
        
        let steps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1099557904-ae455935")))
            .scrollToDriveBanner()

        let screen = steps.onSaleCardScreen()

        Step("После закрытия баннера, он больше не должен показываться") {
            screen.driveBannerCloseIcon.tap()
            screen.driveBanner.shouldNotExist()
        }

        Step("Если вернуться на экран, тоже не должно быть баннера") {
            screen.backButton.tap()

            steps.as(SaleListScreen_.self)
                .openCarOffer(with: "1099557904-ae455935")
                .scrollToTutorial()
                .onSaleCardScreen()
                .driveBanner.shouldNotExist()
        }
    }
}
