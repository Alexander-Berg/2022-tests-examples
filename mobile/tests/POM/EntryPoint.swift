import MarketInstallments
import MarketRegionSelectFeature
import XCTest

// MARK: - SKU

protocol SKUEntryPoint: PageObject {
    func tap() -> SKUPage
}

extension SKUEntryPoint {
    @discardableResult
    func tap() -> SKUPage {
        element.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).tap()
        let skuElem = XCUIApplication().otherElements[SKUAccessibility.root]
        XCTAssertTrue(skuElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return SKUPage(element: skuElem)
    }
}

// MARK: - Cart

protocol CartEntryPoint: PageObject {
    func tap() -> CartPage
}

extension CartEntryPoint {
    func tap() -> CartPage {
        element.tap()
        let cartElem = XCUIApplication().otherElements[CartAccessibility.root]
        XCTAssertTrue(cartElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return CartPage(element: cartElem)
    }
}

// MARK: - Service

protocol ServicesEntryPoint: PageObject {
    func tap() -> ServicesPopupPage
}

extension ServicesEntryPoint {
    func tap() -> ServicesPopupPage {
        element.tap()
        let serviceElem = XCUIApplication().otherElements[CartServicesPopupAccessibility.root]
        XCTAssertTrue(serviceElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return ServicesPopupPage(element: serviceElem)
    }
}

// MARK: - Checkout

protocol CheckoutEntryPoint: PageObject {
    func tap() -> CheckoutPage
}

extension CheckoutEntryPoint {
    func tap() -> CheckoutPage {
        element.tap()
        let elem = XCUIApplication().otherElements[CheckoutAccessibility.root]
        return CheckoutPage(element: elem)
    }
}

// MARK: - Checkout Delivery

protocol CheckoutDeliveryEntryPoint: PageObject {
    func tap() -> CheckoutDeliveryPage
}

extension CheckoutDeliveryEntryPoint {
    func tap() -> CheckoutDeliveryPage {
        element.tap()
        let elem = XCUIApplication().otherElements[CheckoutDeliveryAccessibility.root]
        return CheckoutDeliveryPage(element: elem)
    }
}

// MARK: - Finish multiorder

protocol FinishMultiorderEntryPoint: PageObject {
    func tap() -> FinishMultiorderPage
}

extension FinishMultiorderEntryPoint {
    func tap() -> FinishMultiorderPage {
        element.tap()
        let finishMultiorderElem = XCUIApplication().otherElements[FinishMultiorderAccessibilty.root]
        return FinishMultiorderPage(element: finishMultiorderElem)
    }
}

// MARK: - Feed

protocol FeedEntryPoint: PageObject {
    func tap() -> FeedPage
}

extension FeedEntryPoint {
    func tap() -> FeedPage {
        element.tap()
        return FeedPage.current
    }
}

// MARK: - Webview

protocol WebviewEntryPoint: PageObject {
    func tap() -> WebViewPage
}

extension WebviewEntryPoint {
    func tap() -> WebViewPage {
        element.tap()
        return WebViewPage.current
    }
}

// MARK: - OutletOnMap

protocol OutletOnMapEntryPoint: PageObject {
    func tap() -> OutletMapInfoPage
}

extension OutletOnMapEntryPoint {
    func tap() -> OutletMapInfoPage {
        element.tap()
        let outletMapElem = XCUIApplication().otherElements[OutletMapInfoAccessibility.root]
        return OutletMapInfoPage(element: outletMapElem)
    }
}

// MARK: - OrderReceipts

protocol OrderReceiptsEntryPoint: PageObject {
    func tap() -> XCUIElement
}

extension OrderReceiptsEntryPoint {
    func tap() -> XCUIElement {
        element.tap()
        return XCUIApplication().otherElements[OrderDocumentsAccessibility.root]
    }
}

// MARK: - OrderEditPayment

protocol OrderEditPaymentEntryPoint: PageObject {
    func tap() -> OrderEditPaymentPage
}

extension OrderEditPaymentEntryPoint {
    func tap() -> OrderEditPaymentPage {
        element.tap()
        let orderEditPayment = XCUIApplication().otherElements[OrderEditPaymentAccessibility.root]
        return .init(element: orderEditPayment)
    }
}

// MARK: - WebView

protocol WebViewEntryPoint: PageObject {
    func tap() -> WebViewPage
}

extension WebViewEntryPoint {
    func tap() -> WebViewPage {
        element.tap()
        let webView = XCUIApplication().otherElements[WebviewAccessibility.webview]
        return WebViewPage(element: webView)
    }
}

// MARK: - Search

protocol SearchEntryPoint: PageObject {
    func tap() -> SearchPage
}

extension SearchEntryPoint {
    func tap() -> SearchPage {
        element.tap()
        return SearchPage.current
    }
}

// MARK: - MerchantPopup

protocol MerchantPopupEntryPoint: PageObject {
    func tap() -> MerchantPopupPage
}

extension MerchantPopupEntryPoint {

    func tap() -> MerchantPopupPage {
        element.tap()
        let elem = XCUIApplication().otherElements[MerchantPopupAccessibility.root]
        XCTAssertTrue(elem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return MerchantPopupPage(element: elem)
    }

}

// MARK: - OrdersList

protocol OrdersListEntryPoint: PageObject {
    func tap() -> OrdersListPage
}

extension OrdersListEntryPoint {

    func tap() -> OrdersListPage {
        element.tap()
        let ordersListElem = XCUIApplication().otherElements[OrdersListAccessibility.root]
        XCTAssertTrue(ordersListElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return OrdersListPage(element: ordersListElem)
    }

}

// MARK: - OrderDetails

protocol OrderDetailsEntryPoint: PageObject {
    func tap() -> OrderDetailsPage
}

extension OrderDetailsEntryPoint {

    func tap() -> OrderDetailsPage {
        element.tap()
        let orderDetailElem = XCUIApplication().otherElements[OrderDetailsAccessibility.root]
        XCTAssertTrue(orderDetailElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return OrderDetailsPage(element: orderDetailElem)
    }

}

// MARK: - OrderEdit

protocol OrderEditEntryPoint: PageObject {
    func tap() -> OrderEditPage
}

extension OrderEditEntryPoint {

    func tap() -> OrderEditPage {
        element.tap()
        let orderEditElem = XCUIApplication().otherElements[OrderEditAccessibility.root]
        XCTAssertTrue(orderEditElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return OrderEditPage(element: orderEditElem)
    }

}

// MARK: - OrderEditFinished

protocol OrderEditFinishedEntryPoint: PageObject {
    func tap() -> OrderEditFinishedPage
}

extension OrderEditFinishedEntryPoint {

    func tap() -> OrderEditFinishedPage {
        element.tap()
        let orderEditElem = XCUIApplication().otherElements[OrderEditFinishedAccessibility.root]
        XCTAssertTrue(orderEditElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return OrderEditFinishedPage(element: orderEditElem)
    }

}

// MARK: - AddOrEditAddress

protocol EditAddressEntryPoint: PageObject {
    func tap() -> EditAddressPage
}

extension OrderEditFinishedEntryPoint {

    func tap() -> EditAddressPage {
        element.tap()
        let editAddressElem = XCUIApplication().otherElements[EditAddressAccessibility.root]
        XCTAssertTrue(editAddressElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return EditAddressPage(element: editAddressElem)
    }

}

// MARK: - Filters

protocol FiltersEntryPoint: PageObject {
    func tap() -> FiltersPage
}

extension FiltersEntryPoint {

    func tap() -> FiltersPage {
        element.tap()
        let filtersElem = XCUIApplication().otherElements[FiltersAccessibility.root]
        XCTAssertTrue(filtersElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return FiltersPage(element: filtersElem)
    }

}

// MARK: - Filter

protocol FilterEntryPoint: PageObject {
    func tap() -> FilterPage
}

extension FilterEntryPoint {

    func tap() -> FilterPage {
        element.tap()
        let filtersElem = XCUIApplication().otherElements[FilterAccessibility.root]
        XCTAssertTrue(filtersElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return FilterPage(element: filtersElem)
    }

}

// MARK: - PromoHub

protocol PromoHubEntryPoint: PageObject {
    func tap() -> PromoHubPage
}

extension PromoHubEntryPoint {

    func tap() -> PromoHubPage {
        element.tap()
        let promoHubElem = XCUIApplication().otherElements[PromoHubAccessibility.root]
        XCTAssertTrue(promoHubElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return PromoHubPage(element: promoHubElem)
    }

}

// MARK: - RegionSelectPage

protocol RegionSelectEntryPoint: PageObject {
    func tap() -> RegionSelectPage
}

extension RegionSelectEntryPoint {

    func tap() -> RegionSelectPage {
        element.tap()
        let regionSelectElem = XCUIApplication().otherElements[RegionSelectAccessibility.root]
        XCTAssertTrue(regionSelectElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return RegionSelectPage(element: regionSelectElem)
    }

}

// MARK: - CatalogPage

protocol CatalogEntryPoint: PageObject {
    func tap() -> CatalogPage
}

extension CatalogEntryPoint {

    func tap() -> CatalogPage {
        element.tap()
        let catalogElem = XCUIApplication().otherElements[CatalogAccessibility.container]
        XCTAssertTrue(catalogElem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return CatalogPage(element: catalogElem)
    }

}

// MARK: - PriceDropPopup

protocol PriceDropPopupEntryPoint: PageObject {
    func tap() -> PriceDropPopupPage
}

extension PriceDropPopupEntryPoint {

    func tap() -> PriceDropPopupPage {
        element.tap()
        let elem = XCUIApplication().collectionViews[PriceDropPopupAccessibility.collectionView]
        XCTAssertTrue(elem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return PriceDropPopupPage(element: elem)
    }

}

// MARK: - InstallmentsSelectorPopup

protocol InstallmentsSelectorPopupEntryPoint: PageObject {
    func tap() -> InstallmentsSelectorPopupPage
}

extension InstallmentsSelectorPopupEntryPoint {

    func tap() -> InstallmentsSelectorPopupPage {
        element.tap()
        let elem = XCUIApplication().otherElements[InstallmentsSelectorPopupAccessibility.root]
        XCTAssertTrue(elem.waitForExistence(timeout: XCTestCase.defaultTimeOut))
        return InstallmentsSelectorPopupPage(element: elem)
    }
}
