import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuStories
final class StoriesShowTests: BaseTest {

    override func setUp() {
        super.setUp()

        setupServer()
    }

    func test_excludeOfferStory() {
        let storiesExpectation = api.story.search
            .get(parameters: [.excludeOfferId(["offer_id"])])
            .expect()

        launchMain(options: .init(overrideAppSettings: ["storyOfferPeekThreshold": 1]))
            .toggle(to: \.transport)
            .focus(on: .storiesCarousel, ofType: .storiesCarouselCell) { cell in
                cell.tap(.story(index: 1))
            }
            .should(provider: .storyScreen, .exist)
            .focus { screen in
                screen.tapToNextPage()
                screen.tapToNextPage()
                screen.tapToNextPage()
                screen.tapToNextPage()
                screen.tapToNextPage()
                screen.tapToNextPage()
            }
            .base
            .toggle(to: \.favorites)
            .toggle(to: \.transport)
            .wait(for: [storiesExpectation])
    }

    func test_sendFrontLogShowEvent() {
        let frontlogExpectation: (_ page: Int) -> XCTestExpectation = { page in
            self.expectationForRequest(method: "POST", uri: "/events/log") { (req: Auto_Api_EventsReportRequest) in
                return req.events.contains(where: { $0.storyShowEvent.storyID == "0" && $0.storyShowEvent.slideID == page })
            }
        }

        let frontlogExpectation0 = frontlogExpectation(0)
        let frontlogExpectation1 = frontlogExpectation(1)

        launch(on: .transportScreen)
            .focus(on: .storiesCarousel, ofType: .storiesCarouselCell) { cell in
                cell.tap(.story(index: 0))
            }
            .wait(for: 2) // 1 секунда на трекинг
            .should(provider: .storyScreen, .exist)
            .focus { screen in
                screen.tapToNextPage()
            }
            .wait(for: 2)

        wait(for: [frontlogExpectation0, frontlogExpectation1], timeout: 5.0)
    }

    func test_sendFrontLogShowEvent_withCardID() {
        let frontlogExpectation = expectationForRequest(method: "POST", uri: "/events/log") { (req: Auto_Api_EventsReportRequest) in
            return req.events.contains(where: { event in
                event.storyShowEvent.storyID == "1"
                    && event.storyShowEvent.slideID == 0
                    && event.storyShowEvent.cardID == "offer_id"
            })
        }

        launch(on: .transportScreen)
            .focus(on: .storiesCarousel, ofType: .storiesCarouselCell) { cell in
                cell.tap(.story(index: 1))
            }
            .wait(for: 2) // 1 секунда на трекинг

        wait(for: [frontlogExpectation], timeout: 5.0)
    }

    // MARK: - Private

    private func setupServer() {
        mocker
            .mock_base()
            .mock_eventsLog()
            .mock_storySearch(
                stories: [
                    Vertis_Story_Story.with { story in
                        story.id = "0"
                        story.title = "История 0"
                        story.version = "2"
                        story.nativeStory = "http://127.0.0.1:\(mocker.port)/story.xml"
                        story.background = "#000000"
                        story.text = "#ffffff"
                        story.pages = 5
                    },
                    Vertis_Story_Story.with { story in
                        story.id = "1"
                        story.title = "История 1"
                        story.cardID = "offer_id"
                        story.version = "2"
                        story.nativeStory = "http://127.0.0.1:\(mocker.port)/story.xml"
                        story.background = "#000000"
                        story.text = "#ffffff"
                        story.pages = 5
                    }
                ]
            )
            .mock_storyXML()

        mocker.startMock()
    }
}

private extension Mocker {
    @discardableResult
    func mock_storyXML() -> Self {
        server.addHandler("GET /story.xml") { _, _ in
            let url = Bundle.resources
                .url(forResource: "story", withExtension: "xml")!
            return Response(
                status: "HTTP/1.1 200 OK",
                headers: [:],
                body: try! Data(contentsOf: url, options: .uncached)
            )
        }

        return self
    }
}
