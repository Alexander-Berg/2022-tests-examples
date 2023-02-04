//
//  Video.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_videoSearchCars() -> Self {
        server.addHandler("GET /video/search/cars *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "video_search_cars")
        }
        return self
    }
}
