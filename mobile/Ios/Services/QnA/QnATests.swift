import OHHTTPStubs
import XCTest
@testable import BeruServices

class QnATests: NetworkingTestCase {

    var service: QnAServiceImpl?

    override func setUp() {
        super.setUp()
        service = QnAServiceImpl(apiClient: DependencyProvider().apiClient)
    }

    override func tearDown() {
        service = nil
        super.tearDown()
    }

    func test_shouldContainQuestions_whenLoadMyAnswersRequestSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v2",
            responseFileName: "resolve_myAnswers"
        )

        // when
        let response = service?.loadMyAnswers(pageNumber: 123).expect(in: self)

        // then
        let result = try XCTUnwrap(response?.get())
        XCTAssertNotNil(result)
        XCTAssertEqual(
            result.0.first?.productInfo?.name,
            "Беспроводные наушники Apple AirPods 2 (с зарядным футляром) MV7N2"
        )
        XCTAssertEqual(
            result.0.first?.text,
            "Чем отличаются AirPods 2 от предыдущей версии?"
        )
        XCTAssertEqual(result.0.first?.answers.first?.text, "Пылевлагозащиту добавили, вроде")
    }

    func test_shouldReturnQuestion_whenLoadingQuestionSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v2",
            responseFileName: "resolve_question"
        )

        // when
        let response = service?.loadQuestion(with: 2_845_991).expect(in: self)

        // then
        let result = try XCTUnwrap(response?.get())
        XCTAssertNotNil(result)
        XCTAssertEqual(
            result.text,
            "Какое из этих лезвий вам нравится больше?"
        )
    }

    func test_shouldReturnQuestionId_whenLoadingAnswerSucceeded() throws {
        // given
        stub(
            requestPartName: "api/v2",
            responseFileName: "resolve_answer"
        )

        // when
        let response = service?.loadAnswer(with: 2_720_428).expect(in: self)

        // then
        let result = try XCTUnwrap(response?.get())
        XCTAssertNotNil(result)
        XCTAssertEqual(result.questionId, "1435823")
    }

}
