import XCTest
import Foundation
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

final class GarageAllPromosTests: GarageCardBaseTests {
    func test_openAllPromosMarket() {
        mocker.mock_garagePromos_market()

        launch()
        openGarageCard()
            .scroll(to: .specialOffers)
            .focus { promos in
                promos
                    .scroll(to: "show_all_promos_button", direction: .left)
            }
            .should(.allPromosButton, .exist)
            .tap(.allPromosButton)
            .should(provider: .garageAllPromosScreen, .exist)
            .wait(for: 1)
            .validateSnapshot()
    }

    func test_openAllPromos() {
        mocker.mock_garagePromos()

        let firstPageExpectation = api.garage.user.promos
            .get(parameters: [.page(1), .pageSize(10)])
            .expect()

        let secondPageExpectation = api.garage.user.promos
            .get(parameters: [.page(2), .pageSize(10)])
            .expect()

        launch()
        openGarageCard()
            .scroll(to: .specialOffers)
            .focus { promos in
                promos
                    .scroll(to: "show_all_promos_button", direction: .left)
            }
            .should(.allPromosButton, .exist)
            .tap(.allPromosButton)
            .should(provider: .garageAllPromosScreen, .exist)
            .focus { promosScreen in
                promosScreen
                    .scroll(to: .commonPromo(title: "-30% на услуги от Колесо.ру"))
                    .scroll(to: .commonPromo(title: "-20% на карты помощи на дороге от РАМК"))
                    .tap(.commonPromo(title: "-20% на карты помощи на дороге от РАМК"))
            }
            .should(provider: .garagePromoPopup, .exist)

        wait(for: [firstPageExpectation, secondPageExpectation], timeout: 3)
    }
    
    func test_openAllPromosImmediately() {
        launch()
        openGarageCard()
            .scroll(to: .specialOffers)
            .tap(.openAllPromosLabel)
            .should(provider: .garageAllPromosScreen, .exist)
    }
}
