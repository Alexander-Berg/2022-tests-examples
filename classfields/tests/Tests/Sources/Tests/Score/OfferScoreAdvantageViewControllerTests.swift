import XCTest
import AutoRuProtoModels
import Snapshots
@testable import AutoRuNetwork
@testable import AutoRuOfferAdvantage

extension OfferScoreAdvantageViewController: ViewControllerScrollContaining { }

final class OfferScoreAdvantageViewControllerTests: BaseUnitTest {
    func test_saleCard_offerAdvantageNoReport() {
        Step("Проверяем попап с преимуществом-скором с карточки оффера без репорта") { }

        let offer: Auto_Api_Offer = .with { offer in
            offer.score = .with { score in
                score.transparency = .init(70)
            }
        }

        let controller = OfferScoreAdvantageViewController(
            model: OfferAdvantageModel(
                advantage: .score,
                offer: offer,
                source: .saleCard(canChat: false, canCall: false)
            )
        )

        Snapshot.compareWithSnapshot(scrollContaining: controller)
    }

    func test_saleCard_offerAdvantageReport() {
        Step("Проверяем попап с преимуществом-скором с карточки оффера с репортом") { }

        let offer: Auto_Api_Offer = .with { offer in
            offer.score = .with { score in
                score.transparency = .init(70)
            }

            offer.documents.vinResolution = .ok
        }

        let controller = OfferScoreAdvantageViewController(
            model: OfferAdvantageModel(
                advantage: .score,
                offer: offer,
                source: .saleCard(canChat: false, canCall: false)
            )
        )

        Snapshot.compareWithSnapshot(scrollContaining: controller)
    }

    func test_userSaleCard_offerAdvantageLowScore_noScoring() {
        Step("Проверяем попап с преимуществом-скором с карточки юзерского оффера без скоринга (<= 7)") { }

        let offer: Auto_Api_Offer = .with { offer in
            offer.score = .with { score in
                score.transparency = .init(50)
            }
        }

        let controller = OfferScoreAdvantageViewController(
            model: OfferAdvantageModel(
                advantage: .score,
                offer: offer,
                source: .userSaleCard(scoring: nil)
            )
        )

        Snapshot.compareWithSnapshot(scrollContaining: controller)
    }

    func test_userSaleCard_offerAdvantageScore_improvements() {
        Step("Проверяем попап с преимуществом-скором с карточки юзерского оффера без скоринга") { }

        let offer: Auto_Api_Offer = .with { offer in
            offer.score = .with { score in
                score.transparency = .init(50)
            }
        }

        let scoring: Auto_Api_TransparencyScoring = .with { scoring in
            scoring.photoWithGrzScore = 0.0
            scoring.provenOwnerScore = 0.0
            scoring.threePhotoScore = 0.0
        }

        let controller = OfferScoreAdvantageViewController(
            model: OfferAdvantageModel(
                advantage: .score,
                offer: offer,
                source: .userSaleCard(scoring: scoring)
            )
        )

        Snapshot.compareWithSnapshot(scrollContaining: controller)
    }
}
