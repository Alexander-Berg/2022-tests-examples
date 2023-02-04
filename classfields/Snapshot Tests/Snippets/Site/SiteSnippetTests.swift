//
//  SiteSnippetTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 26.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import XCTest
import YRETestsUtils
import YRECoreUI
import YREModel
import YREModelObjc
import YREModelHelpers
@testable import YRESnippets

final class SiteSnippetTests: XCTestCase {
    func testSiteSnippet() {
        let defaultViewModel = Self.makeViewModel()
        let view = Self.makeView(viewModel: defaultViewModel)
        self.assertSnapshot(view)
    }

    func testSiteSnippetWithQuartography() {
        let quartographyViewModel: [SiteSnippetViewModel.QuartographyViewModel] = [
            .init(title: "Студии", area: .areaFrom("от 34,5 м²"), price: .priceFrom("от 9 362 440 ₽")),
            .init(title: "1-комн.", area: .areaFrom("от 43,6 м²"), price: .priceFrom("от 12 280 290 ₽")),
            .init(title: "2-комн.", area: .areaFrom("от 52,2 м²"), price: .priceFrom("от 16 217 880 ₽")),
            .init(title: "3-комн.", area: .areaFrom("от 72,9 м²"), price: .priceFrom("от 19 148 580 ₽")),
        ]
        let viewModel = Self.makeViewModel(quartographyViewModel: quartographyViewModel)
        let view = Self.makeView(viewModel: viewModel)
        self.assertSnapshot(view)
    }

    func testSiteSnippetWithLongQuartography() {
        let quartographyViewModel: [SiteSnippetViewModel.QuartographyViewModel] = [
            .init(title: "Студии", area: .areaFrom("от 1,0 м²"), price: .priceFrom("от 1 ₽")),
            .init(title: "3-комн.", area: .areaFrom("от 72,9 м²"), price: .priceFrom("от 19 148 580 ₽")),
            .init(title: "4+ комн.", area: .areaFrom("от 100003251,1 м²"), price: .priceFrom("от 1,1 млрд ₽")),
        ]
        let viewModel = Self.makeViewModel(quartographyViewModel: quartographyViewModel)
        let view = Self.makeView(viewModel: viewModel)
        self.assertSnapshot(view)
    }

    func testSiteSnippetWithUnknownQuartography() {
        let quartographyViewModel: [SiteSnippetViewModel.QuartographyViewModel] = [
            .init(title: "Студии", area: .areaFrom("от 34,5 м²"), price: .priceFrom("-")),
            .init(title: "1-комн.", area: .areaFrom("от 43,6 м²"), price: .priceFrom("временно нет в продаже")),
            .init(title: "2-комн.", area: .areaFrom("-"), price: .priceFrom("все проданы")),
            .init(title: "3-комн.", area: .areaFrom("от 72,9 м²"), price: .priceFrom("скоро в продаже")),
            .init(title: "4+ комн.", area: .areaFrom("-"), price: .priceFrom("цена по телефону")),
        ]
        let viewModel = Self.makeViewModel(quartographyViewModel: quartographyViewModel)
        let view = Self.makeView(viewModel: viewModel)
        self.assertSnapshot(view)
    }
}

extension SiteSnippetTests {
    // MARK: - Badges

    func testBadgeMortgage() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [.mortgage])
        self.snapshotTest(with: snippet)
    }

    func testBadgeDiscount() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [.discount])
        self.snapshotTest(with: snippet)
    }

    func testBadgeSale() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [.sale])
        self.snapshotTest(with: snippet)
    }

    func testBadgeGift() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [.gift])
        self.snapshotTest(with: snippet)
    }

    func testBadgeInstallment() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [.installment])
        self.snapshotTest(with: snippet)
    }

    func testAllBadge() {
        let snippet = Self.makeSiteSnippet(
            summarySpecialProposals: [.mortgage, .discount, .sale, .gift, .installment]
        )
        self.snapshotTest(with: snippet)
    }

    func testNoBadge() {
        let snippet = Self.makeSiteSnippet(summarySpecialProposals: [])
        self.snapshotTest(with: snippet)
    }

    // MARK: - Building state

    func testBuildingStateUnfinished() {
        let siteDescription = Self.makeSiteDescription(buildingState: .unfinished)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateUnfinishedAndHandOver() {
        let siteDescription = Self.makeSiteDescription(buildingState: .handOver)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateBuilt() {
        let siteDescription = Self.makeSiteDescription(buildingState: .built)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateHandOver() {
        let siteDescription = Self.makeSiteDescription(buildingState: .handOver)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateSuspended() {
        let siteDescription = Self.makeSiteDescription(buildingState: .suspended)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateSuspendedAndHandOver() {
        let siteDescription = Self.makeSiteDescription(buildingState: .suspended)
        let snippet = Self.makeSiteSnippet(
            siteDescription: siteDescription,
            deliveryDates: [
                .init(year: .init(value: 2020), quarter: .init(value: 2), finished: .paramBoolTrue),
                .init(year: .init(value: 2019), quarter: .init(value: 2), finished: .paramBoolTrue),
                .init(year: .init(value: 2022), quarter: .init(value: 4), finished: .paramBoolFalse),
            ]
        )
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateSuspendedInProject() {
        let siteDescription = Self.makeSiteDescription(buildingState: .suspended)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription, flatStatus: .inProject)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateSuspendedInProjectWithDeliveryDates() {
        let siteDescription = Self.makeSiteDescription(buildingState: .suspended)
        let snippet = Self.makeSiteSnippet(
            siteDescription: siteDescription,
            deliveryDates: [
                .init(year: .init(value: 2020), quarter: .init(value: 2), finished: .paramBoolTrue),
                .init(year: .init(value: 2019), quarter: .init(value: 2), finished: .paramBoolTrue),
                .init(year: .init(value: 2022), quarter: .init(value: 4), finished: .paramBoolFalse),
            ],
            flatStatus: .inProject
        )
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateUnknown() {
        let siteDescription = Self.makeSiteDescription(buildingState: .unknown)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testBuildingStateUnknownAndFlatStatusSoonAvailable() {
        let siteDescription = Self.makeSiteDescription(buildingState: .unknown)
        let snippet = Self.makeSiteSnippet(siteDescription: siteDescription, flatStatus: .soonAvailable)
        self.snapshotTest(with: snippet)
    }

    // MARK: - Building class

    func testShouldBeBuildingClassEconom() {
        let siteDescription = Self.makeSiteDescription(buildingClass: .econom)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeBuildingClassComfort() {
        let siteDescription = Self.makeSiteDescription(buildingState: .unknown, buildingClass: .comfort)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeBuildingClassComfortPlus() {
        let siteDescription = Self.makeSiteDescription(buildingClass: .comfortPlus)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeBuildingClassBusiness() {
        let siteDescription = Self.makeSiteDescription(buildingClass: .business)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeBuildingClassElite() {
        let siteDescription = Self.makeSiteDescription(buildingClass: .elite)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeBuildingClassUnknown() {
        let siteDescription = Self.makeSiteDescription(buildingClass: .unknown)
        let snippet = Self.makeSiteSnippet(priceInfo: nil, siteDescription: siteDescription)
        self.snapshotTest(with: snippet)
    }

    // MARK: - Metro

    func testShouldBeMetroIconMoscow() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.moscow.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconSaintPetersburg() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.saintPetersburg.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconEkaterinburg() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.ekaterinburg.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconKazan() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.kazan.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconSamara() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.samara.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconNovosibirsk() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.novosibirsk.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMetroIconNizhnyNovgorod() {
        let location = Self.makeLocation(subjectFederationId: SubjectFederationID.nizhnyNovgorod.rawValue)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeNoMetro() {
        let location = Self.makeLocation(subjectFederationId: 2222)
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    func testShouldBeMultipleMetroIconsMoscow() {
        let lineColors = ["ffa8af", "ffe400", "6fc1ba"].map(Color.init(rgbHexString:)).map { YREUnwrap($0) }
        let metro = Self.makeMetro(lineColors: lineColors)
        let snippet = Self.makeSiteSnippet(metro: metro)
        self.snapshotTest(with: snippet)
    }

    // MARK: - NewBuilding name

    func testShouldBeLongNewBuildingName() {
        let snippet = Self.makeSiteSnippet(shortName: "Очень очень много букв в названии ЖК")
        self.snapshotTest(with: snippet)
    }

    // MARK: - Developer

    func testShouldBeMultipleDevelopers() {
        let snippet = Self.makeSiteSnippet(developers: [
            Self.makeDeveloper(name: "Первый застройщик"),
            Self.makeDeveloper(name: "Второй застройщик"),
        ])
        self.snapshotTest(with: snippet)
    }

    func testShouldBeLongNameDeveloper() {
        let snippet = Self.makeSiteSnippet(developers: [
            Self.makeDeveloper(name: "Очень очень много букв в названии застройщика"),
        ])
        self.snapshotTest(with: snippet)
    }

    // MARK: - Address

    func testShouldBeLongAddress() {
        let location = Self.makeLocation(streetAddress: "Москва, ул. Очень много букв в названии улицы")
        let snippet = Self.makeSiteSnippet(location: location)
        self.snapshotTest(with: snippet)
    }

    // MARK: - Price

    func testShouldBeNoPrice() {
        let snippet = Self.makeSiteSnippet(priceInfo: nil)
        self.snapshotTest(with: snippet)
    }

    // MARK: - Private

    private func snapshotTest(with snippet: YRESiteSnippet, function: String = #function) {
        let viewModel = Self.makeViewModel(siteSnippet: snippet)
        let view = Self.makeView(viewModel: viewModel)
        self.assertSnapshot(view, function: function)
    }
}

// MARK: - Factory

extension SiteSnippetTests {
    private static func makeViewModel(
        siteSnippet: YRESiteSnippet,
        isQuartographyHidden: Bool = false
    ) -> SiteSnippetViewModel {
        let infoProvider = YREAbstractOfferInfoProvider(
            offer: siteSnippet,
            inFavorites: false,
            inCallHistory: false,
            isViewed: false,
            requestingPhones: false
        )
        let siteSnippetInfoProvider = YREUnwrap(infoProvider.asSiteSnippetProvider())
        return SiteSnippetViewModelGenerator.makeViewModel(
            viewMode: .list,
            siteSnippetInfoProvider: siteSnippetInfoProvider,
            selectedImageIndex: 0,
            viewConfig: .init(isQuartographyHidden: isQuartographyHidden)
        )
    }

    private static func makeView(viewModel: SiteSnippetViewModel) -> SiteSnippetView {
        let sizeClass = YRESizeClass.screen()
        let viewLayout = SiteSnippetView.Layout.make(sizeClass: sizeClass, viewMode: .list)

        let view = SiteSnippetView()
        view.layout = viewLayout
        view.viewModel = viewModel

        let width = UIScreen.main.bounds.width
        let height = SiteSnippetView.height(
            width: width,
            layout: view.layout,
            layoutStyle: view.layoutStyle,
            viewModel: viewModel
        )

        view.frame = CGRect(origin: .zero, size: CGSize(width: width, height: height))

        return view
    }
}

extension SiteSnippetTests {
    private static func makeViewModel(
        isCallButtonEnabled: Bool = true,
        isWriteButtonHidden: Bool = true,
        quartographyViewModel: [SiteSnippetViewModel.QuartographyViewModel] = []
    ) -> SiteSnippetViewModel {
        var metro = MetroStationViewModel(name: "", icon: nil, time: nil, transport: nil, transportIcon: nil)
        if let metroColor = UIColor.yre_color(hexString: "DA80FF") {
            metro = MetroStationViewModel(
                name: "Октябрьское поле",
                icon: MetroIconGenerator.icon(city: .moscowAndMoscowOblast, colors: [metroColor]),
                time: "19 мин.",
                transport: "пешком",
                transportIcon: nil
            )
        }
        let viewModel = SiteSnippetViewModel(
            placeholderImage: nil,
            selectedImageIndex: 0,
            previewImagesFetchRoutine: { _ in [] },
            isRequestingPhones: false,
            isCallButtonEnabled: isCallButtonEnabled,
            callButtonNormalTitle: "Позвонить",
            callButtonDisabledTitle: "Позвонить",
            writeButtonTitle: "Написать",
            isWriteButtonHidden: isWriteButtonHidden,
            isFavoritesButtonHidden: false,
            isFavoritesButtonSelected: true,
            title: "Октябрьское поле",
            areaDescription: "от 9,32 млн ₽, бизнес",
            constructionStatus: "Строится, есть сданные",
            developersString: "Застройщик РГ-Девелопмент",
            metro: metro,
            address: "Москва, ул. Берзарина",
            badgesViewModels: [],
            allowToTruncateFirstBadge: false,
            bottomBadgesViewModels: [],
            quartographyViewModel: quartographyViewModel
        )
        return viewModel
    }

    private static func makeSiteSnippet(
        shortName: String? = "МИР Митино",
        location: YRELocation? = SiteSnippetTests.makeLocation(),
        metro: YREMetro? = SiteSnippetTests.makeMetro(),
        priceInfo: YRESitePriceInfo? = SiteSnippetTests.makePriceInfo(),
        siteDescription: YRESiteDescription? = SiteSnippetTests.makeSiteDescription(),
        developers: [YREOrganization]? = [SiteSnippetTests.makeDeveloper()],
        deliveryDates: [YRECommissioningDate]? = nil,
        summarySpecialProposals: [SummarySpecialProposal]? = nil,
        flatStatus: SiteFlatStatus = .onSale,
        isOutdated: Bool = false
    ) -> YRESiteSnippet {
        YRESiteSnippet(
            identifier: "",
            name: nil,
            shortName: shortName,
            large1242ImageURLs: nil,
            largeImageURLs: nil,
            middleImageURLs: nil,
            fullImageURLs: nil,
            location: location,
            metro: metro,
            filterStatistics: nil,
            resaleFilterStatistics: nil,
            priceInfo: priceInfo,
            siteDescription: siteDescription,
            developers: developers,
            salesDepartments: nil,
            deliveryDates: deliveryDates,
            summarySpecialProposals: summarySpecialProposals,
            salesClosed: .paramBoolUnknown,
            flatStatus: flatStatus,
            isOutdated: isOutdated,
            queryContext: nil,
            hasPaidCalls: .paramBoolUnknown
        )
    }

    private static func makeSiteDescription(
        buildingState: ConstantParamBuildingState = .unfinished,
        buildingClass: ConstantParamBuildingClass = .comfort,
        zhkType: ConstantParamZHKType = .unknown
    ) -> YRESiteDescription {
        YRESiteDescription(
            commonDescription: nil,
            building: .unknown,
            buildingState: buildingState,
            hasPrivateParking: .paramBoolUnknown,
            hasPrivateTerritory: .paramBoolUnknown,
            hasConcierge: .paramBoolUnknown,
            elevatorsBrand: nil,
            ceilingHeight: nil,
            maxCeilingHeight: nil,
            totalFloors: nil,
            minTotalFloors: nil,
            totalApartments: nil,
            buildingClass: buildingClass,
            agreementType: .yreConstantParamAgreementTypeUnknown,
            fz214: .paramBoolUnknown,
            installment: .paramBoolUnknown,
            mortgage: .paramBoolUnknown,
            security: .paramBoolUnknown,
            apartmentType: .unknown,
            maternityFunds: .paramBoolUnknown,
            zhkType: zhkType,
            decoration: .unknown,
            parkings: nil,
            decorationInfo: nil,
            wallTypes: nil,
            decorationDescription: nil,
            decorationImages: nil
        )
    }

    private static func makeMetro(
        name: String? = "Пятницкое шоссе",
        metroTransport: DistanceType = .foot,
        timeToMetro: Int = 19,
        lineColors: [Color] = [Color(rgbHexString: "0042a5")].map { YREUnwrap($0) }
    ) -> YREMetro {
        YREMetro(
            name: name,
            metroTransport: metroTransport,
            timeToMetro: timeToMetro,
            metroGeoID: .init(value: 0),
            coordinate: nil,
            lineColors: lineColors
        )
    }

    private static func makeDeveloper(
        name: String = "Рождествено"
    ) -> YREOrganization {
        YREOrganization(
            identifier: "",
            name: name,
            legalNames: nil,
            logoURL: nil,
            phones: nil,
            address: nil,
            websiteURL: nil,
            statistics: nil,
            hasChat: .paramBoolUnknown
        )
    }

    private static func makePriceInfo() -> YRESitePriceInfo {
        YRESitePriceInfo(
            priceRange: nil,
            priceRangePerMeter: nil,
            priceRatioToMarket: nil,
            totalOffers: nil,
            rooms: [
                Self.makePriceInfoRooms(rooms: .yreSitePriceInfoRoomsStudio),
                Self.makePriceInfoRooms(rooms: .yreSitePriceInfoRooms1),
                Self.makePriceInfoRooms(rooms: .yreSitePriceInfoRooms2),
                Self.makePriceInfoRooms(
                    rooms: .yreSitePriceInfoRooms3,
                    soldOut: .paramBoolFalse
                ),
            ],
            priceStatistics: nil
        )
    }

    private static func makePriceInfoRooms(
        rooms: SitePriceInfoRooms,
        soldOut: ConstantParamBool = .paramBoolTrue,
        status: kYRESitePriceInfoRoomsStatus = .yreSitePriceInfoRoomsStatusOnSale
    ) -> YRESitePriceInfoRooms {
        YRESitePriceInfoRooms(
            rooms: rooms,
            hasOffers: .paramBoolUnknown,
            soldOut: soldOut,
            status: status,
            priceRange: YREPriceRange(
                from: NSNumber(value: 5_540_000),
                to: NSNumber(value: 6_780_000),
                average: NSNumber(value: 6_000_000),
                currency: .RUB,
                unit: .perOffer,
                period: .wholeLife
            ),
            areaRange: YREAreaRange(
                from: YREArea(unit: .m2, value: 32.2),
                to: YREArea(unit: .m2, value: 57.2)
            ),
            priceRatioToMarket: nil
        )
    }

    private static func makeLocation(
        subjectFederationId: UInt = 1,
        streetAddress: String? = "Москва, ул. Муравская"
    ) -> YRELocation {
        YRELocation(
            regionID: nil,
            geoID: nil,
            subjectFederationId: NSNumber(value: subjectFederationId),
            subjectFederationRgid: nil,
            subjectFederationName: nil,
            address: nil,
            streetAddress: streetAddress,
            geocoderAddress: nil,
            point: nil,
            allHeatmapPoints: nil,
            expectedMetros: nil,
            ponds: nil,
            parks: nil,
            schools: nil,
            metroList: nil
        )
    }
}

extension SummarySpecialProposal {
    fileprivate static var discount: Self {
        Self.init(
            type: .discount,
            shortDescription: "Скидка до 300 000 руб.",
            isMain: .paramBoolUnknown
        )
    }

    fileprivate static var gift: Self {
        Self.init(
            type: .gift,
            shortDescription: "Кухня в подарок",
            isMain: .paramBoolUnknown
        )
    }

    fileprivate static var installment: Self {
        Self.init(
            type: .installment,
            shortDescription: "Беспроцентная рассрочка",
            isMain: .paramBoolUnknown
        )
    }

    fileprivate static var mortgage: Self {
        Self.init(
            type: .mortgage,
            shortDescription: "Ипотека 2.6%",
            isMain: .paramBoolUnknown
        )
    }

    fileprivate static var sale: Self {
        Self.init(
            type: .sale,
            shortDescription: "TRADE-IN",
            isMain: .paramBoolUnknown
        )
    }
}
