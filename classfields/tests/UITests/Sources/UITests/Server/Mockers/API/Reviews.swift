//
//  Reviews.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_reviewsAutoListing() -> Self {
        server.addHandler("GET /reviews/auto/listing *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reviews_auto_listing")
        }
        return self
    }

    @discardableResult
    func mock_makeReviewAuto() -> Self {
        server.addHandler("POST /reviews/auto") {
            Auto_Api_ReviewSaveResponse.with { response in
                response.reviewID = "123"
            }
        }
        return self
    }

    @discardableResult
    func mock_reviewCard(_ id: String) -> Self {
        server.api.reviews.subject(.auto).reviewId(id).get
            .ok(mock: .file("review_auto_card"))
        return self
    }

    @discardableResult
    func mock_reviewComments(_ id: String) -> Self {
        server.api.reviews.subject(.auto).reviewId(id).comments.get(parameters: .wildcard)
            .ok(mock: .file("review_card_comments"))
        return self
    }
}
