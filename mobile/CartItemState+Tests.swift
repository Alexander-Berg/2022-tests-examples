import MarketState
@testable import Beru

extension CartItemState {

    static func makeCartItem(
        id: Int,
        hasOffer: Bool = true,
        label: String,
        count: Int = 1,
        actualizedCount: Int? = nil,
        bundleId: String? = nil,
        primary: Bool = true,
        hid: Int? = nil
    ) -> CartItemState {
        let offer = YMTOffer()
        offer.setValue(String(id), forKey: "ID")
        // swiftlint:disable:next force_unwrapping
        offer.setValue(URL(string: "beru://cpa.url")!, forKey: "cpaUrl")

        let item = YMTCartItem()
        item.setValue(NSNumber(value: id), forKey: "ID")
        if hasOffer {
            item.setValue(offer, forKey: "offer")
        }
        item.setValue(label, forKey: "label")
        item.setValue(NSNumber(value: count), forKey: "count")
        item.setValue(bundleId, forKey: "bundleId")
        item.setValue(primary, forKey: "primaryInBundle")
        if let aHid = hid {
            item.setValue(NSNumber(value: aHid), forKey: "hid")
        }

        var state = CartItemState(item: item)
        state.actualizedCount = actualizedCount

        return state
    }

}
