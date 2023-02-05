import OHHTTPStubs
import XCTest
@testable import BeruServices

class SKUReviewSummaryTest: NetworkingTestCase {

    var service: SKUServiceImpl?

    override func setUp() {
        super.setUp()
        service = SKUServiceImpl(
            apiClient: DependencyProvider().apiClient,
            sinsCommissionManager: SinsCommissionManagerStub()
        )
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldContainCorrectMLReviewText_whenLoadSKUReviewSummaryRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "resolveReviewSummaryOpinionsByProductId",
            responseFileName: "review_summary"
        )

        // when
        let response = service?.loadSKUReviewSummary(with: 666, appContext: nil).expect(in: self)

        // then
        let result = try? XCTUnwrap(response?.get())
        XCTAssertNotNil(result)

        let pros = [
            "Скорость, тишина работы, качество картинки.",
            "Дизайн.",
            "С хорошими внутренними характеристиками.",
            "Тихая."
        ]
        let contra = [
            "Мало игр.",
            "Usb для игры с зарядкой от плойки.",
            "Жесткий диск маловат.",
            "Большая."
        ]
        XCTAssertEqual(result?.positiveTags, pros)
        XCTAssertEqual(result?.negativeTags, contra)
    }
}
