//
//  Salon.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_salon(id: String = "lada_tula") -> Self {
        server.addHandler("GET /salon/\(id)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "lada_tula", userAuthorized: false)
        }

        server.addHandler("GET /salon/\(id)/phones *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "lada_tula_phone", userAuthorized: false)
        }

        server.addHandler("GET /salon/by-dealer-id/\(id)") { (_, _) -> Response? in
            return Response.okResponse(fileName: "lada_tula", userAuthorized: false)
        }

        return self
    }

    @discardableResult
    func mock_salonListing() -> Self {
        basicMockReproducer.setup(server: server, mockFolderName: "DealerCard", userAuthorized: false)
        return self
    }
}
