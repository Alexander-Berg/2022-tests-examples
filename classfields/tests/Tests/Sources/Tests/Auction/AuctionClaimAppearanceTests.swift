import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuNavigationContainer
import AutoRuColorSchema
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuNavigationRouterSwiftUI
@testable import AutoRuAuctionClaim

final class AuctionClaimAppearanceTests: BaseUnitTest {
    func test_agreementView_notChecked() {
        let view = AgreementView()
        view.frame = CGRect(x: 0, y: 0, size: .init(width: DeviceWidth.iPhone11, height: 100))
        view.backgroundColor = ColorSchema.Background.surface
        view.isChecked = false

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_agreementView_checked() {
        let view = AgreementView()
        view.frame = CGRect(x: 0, y: 0, size: .init(width: DeviceWidth.iPhone11, height: 100))
        view.backgroundColor = ColorSchema.Background.surface

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_buybackScreen() {
        let analytics = AuctionAnalytics(
            onBuybackShow: nil,
            onBuybackNext: nil,
            onBuybackSkip: nil,
            onClaimShow: nil,
            onClaimSubmit: nil,
            onClaimSkip: nil
        )

        let frontLog = AuctionFrontLog(onBuybackShow: { }, onBuybackNext: {}, onClaimSubmit: { _ in })

        let controller = AuctionBuybackViewController(
            model: AuctionBuybackModel(
                priceFrom: "1\u{00a0}000\u{00a0}000",
                priceTo: "1\u{00a0}500\u{00a0}000",
                title: "Продайте вашу машину за", description: "Описание выкупа",
                items: [
                    AuctionBuybackModel.Item(
                        image: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
                        title: "Пункт 1",
                        subtitle: "Описание этого пункта"
                    ),
                    AuctionBuybackModel.Item(
                        image: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
                        title: "Пункт 2",
                        subtitle: "Описание этого пункта"
                    )
                ]
            ),
            analytics: analytics,
            frontLog: frontLog,
            showSkipInNavBar: false,
            buyoutSource: .wizard,
            sourceID: "",
            onNext: { },
            onSkip: { },
            onPromoOpen: { },
            onAgreementOpen: { }
        )

        Self.makeSnapshot(of: controller)
    }

    func test_buybackScreen_newFlow() {
        let analytics = AuctionAnalytics(
            onBuybackShow: nil,
            onBuybackNext: nil,
            onBuybackSkip: nil,
            onClaimShow: nil,
            onClaimSubmit: nil,
            onClaimSkip: nil
        )

        let frontLog = AuctionFrontLog(onBuybackShow: { }, onBuybackNext: {}, onClaimSubmit: { _ in })

        let controller = AuctionBuybackViewController(
            model: AuctionBuybackModel(
                priceFrom: "1\u{00a0}000\u{00a0}000",
                priceTo: "1\u{00a0}500\u{00a0}000",
                title: "Продайте вашу машину за", description: "Описание выкупа",
                items: [
                    AuctionBuybackModel.Item(
                        image: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
                        title: "Пункт 1",
                        subtitle: "Описание этого пункта"
                    ),
                    AuctionBuybackModel.Item(
                        image: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
                        title: "Пункт 2",
                        subtitle: "Описание этого пункта"
                    )
                ]
            ),
            analytics: analytics,
            frontLog: frontLog,
            showSkipInNavBar: false,
            useNewFlow: true,
            buyoutSource: .wizard,
            sourceID: "",
            onNext: { },
            onSkip: { },
            onPromoOpen: { },
            onAgreementOpen: { }
        )

        Self.makeSnapshot(of: controller)
    }

    func test_claimScreen() {
        let analytics = AuctionAnalytics(
            onBuybackShow: nil,
            onBuybackNext: nil,
            onBuybackSkip: nil,
            onClaimShow: nil,
            onClaimSubmit: nil,
            onClaimSkip: nil
        )

        let controller = AuctionClaimViewController(
            model: AuctionClaimModel(
                description: "Описание подачи заявки",
                phone: "+7 921 976-21-76",
                place: "20 км от МКАД"
            ),
            analytics: analytics,
            showSkipButton: false
        )

        Self.makeSnapshot(of: controller)
    }

    func test_successScreen() {
        let controller = AuctionClaimSuccessViewController(
            model: ClaimSuccessModel(
                description: "Описание успешной заявки",
                title: "Заголовок успешной заявки",
                items: ["Пункт 1", "Пункт 2"]
            )
        )

        Self.makeSnapshot(of: controller)
    }

    func test_buybackPreviewScreen() {
        let offer = Auto_Api_Offer.with { model in
            model.category = .cars
            model.carInfo.markInfo = .with {
                $0.name = "BMW"
            }
            model.carInfo.modelInfo = .with {
                $0.name = "X5"
            }
            model.documents.year = 2014
            model.state.mileage = 100_000
        }

        let controller = HostingViewUtils.wrap(
            BuybackPreviewView(
                viewModel: .init(
                    model: .init(
                        source: .draft(offer),
                        category: .cars,
                        place: .init(),
                        phone: "79991112233",
                        info: .init(),
                        texts: .init()
                    ),
                    application: nil
                ),
                onCloseTap: { }
            ),
            wrapWithNavigation: true
        )

        Self.makeSnapshot(of: controller)
    }

    func test_dealersListScreen() {
        let viewModel = DealersBuybackListViewModel(
            application: .with { model in
                model.propositions = [
                    .with {
                        $0.dealerName = "1"
                        $0.value = 1_000_000
                    },
                    .with {
                        $0.dealerName = "2"
                        $0.value = 1_000_000
                    },
                    .with {
                        $0.dealerName = "3"
                        $0.value = 1_000_000
                    },
                    .with {
                        $0.dealerName = "4"
                        $0.value = 1_000_000
                    }
                ]
            },
            output: Shared(wrappedValue: nil)
        )

        let controller = HostingViewUtils.wrap(
            DealersBuybackListView(viewModel: viewModel),
            wrapWithNavigation: true
        )

        Self.makeSnapshot(of: controller)
    }

    func test_waitManagerCallScreen() {
        let controller = HostingViewUtils.wrap(
            WaitManagerCallView(
                onCloseTap: { },
                onPrepareTap: { }
            ),
            wrapWithNavigation: true
        )

        Self.makeSnapshot(of: controller)
    }

    private static func makeSnapshot(of controller: UIViewController, identifier: String = #function) {
        let navigationController = NavigationStackController(
            overlapsStatusBar: true,
            overlapsHomeIndicator: true,
            maxContentWidth: 640
        )
        navigationController.viewControllers = [controller]

        Snapshot.compareWithSnapshot(viewController: navigationController, identifier: identifier)
    }
}
