import OHHTTPStubs
import XCTest
@testable import BeruServices

class StoriesTests: NetworkingTestCase {

    var service: StoriesServiceImpl?

    override func setUp() {
        super.setUp()

        service = StoriesServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldContainStories_whenGetStoriesRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "resolve_stories"
        )

        // when

        let parameters = ResolveStoriesParameters(
            isPreview: false,
            pageId: 123,
            type: .old,
            isRanking: false,
            idfa: "",
            strategy: .set
        )
        let result = service?.getStories(parameters).expect(in: self)

        // then
        let stories = try XCTUnwrap(result?.get())

        let firstStory: Story? = stories.first.flatMap {
            switch $0 {
            case let .story(story):
                return story
            case .live:
                return nil
            }
        }

        XCTAssertNotNil(stories)
        XCTAssertEqual(firstStory?.preview?.text, "Дома и на даче: всё для животных")
    }

    func test_errorSlides_whenGetStoriesRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v1",
            responseFileName: "resolve_stories_empty_slides"
        )

        // when
        let parameters = ResolveStoriesParameters(
            isPreview: false,
            pageId: 123,
            type: .old,
            isRanking: false,
            idfa: "",
            strategy: .set
        )
        let result = service?.getStories(parameters).expect(in: self)

        // then
        let stories = try XCTUnwrap(result?.get())

        let firstStory: Story? = stories.first.flatMap {
            switch $0 {
            case let .story(story):
                return story
            case .live:
                return nil
            }
        }

        XCTAssertNotNil(stories)
        XCTAssertEqual(stories.count, 1)
        XCTAssertEqual(firstStory?.preview?.text, "Дома и на даче: всё для животных")
    }

}
