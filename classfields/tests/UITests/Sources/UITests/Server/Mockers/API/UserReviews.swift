import Foundation
import AutoRuProtoModels

extension Mocker {

    // Запрос для инициализации id отзыва на бэке
    @discardableResult
    func mock_makeUserReview(reviewId: String = "4943020671716068231") -> Self {
        server.api.reviews.subject(.auto).post.ok(mock: .model(.init()) { model in
            model.reviewID = reviewId
        })
        return self
    }

    // Запрос с 1 отзывом для листинга своих отзывов
    @discardableResult
    func mock_getUserReviews() -> Self {
        server.api.user.reviews.get(parameters: .wildcard).ok(mock: .file("user_reviews"))
        return self
    }

    // Запрос с полностью заполненным отзывом
    @discardableResult
    func mock_getUserReview(reviewId: String = "4943020671716068231") -> Self {
        server.addHandler("GET /user/reviews/\(reviewId)") { (_, _) -> Response? in
            Response.okResponse(fileName: "user_review_bmw", userAuthorized: true)
        }
        return self
    }

    // Запрос на удаление отзыва
    @discardableResult
    func mock_deleteUserReview(reviewId: String = "4943020671716068231") -> Self {
        server.api.reviews.subject(.auto).reviewId(reviewId).delete.ok(mock: .file("success"))
        return self
    }

    // Запрос с опубликованным отзывом
    @discardableResult
    func mock_getPublishedUserReview(reviewId: String = "4943020671716068231") -> Self {
        server.addHandler("GET /user/reviews/\(reviewId)") { (_, _) -> Response? in
            var model: Auto_Api_ReviewResponse = .init(mockFile: "user_review_bmw")
            model.review.status = .enabled
            model.review.moderationHistory[0].status = .accepted
            return Response.okResponse(message: model, userAuthorized: true)
        }
        return self
    }

    // Запрос с пустым листингом своих отзывов
    @discardableResult
    func mock_getEmptyUserReviews() -> Self {
        server.api.user.reviews.get(parameters: .wildcard).ok(mock: .file("epmty_user_reviews"))
        return self
    }

    // Запрос с черновиком сразу после выбора категории
    @discardableResult
    func mock_getInitialUserReview(reviewId: String = "4943020671716068231") -> Self {
        //Нужно обновить SWAGGER и GenerateBackendMethod
        server.addHandler("GET /user/reviews/\(reviewId)") { (_, _) -> Response? in
            Response.okResponse(fileName: "initial_user_review")
        }
        return self
    }

    // Запрос с плюсами и минусами автомобиля
    @discardableResult
    func mock_getUsefulFeatures() -> Self {
        server.addHandler("GET /reviews/auto/suggest/useful") { (_, _) -> Response? in
            Response.okResponse(fileName: "useful_features", userAuthorized: true)
        }
        return self
    }

    // Запрос с ошибками валидации от бэка
    @discardableResult
    func mock_putUserReviewErrors(reviewId: String = "4943020671716068231") -> Self {
        server.api.reviews.subject(.auto).reviewId(reviewId).put.ok(mock: .file("put_user_review_errors"))
        return self
    }

    // Запрос для успешного размещения
    @discardableResult
    func mock_putUserReviewOk(reviewId: String = "4943020671716068231") -> Self {
        server.api.reviews.subject(.auto).reviewId(reviewId).put.ok(mock: .file("success"))
        return self
    }
}
