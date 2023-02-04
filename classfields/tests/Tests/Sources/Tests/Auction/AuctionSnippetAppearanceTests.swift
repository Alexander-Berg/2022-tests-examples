import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuColorSchema
import Snapshots
import AutoRuFetchableImage
@testable import AutoRuAuctionSnippet

final class AuctionSnippetAppearanceTests: BaseUnitTest {
    func test_auctionActive() {
        let status = ApplicationStatus(title: "", description: nil, stage: .estimationInProgress)

        let legacyStatus = LegacyApplicationStatus.active(
            [
                StageModel(index: 1, title: "Этап 1", description: "Описание 1", active: true),
                StageModel(index: 2, title: "Этап 2", description: "Описание 2", active: true),
                StageModel(index: 3, title: "Этап 3", description: "Описание 3", active: false),
            ]
        )
        let info = InfoModel(
            title: "Land Rover Range Rover Evoque I, 2017",
            phone: "8 800 555-35-35",
            price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
            workHours: "10:00 – 20:00",
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
            legacyStatus: legacyStatus,
            status: status
        )
        let application = AuctionSnippetViewState.Application(
            id: 1,
            info: info,
            legacyStatus: legacyStatus,
            status: status,
            useNewFlow: false
        )

        let items = AuctionSnippetTableItemConverter.makeItems(
            for: application,
            onCallSupport: { _ in },
            onCancelApplication: { },
            onApproveApplication: { },
            onPreparationTap: { },
            onAboutTap: { }
        )

        Step("Проверяем внешний вид сниппета аукциона для активного состояния") {
            for (idx, item) in items.enumerated() {
                Snapshot.compareWithSnapshot(
                    cellHelper: item.cellHelper,
                    maxWidth: DeviceWidth.iPhone11,
                    backgroundColor: ColorSchema.Background.surface,
                    identifier: "auction_snippet_active_\(idx)"
                )
            }
        }
    }

    func test_auctionFinished() {
        let status = ApplicationStatus(title: "", description: nil, stage: .dealCompleted)

        let info = InfoModel(
            title: "Land Rover Range Rover Evoque I, 2017",
            phone: "8 800 555-35-35",
            price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
            workHours: "10:00 – 20:00",
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
            legacyStatus: .finished,
            status: status
        )
        let application = AuctionSnippetViewState.Application(
            id: 1,
            info: info,
            legacyStatus: .finished,
            status: status,
            useNewFlow: false
        )

        let items = AuctionSnippetTableItemConverter.makeItems(
            for: application,
            onCallSupport: { _ in },
            onCancelApplication: { },
            onApproveApplication: { },
            onPreparationTap: { },
            onAboutTap: { }
        )

        Step("Проверяем внешний вид сниппета аукциона для завершенного состояния") {
            for (idx, item) in items.enumerated() {
                Snapshot.compareWithSnapshot(
                    cellHelper: item.cellHelper,
                    maxWidth: DeviceWidth.iPhone11,
                    backgroundColor: ColorSchema.Background.surface,
                    identifier: "auction_snippet_finished_\(idx)"
                )
            }
        }
    }

    func test_auctionFinished_newFlow() {
        let status = ApplicationStatus(title: "Сделка состоялась", description: nil, stage: .dealCompleted)

        let info = InfoModel(
            title: "Land Rover Range Rover Evoque I, 2017",
            phone: "8 800 555-35-35",
            price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
            workHours: "10:00 – 20:00",
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
            legacyStatus: .finished,
            status: status
        )
        let application = AuctionSnippetViewState.Application(
            id: 1,
            info: info,
            legacyStatus: .finished,
            status: status,
            useNewFlow: true
        )

        let items = AuctionSnippetTableItemConverter.makeItems(
            for: application,
            onCallSupport: { _ in },
            onCancelApplication: { },
            onApproveApplication: { },
            onPreparationTap: { },
            onAboutTap: { }
        )

        Step("Проверяем внешний вид сниппета аукциона для завершенного состояния (новый флоу)") {
            for (idx, item) in items.enumerated() {
                Snapshot.compareWithSnapshot(
                    cellHelper: item.cellHelper,
                    maxWidth: DeviceWidth.iPhone11,
                    backgroundColor: ColorSchema.Background.surface,
                    identifier: "auction_snippet_finished_\(idx)_new_flow"
                )
            }
        }
    }

    func test_auctionRejected_newFlow() {
        let status = ApplicationStatus(title: "Сделка не состоялась", description: nil, stage: .dealRejected)

        let info = InfoModel(
            title: "Land Rover Range Rover Evoque I, 2017",
            phone: "8 800 555-35-35",
            price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
            workHours: "10:00 – 20:00",
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
            legacyStatus: .rejected,
            status: status
        )
        let application = AuctionSnippetViewState.Application(
            id: 1,
            info: info,
            legacyStatus: .rejected,
            status: status,
            useNewFlow: true
        )

        let items = AuctionSnippetTableItemConverter.makeItems(
            for: application,
            onCallSupport: { _ in },
            onCancelApplication: { },
            onApproveApplication: { },
            onPreparationTap: { },
            onAboutTap: { }
        )

        Step("Проверяем внешний вид сниппета аукциона для отклоненного состояния (новый флоу)") {
            for (idx, item) in items.enumerated() {
                Snapshot.compareWithSnapshot(
                    cellHelper: item.cellHelper,
                    maxWidth: DeviceWidth.iPhone11,
                    backgroundColor: ColorSchema.Background.surface,
                    identifier: "auction_snippet_rejected_\(idx)_new_flow"
                )
            }
        }
    }

    func test_auctionRejected() {
        let status = ApplicationStatus(title: "", description: nil, stage: .dealRejected)

        let info = InfoModel(
            title: "Land Rover Range Rover Evoque I, 2017",
            phone: "8 800 555-35-35",
            price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
            workHours: "10:00 – 20:00",
            photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
            legacyStatus: .rejected,
            status: status
        )
        let application = AuctionSnippetViewState.Application(
            id: 1,
            info: info,
            legacyStatus: .rejected,
            status: status,
            useNewFlow: false
        )

        let items = AuctionSnippetTableItemConverter.makeItems(
            for: application,
            onCallSupport: { _ in },
            onCancelApplication: { },
            onApproveApplication: { },
            onPreparationTap: { },
            onAboutTap: { }
        )

        Step("Проверяем внешний вид сниппета аукциона для отклоненного состояния") {
            for (idx, item) in items.enumerated() {
                Snapshot.compareWithSnapshot(
                    cellHelper: item.cellHelper,
                    maxWidth: DeviceWidth.iPhone11,
                    backgroundColor: ColorSchema.Background.surface,
                    identifier: "auction_snippet_rejected_\(idx)"
                )
            }
        }
    }

    func test_auctionActive_newFlow() {
        for stage in [ApplicationStage.estimationInProgress, .estimationDone, .inspection, .waitingForOffers, .finalPrice] {
            let status = ApplicationStatus(title: stage.name, description: stage.description(with: .init()), stage: stage)

            let info = InfoModel(
                title: "Land Rover Range Rover Evoque I, 2017",
                phone: "8 800 555-35-35",
                price: "1\u{00a0}000\u{00a0}000\u{00a0}–\u{00a0}2\u{00a0}000\u{00a0}000\u{00a0}₽",
                workHours: "10:00 – 20:00",
                photo: FetchableImage.testImage(withFixedSize: CGSize(squareSize: 24)),
                legacyStatus: .active([]),
                status: status
            )
            let application = AuctionSnippetViewState.Application(
                id: 1,
                info: info,
                legacyStatus: .active([]),
                status: status,
                useNewFlow: true
            )

            let items = AuctionSnippetTableItemConverter.makeItems(
                for: application,
                onCallSupport: { _ in },
                onCancelApplication: { },
                onApproveApplication: { },
                onPreparationTap: { },
                onAboutTap: { }
            )

            Step("Проверяем внешний вид сниппета аукциона для активного состояния (новый флоу)") {
                for (idx, item) in items.enumerated() {
                    Snapshot.compareWithSnapshot(
                        cellHelper: item.cellHelper,
                        maxWidth: DeviceWidth.iPhone11,
                        backgroundColor: ColorSchema.Background.surface,
                        identifier: "auction_snippet_active_\(stage)_\(idx)_new_flow"
                    )
                }
            }
        }
    }
}
