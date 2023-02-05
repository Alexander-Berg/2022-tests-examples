import MarketModels
import XCTest

@testable import BeruServices
@testable import MarketDTO

class FlatCMSMapperTests: XCTestCase {
    func test_flatCMSMapping_whenContentIsNotEmpty() {
        // given
        let articlesWidget = ArticlesScrollboxWidgetResult(
            id: 42,
            reloadable: false,
            title: "",
            content: [
                .init(
                    id: "0",
                    title: "",
                    subtitle: "",
                    picture: "https://somevalidurl.com/test.jpeg",
                    type: .choose,
                    link: "https://somevalidurl.com/"
                )
            ]
        )

        let cmsPageResult = CMSPageResult(
            document: CMSPageResult.Document(
                id: 42,
                type: "document",
                popups: []
            ),
            widgets: [.articlesScrollboxWidget(articlesWidget)],
            pageToken: nil,
            analyticParams: [:]
        )

        // when
        let flatCMS = FlatCMSMapper.map(result: cmsPageResult)

        guard let widget = flatCMS.widgets.first?.widget as? ArticlesScrollboxWidget else {
            XCTFail("Wrong type of widget")
            return
        }

        // then
        XCTAssertEqual(flatCMS.id, cmsPageResult.document.id)
        XCTAssertEqual(flatCMS.type, cmsPageResult.document.type)
        XCTAssertEqual(widget.id, articlesWidget.id)
        XCTAssertEqual(widget.isReloadable, articlesWidget.reloadable)
        XCTAssertEqual(widget.title, articlesWidget.title)
    }

    func test_flatCMSMapping_whenContetnIsEmpty() {
        // given
        let articlesWidget = ArticlesScrollboxWidgetResult(
            id: 42,
            reloadable: false,
            title: "",
            content: []
        )

        let cmsPageResult = CMSPageResult(
            document: CMSPageResult.Document(
                id: 42,
                type: "document",
                popups: []
            ),
            widgets: [.articlesScrollboxWidget(articlesWidget)],
            pageToken: nil,
            analyticParams: [:]
        )

        // when
        let flatCMS = FlatCMSMapper.map(result: cmsPageResult)

        XCTAssertNotNil(flatCMS.widgets.first?.widget as? EmptyWidget)
    }
}
