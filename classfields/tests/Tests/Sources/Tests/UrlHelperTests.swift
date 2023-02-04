import AutoRuAppRouting
import AutoRuLegacyFilters
import AutoRuModels
import Foundation
import XCTest

final class UrlHelperTests: BaseUnitTest {

    func testActionsParse() {
        let urlToAction = [
            "https://auto.ru/cars/mazda/cx_5/20939955/used/body-allroad/":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/cars/mazda/cx_5/20939955/used/body-allroad/"),
            "https://auto.ru/motorcycle/suzuki/all/":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/motorcycle/suzuki/all/"),
            "https://auto.ru/lcv/volkswagen/all/":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/lcv/volkswagen/all/"),
            "https://m.auto.ru/user/offer/cars/1092291352-6f668c19":
                RemoteAction.openUserSale(
                    saleInfoReference: AutoRuModels
                        .SaleInfoReference(saleID: "1092291352-6f668c19", parentCategoryAlias: "cars"),
                    openReport: false
                ),
            "https://auto.ru/review/cars/chevrolet/aveo/7857603/372806981243904005/":
                RemoteAction.openReview(reviewId: "372806981243904005", category: "cars"),
            "https://m.auto.ru/cars/used/sale/mercedes/v_klasse/1092291352-6f668c19/?from=mobweb_block":
                RemoteAction.openSale(
                    saleInfoReference: AutoRuModels
                        .SaleInfoReference(saleID: "1092291352-6f668c19", parentCategoryAlias: "cars"),
                    scrollToHistory: false,
                    shouldOpenListing: true,
                    isFromPushAfterCall: false
                ),
            "https://m.auto.ru/lcv/":
                RemoteAction.openSaleListParsing(link: "https://m.auto.ru/lcv/"),
            "https://m.auto.ru/geo/":
                RemoteAction.openGeoFilter,
            "https://mag.auto.ru":
                RemoteAction.openURL(url: URL(string: "https://mag.auto.ru?from=app&only-content=true")!),
            "https://m.auto.ru/moskva/motorcycle/":
                RemoteAction.openSaleListParsing(link: "https://m.auto.ru/moskva/motorcycle/"),
            "https://m.auto.ru/motorcycle/":
                RemoteAction.openSaleListParsing(link: "https://m.auto.ru/motorcycle/"),
            "https://m.auto.ru/promo/from-web-to-app/":
                RemoteAction.regularStart,
            "https://m.auto.ru/rossiya/":
                RemoteAction.openSaleListParsing(link: "https://m.auto.ru/rossiya/"),
            "https://m.auto.ru/rossiya/cars/all/?catalog_filter=mark%3DMERCEDES,model%3DSLK_KLASSE,generation%3D4760233&catalog_filter=mark%3DMERCEDES,model%3DSLK_KLASSE,generation%3D3484134&from=m.shelkov.r170":
                RemoteAction
                .openSaleListParsing(
                    link: "https://m.auto.ru/rossiya/cars/all/?catalog_filter=mark=MERCEDES,model=SLK_KLASSE,generation=4760233&catalog_filter=mark=MERCEDES,model=SLK_KLASSE,generation=3484134&from=m.shelkov.r170"
                ),
            "https://m.auto.ru/rossiya/cars/kia/seltos/new/?from=wizard.model&sort=fresh_relevance_1-desc":
                RemoteAction
                .openSaleListParsing(
                    link: "https://m.auto.ru/rossiya/cars/kia/seltos/new/?from=wizard.model&sort=fresh_relevance_1-desc"
                ),
            "https://media.auto.ru/review/cars/lifan/solano/20845463/3144730500783665345/":
                RemoteAction.openReview(reviewId: "3144730500783665345", category: "cars"),
            "https://media.auto.ru/review/moto/motorcycle/kawasaki/z_250/4232517270043835068/?utm_referrer=https:%2F%2Fzen.yandex.com":
                RemoteAction.openReview(reviewId: "4232517270043835068", category: "moto"),
            "https://media.auto.ru/review/trucks/lcv/rocar/tv_14_15/8436793410195256168/?utm_referrer=https:%2F%2Fzen.yandex.com":
                RemoteAction.openReview(reviewId: "8436793410195256168", category: "trucks"),
            "https://media.auto.ru/reviews/cars/cadillac/srx/2305747/?utm_referrer=https:%2F%2Fzen.yandex.com":
                RemoteAction.openReviewList(
                    category: "cars",
                    subCategory: nil,
                    mark: "cadillac",
                    model: "srx",
                    generatiron: "2305747"
                ),
            "https://auto.ru/review/cars/toyota/land_cruiser_prado/21090216/7481895211383295911/?utm_referrer=https:%2F%2Fzen.yandex.com":
                RemoteAction.openReview(reviewId: "7481895211383295911", category: "cars"),
            "https://auto.ru/":
                RemoteAction.regularStart,
            "https://m.auto.ru/":
                RemoteAction.regularStart,
            "https://auto.ru/promo/from-web-to-app/":
                RemoteAction.regularStart,
            "https://m.auto.ru/?from=wizard.brand&geo_id=213":
                RemoteAction.regularStart,
            "https://m.auto.ru/?from=wizard.model&geo_id=225":
                RemoteAction.regularStart,
            "https://m.auto.ru/?nosplash=1":
                RemoteAction.regularStart,
            "https://m.auto.ru/?utm_referrer=https:%2F%2Fyandex.ru%2F%3Ffrom%3Dalice":
                RemoteAction.regularStart,
            "https://m.auto.ru/?from=mobweb_block":
                RemoteAction.regularStart,
            "https://auto.ru/history":
                RemoteAction.openCarReportMenu,
            "https://pro.auto.ru":
                RemoteAction.openCarReportMenu,
            "https://auto.ru/serve/xxx":
                RemoteAction.regularStart,
            "https://auto.ru/add":
                RemoteAction.openAddSale,
            "https://m.auto.ru/like/cars/":
                RemoteAction.openFavorites,
            "https://auto.ru/moskva/":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/moskva/"),
            "https://auto.ru/moskva/cars/":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/moskva/cars/"),
            "https://auto.ru/lcv/new":
                RemoteAction.openSaleListParsing(link: "https://auto.ru/lcv/new"),
            "https://auto.ru/moscow/add":
                RemoteAction.openAddSale,
            "https://auto.ru/cars/used/":
                .openSaleListParsing(link: "https://auto.ru/cars/used/"),
            "https://auto.ru/cars/used/add":
                RemoteAction.openAddSale,
            "https://auto.ru/cars/evaluation/":
                RemoteAction.openEstimation,
            "https://auto.ru/cars/used/sale/mercedes/e_klasse/1093571834-4df777a4/":
                RemoteAction.openSale(
                    saleInfoReference: SaleInfoReference(saleID: "1093571834-4df777a4", parentCategoryAlias: "cars"),
                    scrollToHistory: false,
                    shouldOpenListing: true,
                    isFromPushAfterCall: false
                ),
            "https://auto.ru/cars/used/sale/mercedes/e_klasse/1093571834-4df777a4/history":
                RemoteAction.openSale(
                    saleInfoReference: SaleInfoReference(saleID: "1093571834-4df777a4", parentCategoryAlias: "cars"),
                    scrollToHistory: true,
                    shouldOpenListing: true,
                    isFromPushAfterCall: false
                ),
            "https://auto.ru/cars/used/sale/bmw/x6/1097249152-68dc8680/#block-pts":
                RemoteAction.openSale(
                    saleInfoReference: SaleInfoReference(saleID: "1097249152-68dc8680", parentCategoryAlias: "cars"),
                    scrollToHistory: false,
                    shouldOpenListing: true,
                    isFromPushAfterCall: false
                ),
            "https://auto.ru/moskva/cars/used/?mark_model_nameplate=FORD%23MONDEO%23%236519702&sort=fresh_relevance_1-desc&body_type_group=SEDAN&body_type_group=WAGON&price_from=50000&price_to=2500000&seller_group=PRIVATE&geo_id=2%2C213":
                RemoteAction
                .openSaleListParsing(
                    link: "https://auto.ru/moskva/cars/used/?mark_model_nameplate=FORD%23MONDEO%23%236519702&sort=fresh_relevance_1-desc&body_type_group=SEDAN&body_type_group=WAGON&price_from=50000&price_to=2500000&seller_group=PRIVATE&geo_id=2%2C213"
                ),
            "https://auto.ru/sankt-peterburg/cars/bmw/all/?sort=fresh_relevance_1-desc":
                RemoteAction
                .openSaleListParsing(link: "https://auto.ru/sankt-peterburg/cars/bmw/all/?sort=fresh_relevance_1-desc"),
            "https://auto.ru/sankt-peterburg/bmw/all/?sort=fresh_relevance_1-desc":
                RemoteAction
                .openSaleListParsing(link: "https://auto.ru/sankt-peterburg/bmw/all/?sort=fresh_relevance_1-desc"),
            "https://auto.ru/diler/cars/new/pragmatika_lada_kupchino?from=card":
                RemoteAction.openDealerCard(dealerCode: "pragmatika_lada_kupchino"),
            "https://auto.ru/cars/new/group/vaz/granta/21377296-21377429/":
                RemoteAction.openStockCard(
                    url: URL(string: "https://auto.ru/cars/new/group/vaz/granta/21377296-21377429/")!,
                    isOffer: false
                ),
            "https://auto.ru/cars/new/group/bmw/6er/21322577/21773333/1096132284-1f18e18b":
                RemoteAction.openStockCard(
                    url: URL(string: "https://auto.ru/cars/new/group/bmw/6er/21322577/21773333/1096132284-1f18e18b")!,
                    isOffer: true
                ),
            "https://auto.ru/cars/new/group/vaz/granta/21377296-21377429":
                RemoteAction.openStockCard(
                    url: URL(string: "https://auto.ru/cars/new/group/vaz/granta/21377296-21377429")!,
                    isOffer: false
                ),
            "https://auto.ru/article":
                RemoteAction.openURL(url: URL(string: "https://auto.ru/article?from=app&only-content=true")!),
            "autoru://app/chat/1234/":
                RemoteAction.openChat(chatId: "1234"),
            "https://auto.ru/cars/used/edit/xxx123":
                RemoteAction.openEditForm(saleRef: SaleInfoReference(saleID: "xxx123", parentCategoryAlias: "cars")),
            "https://auto.ru/cars/edit/xxx123":
                RemoteAction.openEditForm(saleRef: SaleInfoReference(saleID: "xxx123", parentCategoryAlias: "cars")),
            "autoru://app/cars/used/edit/xxx123":
                RemoteAction.openEditForm(saleRef: SaleInfoReference(saleID: "xxx123", parentCategoryAlias: "cars")),
            "autoru://app/users.auto.ru/sales":
                RemoteAction.openMySales,
            "https://auto.ru/cars/all/?show-searches=true":
                RemoteAction.openSavedSearches,
            "autoru://app/story/123/3":
                RemoteAction.openStory(id: "123", slideIndex: 3),
            "https://auto.ru/app/story/123/3":
                RemoteAction.openStory(id: "123", slideIndex: 3),
            "https://auto.ru/story/123/3":
                RemoteAction.openStory(id: "123", slideIndex: 3),
            "https://auto.ru/history/o261to178":
                RemoteAction.openCarReportByVinOrGovNumber("o261to178"),
            "https://auto.ru/history/":
                RemoteAction.openCarReportMenu,
            "autoru://app/techsupport":
                RemoteAction.openTechsupport(openCamera: false),
            "autoru://app/techsupport/camera":
                RemoteAction.openTechsupport(openCamera: true),
            "https://auto.ru/recalls":
                RemoteAction.openRecalls,
            "https://media.auto.ru/review/cars/bmw/1er/7707451/5557496402602234049/?category=cars&from=card":
                RemoteAction.openReview(reviewId: "5557496402602234049", category: "cars"),
            "https://media.auto.ru/review/moto/moto/bmw/7707451/5557496402602234049/?category=cars&from=card":
                RemoteAction.openReview(reviewId: "5557496402602234049", category: "moto"),
            "https://auto.ru/cars/reviews/add/1051167380-1d285":
                RemoteAction.editReview(offerId: "1051167380-1d285", category: "cars", from: nil, rvwCampaign: nil),
            "https://auto.ru/cars/reviews/add/1051167380-1d285?from=test":
                RemoteAction.editReview(offerId: "1051167380-1d285", category: "cars", from: "test", rvwCampaign: nil),
            "https://auto.ru/cars/reviews/add/1051167380-1d285?rvw_campaign=test":
                RemoteAction.editReview(offerId: "1051167380-1d285", category: "cars", from: nil, rvwCampaign: "test"),
            "https://auto.ru/cars/reviews/add/1051167380-1d285?rvw_campaign=test1&from=test2":
                RemoteAction.editReview(
                    offerId: "1051167380-1d285",
                    category: "cars",
                    from: "test2",
                    rvwCampaign: "test1"
                ),
            "https://auto.ru/moto/reviews/add/":
                RemoteAction.addReview(category: "moto"),
            "https://auto.ru/my/profile/":
                RemoteAction.openMyProfile,
            "https://auto.ru/diler-oficialniy/cars/all/zvezda_stolici_kashirka_moskva_mercedes/":
                RemoteAction.openDealerCard(dealerCode: "zvezda_stolici_kashirka_moskva_mercedes"),
            "https://auto.ru/diler-oficialniy/cars/new/avtomir_moskva_mitsubishi":
                RemoteAction.openDealerCard(dealerCode: "avtomir_moskva_mitsubishi"),
            "https://auto.ru/diler/cars/all/inkom_avto_moskva/chevrolet/tahoe/?with_warranty=true":
                RemoteAction.openDealerCard(dealerCode: "inkom_avto_moskva"),
            "https://auto.ru/diler/cars/all/inkom_avto_moskva/chevrolet/?with_warranty=true":
                RemoteAction.openDealerCard(dealerCode: "inkom_avto_moskva"),
            "https://auto.ru/diler/cars/used/inkom_avto_moskva/":
                RemoteAction.openDealerCard(dealerCode: "inkom_avto_moskva"),
            "autoru://app/autoru-pdd":
                RemoteAction.openURL(url: URL(string: "https://autoru-pdd.ru/?utm_source=push")!),
            "https://auto.ru/compare":
                RemoteAction.openOffersComparison,
            "https://auto.ru/compare-offers":
                RemoteAction.openOffersComparison,
            "https://auto.ru/compare-models":
                RemoteAction.openModelsComparison,
            "https://m.auto.ru/moskva/cars/new/get_best_price":
                RemoteAction.openSaleListParsing(link: "https://m.auto.ru/moskva/cars/new/get_best_price"),
            "https://freshauto.ru/":
                RemoteAction.openURL(url: URL(string: "https://freshauto.ru/")!),
            "https://auto.ru/my/credits":
                RemoteAction.openCredit,
            "https://auto.ru/my/credits/edit":
                RemoteAction.openCredit,
            "https://auto.ru/my/credits/wizard":
                RemoteAction.openCredit,
            "https://auto.ru/garage/share/101010":
                RemoteAction.openPublicGarageCard(id: "101010"),
            "https://auto.ru/garage/landing":
                RemoteAction.openGarage,
            "https://auto.ru/garage":
                RemoteAction.openGarage,
            "https://auto.ru/my/deals":
                RemoteAction.openSafeDealList,
            "autoru://app/store":
                RemoteAction.openAppStore,
            "https://auto.ru/s/I6wMJE":
                RemoteAction.openExpandedURL
        ]

        let regionServiceMock = RegionServiceMock(knownAliases: ["moskva", "sankt-peterburg", "rossiya"])

        let helper = UrlHelper(regionService: regionServiceMock)

        for (urlString, waitedAction) in urlToAction {
            guard let url = URL(string: urlString) else {
                XCTFail("Can't create URL from: \(urlString)")
                continue
            }
            XCTContext.runActivity(named: "Проверяем URL \(urlString): Ожидаем \(waitedAction)") { _ in
                let parsedAction = helper.actionForWebURL(url)
                if parsedAction != waitedAction {
                    XCTAssert(false, "Wrong action \(parsedAction) instead \(waitedAction) for \(urlString)")
                }
            }
        }
    }
}

private struct RegionServiceMock: RegionService {
    var knownAliases: Set<String> = []

    func regionAliasExists(_ alias: String) -> Bool {
        knownAliases.contains(alias)
    }
}
