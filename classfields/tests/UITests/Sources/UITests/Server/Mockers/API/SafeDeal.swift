import Foundation
import AutoRuProtoModels

extension Mocker {
    @discardableResult
    func mock_safeDealCreate(offerID: String) -> Self {
        server.api.safeDeal.deal.create
            .post
            .ok(
                mock: .file("safe-deal_deal_create") { model in
                    model.deal.subject.autoru.offer.id = offerID
                }
            )

        return self
    }

    @discardableResult
    func mock_safeDealCancel(dealID: String, offerID: String) -> Self {
        server.api.safeDeal.deal.update.dealId(dealID)
            .post(parameters: [])
            .ok(
                mock: .file("safe-deal_deal_update_4eebee84-0919-4ace-8869-d899c455d885") { model in
                    model.deal.subject.autoru.offer.id = offerID
                    model.deal.id = dealID
                }
            )

        return self
    }

    @discardableResult
    func mock_safeDealList() -> Self {
        server.api.safeDeal.deal.list
            .get(parameters: .wildcard)
            .ok(mock: .file("safe_deal_list"))

        return self
    }
}
