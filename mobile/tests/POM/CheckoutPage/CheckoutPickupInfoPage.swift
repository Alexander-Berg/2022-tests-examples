import XCTest

final class CheckoutPickupInfoPage: PageObject {
    var pickupInfo: CheckoutDeliveryPage.PickupInfo {
        let el = element.otherElements
            .matching(identifier: CheckoutPickupInfoViewControllerAccessibility.pickupInfo)
            .firstMatch
        return CheckoutDeliveryPage.PickupInfo(element: el)
    }
}
