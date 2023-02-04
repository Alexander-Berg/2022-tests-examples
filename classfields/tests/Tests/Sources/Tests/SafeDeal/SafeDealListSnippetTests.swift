import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuAppearance
import Snapshots
@testable import AutoRuSafeDeal
import AutoRuColorSchema

final class SafeDealListSnippetTests: BaseUnitTest {

    func test_offerSellerSteps() {
        checks.forEach { step, snapshot in
            checkModel(for: .seller, step: step, id: "seller_\(snapshot)")
        }
    }

    func test_offerBuyerSteps() {
        checks.forEach { step, snapshot in
            checkModel(for: .buyer, step: step, id: "buyer_\(snapshot)")
        }
    }

    private let checks: [AutoRuProtoModels.Vertis_SafeDeal_DealStep: String] = [
        .dealInviteAccepted: "step_deal_invite_accepted",
        .dealCreated: "step_deal_created",
        .dealCancelling: "step_deal_cancelling",
        .dealCancelled: "step_deal_cancelled",
        .dealConfirmed: "step_deal_confirmed",
        .dealCompleted: "step_deal_completed"
    ]

    private func checkModel(
        for participantType: Vertis_SafeDeal_ParticipantType,
        step: Vertis_SafeDeal_DealStep,
        id: String
    ) {
        Step("Проверям сниппет сделки в шаге \(step.rawValue)")

        let deal: Vertis_SafeDeal_DealView = .with {
            $0.id = "fd0e806f-cb31-464a-957c-9cd94c7c345e"
            $0.state = .inProgress
            $0.step = step
            $0.sellerStep = .sellerAwaitingAccept
            $0.buyerStep = .buyerAwaitingAccept
            $0.participantType = participantType
            $0.sellingPriceRub = 400000

            $0.subject = .with { subject in
                subject.autoru = .with { autoru in
                    autoru.offer = .with { offer in
                        offer.id = "1092781260-70ca00fc"
                        offer.category = .cars
                    }
                }
            }
            if participantType == .seller {
                $0.party = .with { party in
                    party.seller = .with { seller in
                        seller.buyerInfo = .with {
                            $0.userName = "Эдуард"
                        }
                    }
                }
            }
        }

        let offer: Auto_Api_Offer = .with {
            $0.id = "1092781260-70ca00fc"
            $0.category = .cars
            $0.documents = .with { documents in
                documents.year = 2007
            }
            $0.carInfo = .with { carInfo in
                carInfo.modelInfo.name = "Accord"
                carInfo.markInfo.name = "Honda"
            }
        }

        let model = SafeDealOfferViewModel.model(
            for: deal,
            offer: offer
        )

        let layout = SafeDealOfferLayout(
            model: model,
            onActionButtonTap: { _ in },
            onAgreementTap: {},
            onOpponentNameTap: {}
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: id
        )
    }
}
