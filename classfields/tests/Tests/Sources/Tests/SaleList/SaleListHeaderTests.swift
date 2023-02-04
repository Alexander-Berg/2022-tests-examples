import XCTest
import AutoRuProtoModels
import AutoRuModels
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots

@testable import AutoRuViews
@testable import AutoRuSaleCard
@testable import AutoRuCellHelpers

class SaleListHeaderTests: BaseUnitTest {

    private lazy var offer: Auto_Api_Offer = (.init(mockFile: "SaleListHeaderTests_single-offer") as Auto_Api_OfferListingResponse).offers.first!

    override func setUp() {
        super.setUp()

        FetchableImage.blockThreadUntilFinished = true
    }

    override func tearDown() {
        super.tearDown()

        FetchableImage.blockThreadUntilFinished = false
    }

    // MARK: -

    func test_snippetBooked() {
        Step("Проверякм наличие иконки+бейджа \"Автомобиль забронирован\"")
        checkModel(makeSnippetModel(offer: offer.setBookedByMe(false)))
    }

    func test_snippetBookedByMe() {
        Step("Проверякм наличие иконки+бейджа \"Забронирован вами\"")
        checkModel(makeSnippetModel(offer: offer.setBookedByMe()))
    }

    // MARK: -

    func test_plainPriceNew() {
        Step("Проверям обычное состояние хедера для новых авто")
        checkModel(
            makeSnippetModel(offer: offer.setNew())
        )
    }

    func test_plainPriceUsed() {
        Step("Проверям обычное состояние хедера для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer.setUsed())
        )
    }

    // MARK: -

    func test_plainPriceGoodDealNew() {
        Step("Проверям наличие бейджа \"Хорошая цена\" для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTag(.goodDeal)
            )
        )
    }

    func test_plainPriceGoodDealUsed() {
        Step("Проверям наличие бейджа \"Хорошая цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTag(.goodDeal)
            )
        )
    }

    // MARK: -

    func test_plainPriceGreatDealNew() {
        Step("Проверям наличие бейджа \"Отличная цена\" для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTag(.greatDeal)
            )
        )
    }

    func test_plainPriceGreatDealUsed() {
        Step("Проверям наличие бейджа \"Отличная цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTag(.greatDeal)
            )
        )
    }

    func test_plainPriceGreatDealNewDealer() {
        Step("Проверям наличие бейджа \"Отличная цена\" для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTag(.greatDeal)
                                .setDealer()
            )
        )
    }

    func test_plainPriceGreatDealUsedDealer() {
        Step("Проверям наличие бейджа \"Отличная цена\" для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTag(.greatDeal)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_highlightedPriceNew() {
        Step("Проверям выделение цены цветом для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .setHighlightedPriceVAS()
            )
        )
    }

    func test_highlightedPriceUsed() {
        Step("Проверям выделение цены цветом для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .setHighlightedPriceVAS()
            )
        )
    }

    // MARK: -

    func test_highlightedPriceGoodDealNew() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Хорошая цена\" для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .setHighlightedPriceVAS()
                                .addTag(.goodDeal)
            )
        )
    }

    func test_highlightedPriceGoodDealUsed() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Хорошая цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .setHighlightedPriceVAS()
                                .addTag(.goodDeal)
                            )
        )
    }

    // MARK: -

    func test_highlightedPriceGreatDealNew() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Отличная цена\" для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .setHighlightedPriceVAS()
                                .addTag(.greatDeal)
            )
        )
    }

    func test_highlightedPriceGreatDealUsed() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Отличная цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .setHighlightedPriceVAS()
                                .addTag(.greatDeal)
            )
        )
    }

    func test_highlightedPriceGreatDealNewDealer() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Отличная цена\" для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .setHighlightedPriceVAS()
                                .addTag(.greatDeal)
                                .setDealer()
                                .addDeliveryInfo()
            )
        )
    }

    func test_highlightedPriceGreatDealUsedDealer() {
        Step("Проверям выделение цены цветом и наличие бейджа \"Отличная цена\" для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .setHighlightedPriceVAS()
                                .addTag(.greatDeal)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownNew() {
        Step("Проверям изменение цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTag(.priceChange)
                                .addPriceHistory(price: 15_000_000)
                                .addPriceHistory(price: 13_000_000)
                            )
        )
    }

    func test_priceHistoryDownNewDealer() {
        Step("Проверям изменение цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTag(.priceChange)
                                .addPriceHistory(price: 15_000_000)
                                .addPriceHistory(price: 13_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownUsed() {
        Step("Проверям перечеркнутую старую цену для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTag(.priceChange)
                                .addPriceHistory(price: 15_000_000)
                                .addPriceHistory(price: 13_000_000)
            )
        )
    }

    func test_priceHistoryDownUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTag(.priceChange)
                                .addPriceHistory(price: 15_000_000)
                                .addPriceHistory(price: 13_000_000)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownGoodDealNew() {
        Step("Проверям наличие бейджа \"Хорошая цена\" при изменении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .goodDeal)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownGoodDealNewDealer() {
        Step("Проверям наличие бейджа \"Хорошая цена\" при изменении цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .goodDeal)
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownGoodDealUsed() {
        Step("Проверям перечеркнутую старую цену и наличие бейджа \"Хорошая цена\" при изменении цены для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .goodDeal)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownGoodDealUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены и наличие бейджа \"Хорошая цена\" для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .goodDeal)
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownGreatDealNew() {
        Step("Проверям наличие бейджа \"Отличная цена\" при изменении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .greatDeal)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownGreatDealNewDealer() {
        Step("Проверям наличие наличие бейджа \"Отличная цена\" при изменении цены для для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .greatDeal)
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownGreatDealUsed() {
        Step("Проверям перечеркнутую старую цену и наличие бейджа \"Отличная цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .greatDeal)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownGreatDealUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены и наличие бейджа \"Отличная цена\" для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .greatDeal)
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownGoodAndGreatDeal() {
        Step("Проверям перечеркнутую старую цену и наличие бейджа \"Отличная цена\" при двух тегах: \"Хорошая цена\" и \"Отличная цена\"")
        checkModel(
            makeSnippetModel(offer: offer
                                .addTags(.priceChange, .greatDeal, .greatDeal)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryUpHighlightedNew() {
        Step("Проверям выделение цены цветом при снижении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }
    // MARK: -

    func test_priceHistoryDownHighlightedNew() {
        Step("Проверям выделение цены цветом при снижении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedNewDealer() {
        Step("Проверям выделение цены цветом при снижении цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownHighlightedUsed() {
        Step("Проверям перечеркнутую старую цену и выделение новой цены цветом для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedUsedDealer() {
        Step("Проверям выделение цветом и наличие стелки-идикатора снижения цены для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownHighlightedGoodDealNew() {
        Step("Проверям выделение цветом и наличие бейджа \"Хорошая цена\" при снижении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .goodDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedGoodDealNewDealer() {
        Step("Проверям выделение цветом и наличие бейджа \"Хорошая цена\" при снижении цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .goodDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownHighlightedGoodDealUsed() {
        Step("Проверям перечернутую старую цену, выделение цветом новой и наличие бейджа \"Хорошая цена\" при снижении цены для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .goodDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedGoodDealUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены, выделение цветом и наличие бейджа \"Хорошая цена\" при снижении цены для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .goodDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownHighlightedGreatDealNew() {
        Step("Проверям выделение цветом и наличие бейджа \"Отличная цена\" при снижении цены для новых авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .greatDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedGreatDealNewDealer() {
        Step("Проверям выделение цветом и наличие бейджа \"Отличная цена\" при снижении цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setNew()
                                .addTags(.priceChange, .greatDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    func test_priceHistoryDownHighlightedGreatDealUsed() {
        Step("Проверям перечернутую старую цену, выделение цветом новой и наличие бейджа \"Отличная цена\" при снижении цены для б/у авто")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .greatDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
            )
        )
    }

    func test_priceHistoryDownHighlightedGreatDealUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены, выделение цветом и наличие бейджа \"Отличная цена\" при снижении цены для б/у авто у дилера")
        checkModel(
            makeSnippetModel(offer: offer
                                .setUsed()
                                .addTags(.priceChange, .greatDeal)
                                .setHighlightedPriceVAS()
                                .addPriceHistory(price: 15_000_000)
                                .setDealer()
            )
        )
    }

    // MARK: -

    func test_plainPriceAllowedForCreditNew() {
        Step("Проверям наличие тайтла с начальной ценой кредита для новых авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTag(.allowedForCredit)
                    .addSharkInfo(),
                showCredit: true
            )
        )
    }

    func test_plainPriceAllowedForCreditNewDealer() {
        Step("Проверям наличие тайтла с начальной ценой кредита для новых авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTag(.allowedForCredit)
                    .addSharkInfo()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    func test_plainPriceAllowedForCreditUsed() {
        Step("Проверям наличие тайтла с начальной ценой кредита для б/у авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTag(.allowedForCredit)
                    .addSharkInfo(),
                showCredit: true
            )
        )
    }

    func test_plainPriceAllowedForCreditUsedDealer() {
        Step("Проверям наличие тайтла с начальной ценой кредита для б/у авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTag(.allowedForCredit)
                    .addSharkInfo()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    // MARK: -

    func test_priceHistoryDownAllowedForCreditNew() {
        Step("Проверям наличие тайтла с начальной ценой кредита при снижении цены для новых авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTags(.priceChange, .allowedForCredit)
                    .addPriceHistory(price: 15_000_000)
                    .addSharkInfo()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    func test_priceHistoryDownAllowedForCreditNewDealer() {
        Step("Проверям наличие тайтла с начальной ценой кредита при снижении цены для новых авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTags(.priceChange, .allowedForCredit)
                    .addPriceHistory(price: 15_000_000)
                    .addSharkInfo()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    func test_priceHistoryDownAllowedForCreditUsed() {
        Step("Проверям перечернутую старую цену, наличие тайтла с начальной ценой кредита при снижении цены для б/у авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTags(.priceChange, .allowedForCredit)
                    .addPriceHistory(price: 15_000_000)
                    .addSharkInfo(),
                showCredit: true
            )
        )
    }

    func test_priceHistoryDownAllowedForCreditUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены и тайтла с начальной ценой кредита при снижении цены для б/у авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTags(.priceChange, .allowedForCredit)
                    .addPriceHistory(price: 15_000_000)
                    .addSharkInfo()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    // MARK: -

    func test_plainPriceHasMaxDiscountNew() {
        Step("Проверям наличие в цене префикса \"От\" при максимальной скидке для новых авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTag(.hasDiscount)
                    .setMaxDiscount()
            )
        )
    }

    func test_plainPriceHasMaxDiscountNewDealer() {
        Step("Проверям наличие в цене префикса \"От\" при максимальной скидке для новых авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTag(.hasDiscount)
                    .setMaxDiscount()
                    .setDealer()
            )
        )
    }

    func test_plainPriceHasMaxDiscountUsed() {
        Step("Проверям наличие в цене префикса \"От\" при максимальной скидке для б/у авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTag(.hasDiscount)
                    .setMaxDiscount()
            )
        )
    }

    func test_plainPriceHasMaxDiscountUsedDealer() {
        Step("Проверям наличие сабтайтла \"... со скидками\" при максимальной скидке для б/у авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTag(.hasDiscount)
                    .setMaxDiscount()
                    .setDealer()
            )
        )
    }

    // MARK: -

    func test_plainPriceHasMaxDiscountAllowedForCreditNew() {
        Step("Проверям наличие в цене префикса \"От\" и тайтла с начальной ценой кредита при максимальной скидке для новых авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTags(.hasDiscount, .allowedForCredit)
                    .addSharkInfo()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditNewDealer() {
        Step("Проверям наличие в цене префикса \"От\" и тайтла с начальной ценой кредита при максимальной скидке для новых авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTags(.hasDiscount, .allowedForCredit)
                    .addSharkInfo()
                    .setMaxDiscount()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditUsed() {
        Step("Проверям наличие в цене префикса \"От\" и тайтла с начальной ценой кредита при максимальной скидке для б/у авто")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTags(.hasDiscount, .allowedForCredit)
                    .addSharkInfo()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }

    func test_plainPriceHasMaxDiscountAllowedForCreditUsedDealer() {
        Step("Проверям наличие тайтла с начальной ценой кредита при максимальной скидке для б/у авто у дилера")
        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTags(.hasDiscount, .allowedForCredit)
                    .addSharkInfo()
                    .setMaxDiscount()
                    .setDealer(),
                showCredit: true
            )
        )
    }

    // MARK: -

    func test_plainPriceDownHasMaxDiscountAllowedForCreditGreatDealBookedByMeNew() {
        Step("Проверям наличие префикса \"От\" в цене" +
                "тайтла с начальной ценой кредита" +
                "бейджа \"Отличная цена\"" +
                "иконки+бейджа \"Забронирован вами\" при максимальной скидке для новых авто"
        )

        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .addTags(.priceChange, .hasDiscount, .allowedForCredit, .greatDeal)
                    .addPriceHistory(price: 15_000_000)
                    .addPriceHistory(price: 13_000_000)
                    .addSharkInfo()
                    .setBookedByMe()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }

    func test_plainPriceDownHasMaxDiscountAllowedForCreditGreatDealBookedByMeNewDealer() {
        Step("Проверям наличие префикса \"От\" в цене " +
                "тайтла с начальной ценой кредита " +
                "бейджа \"Отличная цена\" " +
                "иконки+бейджа \"Забронирован вами\" при максимальной скидке для новых авто у дилера"
        )

        checkModel(
            makeSnippetModel(
                offer: offer
                    .setNew()
                    .setDealer()
                    .addTags(.priceChange, .hasDiscount, .allowedForCredit, .greatDeal)
                    .addPriceHistory(price: 15_000_000)
                    .addPriceHistory(price: 13_000_000)
                    .addSharkInfo()
                    .setBookedByMe()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }

    func test_plainPriceDownHasMaxDiscountAllowedForCreditGreatDealBookedByMeUsed() {
        Step("Проверям перечернутую старую цену" +
                "наличие префикса \"От\" в цене " +
                "тайтла с начальной ценой кредита " +
                "бейджа \"Отличная цена\" " +
                "иконки+бейджа \"Забронирован вами\" при максимальной скидке для новых б/у авто"
        )

        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .addTags(.priceChange, .hasDiscount, .allowedForCredit, .greatDeal)
                    .addPriceHistory(price: 15_000_000)
                    .addPriceHistory(price: 13_000_000)
                    .addSharkInfo()
                    .setBookedByMe()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }

    func test_plainPriceDownHasMaxDiscountAllowedForCreditGreatDealBookedByMeUsedDealer() {
        Step("Проверям наличие стелки-идикатора снижения цены " +
                "тайтла с начальной ценой кредита " +
                "бейджа \"Отличная цена\" " +
                "наличие сабтайтла \"... со скидками\" " +
                "иконки+бейджа \"Забронирован вами\" при максимальной скидке для новых б/у авто у дилера"
        )

        checkModel(
            makeSnippetModel(
                offer: offer
                    .setUsed()
                    .setDealer()
                    .addTags(.priceChange, .hasDiscount, .allowedForCredit, .greatDeal)
                    .addPriceHistory(price: 15_000_000)
                    .addPriceHistory(price: 13_000_000)
                    .addSharkInfo()
                    .setBookedByMe()
                    .setMaxDiscount(),
                showCredit: true
            )
        )
    }
}

private extension SaleListHeaderTests {
    func checkModel(_ model: SaleSnippetModel, id: String = #function) {
        let creator = SaleSnippetViewCreator(model: model, extended: false, showSold: false) {}
        let size = creator.sizeForWidth(min(DeviceWidth.iPhone11, 400))
        let view = creator.createViewWithSize(size)
        Snapshot.compareWithSnapshot(view: view, identifier: id)
        let extendedCreator = SaleSnippetViewCreator(model: model, extended: true, showSold: false)
        let extendedSize = extendedCreator.sizeForWidth(min(DeviceWidth.iPhone11, 400))
        let extendedView = extendedCreator.createViewWithSize(extendedSize)
        Snapshot.compareWithSnapshot(view: extendedView, identifier: id+"_extended")
    }

    func makeSnippetModel(offer: Auto_Api_Offer, showCredit: Bool = false) -> SaleSnippetModel {
        let model = SaleSnippetModel.modelForData(
            offer: offer,
            peeked: false,
            favorite: false,
            canAddToComparison: false,
            isInComparison: false,
            userNote: nil,
            phoneState: .received(.init()),
            cachedBookedOffers: [:],
            creditInfo: nil,
            currentImageIndex: nil,
            hasChatBotFeature: false,
            geoRadius: nil,
            isExtendedRadius: false,
            showExtendedDate: false,
            shouldShowNewBadge: false,
            hasChatAccess: false,
            shouldShowCreditPriceOnSnippet: showCredit,
            isDealer: false
        )
        return model
    }
}
