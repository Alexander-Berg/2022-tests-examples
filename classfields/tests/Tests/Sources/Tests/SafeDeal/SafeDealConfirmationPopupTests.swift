import XCTest
import AutoRuProtoModels
import AutoRuUtils
import AutoRuFetchableImage
import AutoRuAppearance
import Snapshots
@testable import AutoRuSafeDeal
import AutoRuColorSchema
import Foundation

final class SafeDealConfirmationPopupTests: BaseUnitTest {
    private static let offerInfo = SafeDealOfferInfoLayoutModel(
        image: FetchableImage(url: URL(string: "https://auto.ru/")!),
        title: "Заголовок",
        rawOffer: .init()
    )

    private static let offerInfoNoPhoto = SafeDealOfferInfoLayoutModel(
        image: nil,
        title: "Заголовок",
        rawOffer: .init()
    )

    override func setUp() {
        super.setUp()

        FetchableImage.blockThreadUntilFinished = true
        setReplaceImagesWithStub("audi_snippet_stub")
    }

    override func tearDown() {
        super.tearDown()

        FetchableImage.blockThreadUntilFinished = false
        setReplaceImagesDefaultBehavior()
    }

    func test_singleRequest() {
        Step("Внешний вид попапа для продавца БС: кейс, когда один запрос по одному офферу")

        let layout = SafeDealSingleRequestLayout(
            offerInfo: Self.offerInfo,
            sellingPrice: "1\(String.nbsp)234\(String.nbsp)567 \(String.rubleSign)",
            onMoreTap: { },
            onCloseTap: { },
            onAgreementTap: { },
            onAcceptTap: { },
            onContactTap: { },
            onRejectTap: { }
        )
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_single_request"
        )
    }

    func test_singleRequest_noPhoto() {
        Step("Внешний вид попапа для продавца БС: кейс, когда один запрос по одному офферу. Без фото")

        let layout = SafeDealSingleRequestLayout(
            offerInfo: Self.offerInfoNoPhoto,
            sellingPrice: "1\(String.nbsp)234\(String.nbsp)567 \(String.rubleSign)",
            onMoreTap: { },
            onCloseTap: { },
            onAgreementTap: { },
            onAcceptTap: { },
            onContactTap: { },
            onRejectTap: { }
        )
        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_single_request_no_photo"
        )
    }

    func test_cancelRequest_noPhoto() {
        Step("Внешний вид попапа для продавца БС: кейс, когда запрос по одному офферу поверх существующей сделки. Без фото")

        let layout = SafeDealCancelRequestLayout(
            offerInfo: Self.offerInfoNoPhoto,
            sellingPrice: "1\(String.nbsp)234\(String.nbsp)567 \(String.rubleSign)",
            onCloseTap: { },
            onContactTap: { },
            onDealsTap: { }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_cancel_request_no_photo"
        )
    }

    func test_cancelRequest() {
        Step("Внешний вид попапа для продавца БС: кейс, когда запрос по одному офферу поверх существующей сделки")

        let layout = SafeDealCancelRequestLayout(
            offerInfo: Self.offerInfo,
            sellingPrice: "1\(String.nbsp)234\(String.nbsp)567 \(String.rubleSign)",
            onCloseTap: { },
            onContactTap: { },
            onDealsTap: { }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_cancel_request"
        )
    }

    func test_multipleDealsRequest() {
        Step("Внешний вид попапа для продавца БС: кейс, когда много запросов по одному офферу")

        let layout = SafeDealMultipleRequestsLayout(
            dealsRequestsCount: "10 новых запросов",
            offerInfo: Self.offerInfo,
            onCloseTap: { },
            onMoreTap: { },
            onDealsTap: { }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_multiple_deals_request"
        )
    }

    func test_multipleDealsRequest_noPhoto() {
        Step("Внешний вид попапа для продавца БС: кейс, когда много запросов по одному офферу. Без фото")

        let layout = SafeDealMultipleRequestsLayout(
            dealsRequestsCount: "10 новых запросов",
            offerInfo: Self.offerInfoNoPhoto,
            onCloseTap: { },
            onMoreTap: { },
            onDealsTap: { }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_multiple_deals_request_no_photo"
        )
    }

    func test_multipleDealsMultipleOffersRequest() {
        Step("Внешний вид попапа для продавца БС: кейс, когда много запросов по нескольким офферам")

        let layout = SafeDealMultipleRequestsDifferentOffersLayout(
            dealsRequestsCount: "10 новых запросов",
            onCloseTap: { },
            onMoreTap: { },
            onDealsTap: { }
        )

        Snapshot.compareWithSnapshot(
            layout: layout,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            identifier: "seller_confirmation_multiple_deals_offers_request"
        )
    }
}
