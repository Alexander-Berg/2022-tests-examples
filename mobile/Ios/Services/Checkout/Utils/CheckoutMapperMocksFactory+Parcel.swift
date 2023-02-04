import MarketCodableUtils
import XCTest

@testable import BeruServices
@testable import MarketDTO

// MARK: - Parcel

extension CheckoutMapperMocksFactory {

    func makeParcel(
        info: ParcelInfo,
        deliveryOptions: DeliveryOptionsByType = Constants.Shipment.deliveryOptions,
        paymentMethods: [PaymentMethodNew] = [],
        vghInfo: String = Constants.Parcel.vghInfo
    ) -> Parcel {
        let itemsErrors = info.items.reduce(into: ParcelItemsErrors()) { result, item in
            result[item.label] = []
        }
        return Parcel(
            info: info,
            deliveryOptions: deliveryOptions,
            paymentMethods: paymentMethods,
            vghInfo: vghInfo,
            itemsErrors: itemsErrors
        )
    }

    func makeParcelInfo(
        label: String = Constants.Parcel.label,
        shopId: Int = Constants.Parcel.shopId,
        shopName: String = Constants.Parcel.shopName,
        isPreorder: Bool = false,
        items: [ParcelItem] = []
    ) -> ParcelInfo {
        ParcelInfo(
            label: label,
            shopId: shopId,
            shopName: shopName,
            isPreorder: isPreorder,
            items: items
        )
    }

    func makeParcelItem(
        offerId: String = Constants.ParcelItem.offerId,
        wareMd5: String = Constants.ParcelItem.wareMd5,
        feeShow: String = Constants.ParcelItem.feeShow,
        cartShowInfo: String = Constants.ParcelItem.cartShowInfo,
        payload: String = Constants.ParcelItem.payload,
        title: String = Constants.ParcelItem.title,
        price: Int = Constants.ParcelItem.price,
        basePrice: Int = Constants.ParcelItem.basePrice,
        count: Int = Constants.ParcelItem.count,
        image: Photo = Constants.ParcelItem.image,
        manufactCountries: [String] = [Constants.ParcelItem.manufactCountry],
        supplierDescription: String? = nil,
        sku: String? = nil,
        bundleId: String? = nil,
        isPrimaryInBundle: Bool = false,
        label: String = Constants.ParcelItem.label,
        relatedItemLabel: String? = nil,
        skuLink: URL? = nil,
        specs: [InternalSpecification] = [],
        services: [ParcelItem.Service] = [],
        availableServices: [ParcelItem.Service] = []
    ) -> ParcelItem {
        ParcelItem(
            offerId: offerId,
            wareMd5: wareMd5,
            feeShow: feeShow,
            cartShowInfo: cartShowInfo,
            payload: payload,
            title: title,
            price: Decimal(price),
            buyerPriceNominal: Decimal(price),
            basePrice: Decimal(basePrice),
            count: count,
            image: image,
            manufactCountries: manufactCountries,
            supplierDescription: supplierDescription,
            sku: sku,
            bundleId: bundleId,
            isPrimaryInBundle: isPrimaryInBundle,
            label: label,
            relatedItemLabel: relatedItemLabel,
            skuLink: skuLink,
            specs: specs,
            services: services,
            availableServices: availableServices,
            isBnpl: false,
            minQuantity: nil
        )
    }

    // MARK: - DTO

    func makeParcelResult(
        shop: ParcelResult.Shop?,
        label: String = Constants.Parcel.label,
        parcelInfo: String = Constants.Parcel.vghInfo,
        preorder: Bool = false,
        isPartialDeliveryAvailable: Bool = false,
        items: [ParcelItemResult] = [],
        paymentMethods: [PaymentMethodResult] = [],
        deliveryOptions: [DeliveryOptionResult] = [],
        cheapestDeliveryOptions: [CheapestDeliveryOptionResult] = [],
        errors: [ParcelErrorResult] = [],
        warnings: [ParcelErrorResult] = []
    ) -> ParcelResult {
        ParcelResult(
            shop: shop,
            label: label,
            parcelInfo: parcelInfo,
            preorder: preorder,
            items: items,
            paymentMethods: paymentMethods,
            deliveryOptions: deliveryOptions,
            cheapestDeliveryOptions: cheapestDeliveryOptions,
            errors: errors,
            warnings: warnings
        )
    }

    func makeParcelResultShop(
        id: Int? = Constants.Parcel.shopId,
        name: String = Constants.Parcel.shopName
    ) -> ParcelResult.Shop {
        ParcelResult.Shop(id: id, name: name)
    }

    func makeParcelItemResult(
        offerId: String = Constants.ParcelItem.offerId,
        wareMd5: String = Constants.ParcelItem.wareMd5,
        feeShow: String = Constants.ParcelItem.feeShow,
        cartShowInfo: String = Constants.ParcelItem.cartShowInfo,
        payload: String = Constants.ParcelItem.payload,
        title: String = Constants.ParcelItem.title,
        price: Int = Constants.ParcelItem.price,
        basePrice: Int = Constants.ParcelItem.basePrice,
        count: Int = Constants.ParcelItem.count,
        image: ImageResult? = Constants.ParcelItem.imageResult,
        manufactCountries: [RegionResult] = [Constants.ParcelItem.manufactCountryResult],
        supplierDescription: String? = nil,
        sku: String? = nil,
        bundleId: String? = nil,
        isPrimaryInBundle: Bool = false,
        label: String = Constants.ParcelItem.label,
        relatedItemLabel: String? = nil,
        specs: SpecificationResult? = nil,
        services: [ServiceResult] = [],
        availableServices: [ServiceResult] = [],
        bnpl: Bool = false,
        errors: [ParcelItemResult.Error] = [],
        modifications: [ParcelItemResult.Modification] = []
    ) -> ParcelItemResult {
        ParcelItemResult(
            offerId: offerId,
            wareMd5: wareMd5,
            feeShow: feeShow,
            cartShowInfo: cartShowInfo,
            payload: payload,
            title: title,
            price: price,
            buyerPriceNominal: price,
            basePrice: basePrice,
            count: count,
            image: image,
            manufactCountries: manufactCountries,
            supplierDescription: supplierDescription,
            sku: sku.map { ParcelItemResult.SKU(id: $0) },
            bundleId: bundleId,
            isPrimaryInBundle: isPrimaryInBundle,
            label: label,
            relatedItemLabel: relatedItemLabel,
            availableServices: availableServices,
            specs: specs,
            services: services,
            bnpl: bnpl,
            errors: errors,
            modifications: modifications,
            bundleSettings: nil,
            promos: []
        )
    }
}

// MARK: - Nested Types

private extension CheckoutMapperMocksFactory {
    enum Constants {
        enum Shipment {
            static let deliveryOptions: DeliveryOptionsByType = [:]
        }

        enum Parcel {
            static let label = "stub_label"
            static let shopId = 0
            static let shopName = "stub_shopName"
            static let vghInfo = "stub_vghInfo"
        }

        enum ParcelItem {
            static let offerId = "stub_offerId"
            static let wareMd5 = "stub_wareMd5"
            static let feeShow = "stub_feeShow"
            static let cartShowInfo = "stub_cartShowInfo"
            static let payload = "stub_payload"
            static let title = "stub_title"
            static let price = 9_999
            static let basePrice = 9_999
            static let count = 2
            static let image = Photo(rawUrl: "ya.ru", width: 100, height: 100)
            static let imageResult = ImageResult(width: 100, height: 100, url: "ya.ru")
            static let manufactCountry = "Россия"
            static let manufactCountryResult = RegionResult(id: 215, name: manufactCountry)
            static let label = "stub_label"
        }
    }
}
