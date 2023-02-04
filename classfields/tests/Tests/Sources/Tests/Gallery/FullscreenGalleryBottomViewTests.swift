import AutoRuViews
import AutoRuModels
import AutoRuUtils
import AutoRuFetchableImage
import XCTest
import Snapshots

final class FullscreenGalleryBottomViewTests: BaseUnitTest {
    private static var mediaItem: MediaItem {
        MediaItem.photo(FetchableImage.testImage(withFixedSize: CGSize(squareSize: 100)))
    }

    func test_mediaSlide_hasReport_notOnlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .media(Self.mediaItem),
            hasCarReport: true,
            chatsOnly: false,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_mediaSlide_hasReport_onlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .media(Self.mediaItem),
            hasCarReport: true,
            chatsOnly: true,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_mediaSlide_noReport_onlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .media(Self.mediaItem),
            hasCarReport: false,
            chatsOnly: true,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_mediaSlide_noReport_notOnlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .media(Self.mediaItem),
            hasCarReport: false,
            chatsOnly: false,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_sold() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .media(Self.mediaItem),
            hasCarReport: false,
            chatsOnly: true,
            sold: true,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        XCTAssertNil(panel)
    }

    func test_reportSlide_onlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .report(FetchableImage.testImage(withFixedSize: CGSize(squareSize: 100)), .init()),
            hasCarReport: true,
            chatsOnly: true,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }

    func test_reportSlide_notOnlyChats() throws {
        let panel = MediaGalleryPanel.makePanelView(
            for: .report(FetchableImage.testImage(withFixedSize: CGSize(squareSize: 100)), .init()),
            hasCarReport: true,
            chatsOnly: false,
            sold: false,
            title: "BMW X5",
            priceText: "1 000 000 ₽",
            onCall: { _ in },
            onChat: { _ in },
            onShowReport: { }
        )

        let view = try XCTUnwrap(panel)

        view.backgroundColor = Snapshot.transparencyReplacementColor
        view.pin(.width).const(DeviceWidth.iPhone11).equal()

        Snapshot.compareWithSnapshot(view: view)
    }
}
