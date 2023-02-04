import AutoRuModels
@testable import AutoRuStandaloneCarHistory
import UIKit
import AutoRuAppearance
import AutoRuProtoModels
import AutoRuUtils
import AutoRuFetchableImage

import AutoRuBackendLayout

final class CreditBannerCellTests: BaseUnitTest, CarReportCardBlockTest, BackendLayoutOutput {
    var creditInfo: CarReportCreditInfo?
    var offer: Auto_Api_Offer {
            var offer = Auto_Api_Offer()
            offer.status = .active
            offer.dealerCreditConfig = Auto_Api_CreditConfiguration.with({ ( creditInfo) in
                creditInfo.creditAmountSliderStep = 100
                creditInfo.creditDefaultTerm = 5
                creditInfo.creditMaxAmount = 1_000_000
                creditInfo.creditMinAmount = 100_000
                creditInfo.creditMinRate = 0.08
                creditInfo.creditOfferInitialPaymentRate = 0.1
                creditInfo.creditStep = 100
                creditInfo.creditTermValues = [1, 2, 3, 4, 5]
            })
            return offer
        }

    func test_noApplication() {
        Step("Кредитный баннер, без заявки") {
            creditInfo = CarReportCreditInfo(
                title: offer.dealerCreditInfo!.possibleCreditParam!.titleForCarReport(price: 1231231),
                creditStatus: .readyForCalculation,
                bankIcons: [
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                ],
                onTap: {}
            )
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }

    func test_hasApplication() {
        Step("Кредитный баннер, есть заявка") {
            creditInfo = CarReportCreditInfo(
                title:  offer.dealerCreditInfo!.possibleCreditParam!.titleForCarReport(price: 1231231),
                creditStatus: .draftExist(offer: nil, amount: 30, term: 12),
                bankIcons: [
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56)),
                    FetchableImage.testImage(withFixedSize: .init(squareSize: 56))
                ],
                onTap: {}
            )
            snapshot(functionName: "\(String(describing: Self.self))_\(#function)")
        }
    }
}
