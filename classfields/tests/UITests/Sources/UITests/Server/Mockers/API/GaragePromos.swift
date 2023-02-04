import SwiftProtobuf
import AutoRuProtoModels

extension Mocker {
    @discardableResult
    func mock_garagePromos(
        targetPromoIds: [String] = [],
        fileNames: [String] = ["garage_promos_page", "garage_promos_page2"],
        pageSize: Int = 10
    ) -> Self {
        if targetPromoIds.isEmpty {
            fileNames.enumerated().forEach { index, fileName in
                server.api.garage.user.promos
                    .get(parameters: [.page(index + 1), .pageSize(pageSize)])
                    .ok(mock: .file(fileName))
            }
        } else {
            fileNames.enumerated().forEach { index, fileName in
                server.api.garage.user.promos
                    .get(parameters: [.page(index + 1), .pageSize(pageSize), .targetPromoIds(targetPromoIds)])
                    .ok(mock: .file(fileName))
            }

            fileNames.enumerated().forEach { index, fileName in
                server.api.garage.user.promos
                    .get(parameters: [.page(index + 1), .pageSize(pageSize)])
                    .ok(mock: .file(fileName))
            }
        }

        return self
    }

    @discardableResult
    func mock_garagePromos_market() -> Self {
        server.api.garage.user.promos
            .get(parameters: .wildcard)
            .ok(mock: .file("garage_promos_page"))
        return self
    }

    @discardableResult
    func mock_garagePromos_empty() -> Self {
        server.api.garage.user.promos
            .get(parameters: .wildcard)
            .ok(mock: .model())
        return self
    }

    @discardableResult
    func mock_garageCardPromos() -> Self {
        server.api.garage.user.promos
            .get(parameters: [.page(1), .pageSize(10)])
            .ok(mock: .file("garage_card_promos"))

        return self
    }
}
