//
//  CatalogMocker.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 30.04.2021.
//

import Foundation
import AutoRuProtoModels
import SwiftProtobuf

extension Mocker {

    @discardableResult
    func mock_referenceCatalogCarConfigurationsSubtree() -> Self {
        server.addHandler("GET /reference/catalog/cars/configurations/subtree *") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reference_catalog_cars_configurations_subtree")
        }
        return self
    }

    @discardableResult
    func mock_referenceCatalogCarsAllOptions() -> Self {
        server.addHandler("GET /reference/catalog/cars/all-options") { (_, _) -> Response? in
            return Response.okResponse(fileName: "reference_catalog_cars_all-options")
        }
        return self
    }

    @discardableResult
    func mock_catalogSpecifications() -> Self {
        server.api.reference.catalog.cars.techInfo.get(parameters: .wildcard)
            .ok(mock: .file("catalog_cars_specifications"))
        return self
    }
}
