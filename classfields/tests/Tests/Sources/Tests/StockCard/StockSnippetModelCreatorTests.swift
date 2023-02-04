import XCTest
import AutoRuProtoModels
import AutoRuCellHelpers
import AutoRuAppearance
import Snapshots
import AutoRuUtils
@testable import AutoRuStockCard
import AutoRuColorSchema

final class StockSnippetModelCreatorTests: BaseUnitTest {
    private lazy var newOffer: Auto_Api_Offer = {
        let response: Auto_Api_OfferResponse = .init(mockFile: "offer_CARS_1098252972-99d8c274_ok")
        return response.offer
    }()

    override func setUp() {
        super.setUp()
        self.setReplaceImagesWithStub(nil)
    }

    override func tearDown() {
        super.tearDown()
        self.setReplaceImagesDefaultBehavior()
    }

    func test_snippet_withoutAuctionLeaderTag() {
        Step("Проверяем, что сниппет без тега auction_leader не содержит расширенную галерею")

        var tags = Set(newOffer.tags)
        tags.remove("auction_leader")
        newOffer.tags = Array(tags)
        let model = StockSnippetModel.modelForData(
            offer: newOffer,
            peeked: false,
            favorite: false,
            userNote: nil,
            currentImageIndex: nil,
            canAddToComparison: true,
            isInComparison: false,
            geoRadius: nil,
            isExtendedRadius: false
        )

        XCTAssert(!model.shouldShowExtendedPhoto, "Не показываем расширенную галерею")

        checkModel(model: model, id: "without_auction")
    }

    func test_snippet_withAuctionLeaderTag() {
        Step("Проверяем, что сниппет с тегом auction_leader содержит расширенную галерею")

        var tags = newOffer.tags
        tags.append("auction_leader")

        newOffer.tags = Array(Set(tags))

        let model = StockSnippetModel.modelForData(
            offer: newOffer,
            peeked: false,
            favorite: false,
            userNote: nil,
            currentImageIndex: nil,
            canAddToComparison: true,
            isInComparison: false,
            geoRadius: nil,
            isExtendedRadius: false
        )

        XCTAssert(model.shouldShowExtendedPhoto, "Показываем расширенную галерею")

        checkModel(model: model, id: "with_auction")
    }

    func test_stockSnippetPeekedExtended() {
        Step("Проверяем, просмотренное состояние расширенного сниппета стокового листинга")
        var tags = newOffer.tags
        tags.append("auction_leader")

        newOffer.tags = Array(Set(tags))
        newOffer = newOffer.setMaxDiscount()
            .setDealer()

        let model = StockSnippetModel.modelForData(
            offer: newOffer,
            peeked: true,
            favorite: false,
            userNote: nil,
            currentImageIndex: nil,
            canAddToComparison: true,
            isInComparison: false,
            geoRadius: nil,
            isExtendedRadius: false
        )

        checkModel(model: model)
    }

    func test_stockSnippetPeeked() {
        Step("Проверяем, просмотренное состояние сниппета стокового листинга")
        newOffer.tags = []

        let model = StockSnippetModel.modelForData(
            offer: newOffer,
            peeked: true,
            favorite: false,
            userNote: nil,
            currentImageIndex: nil,
            canAddToComparison: true,
            isInComparison: false,
            geoRadius: nil,
            isExtendedRadius: false
        )

        checkModel(model: model)
    }

    private func checkModel(model: StockSnippetModel, id: String = #function) {

        let builder = BaseTableModelBuilder()
        let creator = StockSnippetModelCreator(tableModelBuilder: builder)

        creator.addStockOffer(
            model,
            offer: newOffer,
            equipmentOfferCountTable: [:],
            id: newOffer.id,
            searchQueryID: nil,
            onTap: {},
            onFavoriteTap: nil,
            onUserNoteTap: nil,
            onCallTap: nil,
            onImageTap: nil,
            onContactTap: nil,
            onChatsTap: nil,
            onFirstScroll: nil,
            onActionsMenuTap: nil,
            onDealerDescriptionTap: nil,
            onContactsShowTap: nil,
            onCompareButtonTap: nil,
            scrollDidScroll: nil
        )

        let title = builder.tableItem(withIdStartsWith: "stock_offer_title")!.cellHelper
        let images = builder.tableItem(withIdStartsWith: "offer_image_\(newOffer.id)")!.cellHelper
        let characteristics = builder.tableItem(withIdStartsWith: "offer_characteristics")!.cellHelper
        let body = builder.tableItem(withIdStartsWith: "offer_\(newOffer.id)")!.cellHelper

        Snapshot.compareWithSnapshot(
            cellHelper: title,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            interfaceStyle: [.light],
            identifier: "\(id)_title"
        )

        do {
            images.precalculateLayout(indexPath: .init(row: 0, section: 0), width: DeviceWidth.iPhone11)
            let imagesView = images.createCellView(width: DeviceWidth.iPhone11) as! UICollectionView

            Snapshot.compareWithSnapshot(
                view: imagesView,
                interfaceStyle: [.light],
                identifier: "\(id)_images"
            )
        }

        Snapshot.compareWithSnapshot(
            cellHelper: characteristics,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            interfaceStyle: [.light],
            identifier: "\(id)_characteristics"
        )

        Snapshot.compareWithSnapshot(
            cellHelper: body,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface,
            interfaceStyle: [.light],
            identifier: "\(id)_body"
        )
    }
}
