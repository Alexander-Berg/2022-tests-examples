//
//  ReviewTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 24.04.2020.
//

import XCTest
import Snapshots
import AutoRuProtoModels

/// @depends_on AutoRuVehicleReviews
class ReviewTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: false)
        }

        server.addHandler("GET /story/search") { (_, _) -> Response? in
            return Response.okResponse(fileName: "story_search_ok", userAuthorized: false)
        }

        try! server.start()
    }

    func test_magazineArticle_shoudBothArticleExistWhenSelectMarkModel() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_bmw_ok", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE") {
            XCTAssert(false, "No snippet")
        } else {
            validateSnippetSnapshots(accessibilityId: "testdrive_magazineSnippet", snapshotId: "\(#function)_testdrive")
        }

        if !reviewStep
            .scrollTo("Хочу купить старый Х5, поможете?") {
            XCTAssert(false, "No snippet")
        } else {
            validateSnippetSnapshots(accessibilityId: "explain_magazineSnippet", snapshotId: "\(#function)_explain")
        }
    }

    func test_magazineArticle_shoudBothArticleExistWhenNoFeed() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE") {
            XCTAssert(false, "No snippet")
        }

        if !reviewStep
            .scrollTo("Хочу купить старый Х5, поможете?") {
            XCTAssert(false, "No snippet")
        }
    }

    func test_magazineArticle_shoudExplainArticleExistWhenSelectMarkModelAndNoTest() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test_empty", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_bmw_ok", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Хочу купить старый Х5, поможете?") {
            XCTAssert(false, "No snippet")
        }
    }

    func test_magazineArticle_shoudExplainArticleExistWhenSelectMarkModelAndNoExplain() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE") {
            XCTAssert(false, "No snippet")
        }

        if !reviewStep
            .scrollTo("Хочу купить старый Х5, поможете?") {
            XCTAssert(false, "No snippet")
        }
    }

    func test_magazineArticle_shoudOpenWebViewOnTestTap() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE") {
            XCTAssert(false, "No snippet")
        } else {
            reviewStep
                .tap(selector: "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
                .exist(selector: "webView")
        }
    }

    func test_magazineArticle_shoudOpenWebViewOnExplainTap() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Хочу купить старый Х5, поможете?") {
            XCTAssert(false, "No snippet")
        } else {
            reviewStep
                .tap(selector: "Хочу купить старый Х5, поможете?")
                .exist(selector: "webView")
        }
    }

    func test_magazineArticle_shoudNotExistArticleWhenOnlyMarkSelected() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW#X5") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()
            .cleanModel()
            .notExist(selector: "Хочу купить старый Х5, поможете?")
    }

    func test_magazineArticle_shoudNotExistArticleWhenNothingSelected() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW#X5") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()
            .cleanMark()
            .notExist(selector: "Хочу купить старый Х5, поможете?")
    }

    func test_magazineArticle_shoudAtricleExistWhenSelectMarkModelGeneration() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW#X5") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmwx5_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()
            .selectGeneration()
            .exist(selector: "Хочу купить старый Х5, поможете?")
    }

    func test_magazineArticle_snippetWithoutPhoto() {
        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW#X5") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmwx5_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_test", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain_noPhoto", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()
            .exist(selector: "Хочу купить старый Х5, поможете?")
        validateSnippetSnapshots(accessibilityId: "explain_magazineSnippet", snapshotId: "\(#function)_explain_noPhoto")
    }

    func test_magazineArticle_correctURL() {
        let filePath = Bundle.resources.url(forResource: "magazine_article_test", withExtension: "json")
        let body: Data? = filePath.flatMap { try? Data(contentsOf: $0 ) }
        var resp = try! Auto_Api_MagazineArticleSnippetListResponse(jsonUTF8Data: body!)
        resp.articles[0].articleURL = "http://127.0.0.1:\(port)/mag.auto.ru/article/bmwx5new/"

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_ok", userAuthorized: false)
        }

        server.addHandler("GET /search/CARS/breadcrumbs?bc_lookup=BMW") { (_, _) -> Response? in
            return Response.okResponse(fileName: "search_CARS_breadcrumbs_bmw_ok", userAuthorized: false)
        }

        server.addHandler("GET /magazine/articles/snippets?category=Тесты&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.responseWithStatus(body: try! resp.jsonUTF8Data())
        }

        server.addHandler("GET /magazine/articles/snippets?category=Разбор&mark=BMW&model=X5&page=1&page_size=1&sort=publishDate-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "magazine_article_explain", userAuthorized: false)
        }

        server.addHandler("GET /reviews/auto/listing?category=CARS&mark=BMW&model=X5&page=1&page_size=20&sort=relevance-exp1-desc") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing_empty", userAuthorized: false)
        }

        let requestExpectation: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "GET", request.uri == "/mag.auto.ru/article/bmwx5new/?only-content=true&from=auto_app&utm_source=auto-ru&utm_medium=cpm&utm_content=bmwx5new&utm_campaign=reviews_test-drive" {
                return true
            }
            return false
        }

        let reviewStep = mainSteps
            .openReview()
            .openMarkSelector()
            .selectBMW()

        if !reviewStep
            .scrollTo("Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE") {
            XCTAssert(false, "No snippet")
        } else {
            reviewStep
                .tap(selector: "Съезд духовных лидеров: тест BMW X5, Audi Q8 и Mercedes-Benz GLE")
                .exist(selector: "webView")
        }
        self.wait(for: [requestExpectation], timeout: 10)

    }

    private func validateSnippetSnapshots(accessibilityId: String, snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let screenshot = app.descendants(matching: .any).matching(identifier: accessibilityId).firstMatch.screenshot().image
        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 1)
    }
}
