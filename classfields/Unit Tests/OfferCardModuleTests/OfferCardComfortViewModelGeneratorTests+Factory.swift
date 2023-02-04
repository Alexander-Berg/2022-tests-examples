//
//  OfferCardComfortViewModelGeneratorTests+Factory.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 18.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable file_length

import Foundation
import YREModel
import YREModelObjc

// MARK: - Building

extension OfferCardComfortViewModelGeneratorTests {
    static func makeBuildingPositive() -> YREBuilding {
        Self.makeBuilding(
            hasParking: .paramBoolTrue,
            hasGuestParking: .paramBoolTrue,
            hasLift: .paramBoolTrue,
            hasRubbishChute: .paramBoolTrue,
            hasPassBy: .paramBoolTrue,
            hasAlarm: .paramBoolTrue,
            forCityRenovation: .paramBoolTrue,
            guarded: .paramBoolTrue,
            security: .paramBoolTrue,
            hasAccessControlSystem: .paramBoolTrue,
            twentyFourSeven: .paramBoolTrue,
            hasEatingFacilities: .paramBoolTrue,
            hasCCTV: .paramBoolTrue,
            hasDeveloperChat: .paramBoolTrue
        )
    }

    static func makeBuildingNegative() -> YREBuilding {
        Self.makeBuilding(
            hasParking: .paramBoolFalse,
            hasGuestParking: .paramBoolFalse,
            hasLift: .paramBoolFalse,
            hasRubbishChute: .paramBoolFalse,
            hasPassBy: .paramBoolFalse,
            hasAlarm: .paramBoolFalse,
            forCityRenovation: .paramBoolFalse,
            guarded: .paramBoolFalse,
            security: .paramBoolFalse,
            hasAccessControlSystem: .paramBoolFalse,
            twentyFourSeven: .paramBoolFalse,
            hasEatingFacilities: .paramBoolFalse,
            hasCCTV: .paramBoolFalse,
            hasDeveloperChat: .paramBoolFalse
        )
    }

    static func makeBuildingUnknown() -> YREBuilding {
        Self.makeBuilding()
    }

    private static func makeBuilding(
        hasParking: ConstantParamBool = .paramBoolUnknown,
        hasGuestParking: ConstantParamBool = .paramBoolUnknown,
        hasLift: ConstantParamBool = .paramBoolUnknown,
        hasRubbishChute: ConstantParamBool = .paramBoolUnknown,
        hasPassBy: ConstantParamBool = .paramBoolUnknown,
        hasAlarm: ConstantParamBool = .paramBoolUnknown,
        forCityRenovation: ConstantParamBool = .paramBoolUnknown,
        guarded: ConstantParamBool = .paramBoolUnknown,
        security: ConstantParamBool = .paramBoolUnknown,
        hasAccessControlSystem: ConstantParamBool = .paramBoolUnknown,
        twentyFourSeven: ConstantParamBool = .paramBoolUnknown,
        hasEatingFacilities: ConstantParamBool = .paramBoolUnknown,
        hasCCTV: ConstantParamBool = .paramBoolUnknown,
        hasDeveloperChat: ConstantParamBool = .paramBoolUnknown
    ) -> YREBuilding {
        YREBuilding(
            builtYear: nil,
            buildingSeries: nil,
            builtQuarter: nil, 
            buildingState: .unknown,
            siteId: nil,
            building: .unknown,
            buildingEpoch: .unknown,
            parkingType: .unknown,
            heatingType: .unknown,
            officeClass: .classA,
            hasParking: hasParking,
            hasGuestParking: hasGuestParking,
            hasLift: hasLift,
            hasRubbishChute: hasRubbishChute,
            hasPassBy: hasPassBy,
            hasAlarm: hasAlarm,
            forCityRenovation: forCityRenovation,
            guarded: guarded,
            security: security,
            hasAccessControlSystem: hasAccessControlSystem,
            twentyFourSeven: twentyFourSeven,
            hasEatingFacilities: hasEatingFacilities,
            hasCCTV: hasCCTV,
            siteDisplayName: nil,
            houseReadableName: nil,
            flatsCount: nil,
            porchCount: nil,
            reconstructionYear: nil,
            developerIds: nil,
            hasDeveloperChat: hasDeveloperChat
        )
    }
}

// MARK: - Apartment

extension OfferCardComfortViewModelGeneratorTests {
    static func makeApartmentPositive() -> YREApartment {
        Self.makeApartment(
            phone: .paramBoolTrue,
            internet: .paramBoolTrue,
            selfSelectionTelecom: .paramBoolTrue,
            roomFurniture: .paramBoolTrue,
            kitchenFurniture: .paramBoolTrue,
            buildInTech: .paramBoolTrue,
            aircondiotion: .paramBoolTrue,
            ventilation: .paramBoolTrue,
            refrigerator: .paramBoolTrue,
            noFurniture: .paramBoolTrue,
            flatAlarm: .paramBoolTrue,
            fireAlarm: .paramBoolTrue,
            dishwasher: .paramBoolTrue,
            washingMachine: .paramBoolTrue,
            television: .paramBoolTrue,
            addingPhoneOnRequest: .paramBoolTrue,
            responsibleStorage: .paramBoolTrue
        )
    }

    static func makeApartmentNegative() -> YREApartment {
        Self.makeApartment(
            phone: .paramBoolFalse,
            internet: .paramBoolFalse,
            selfSelectionTelecom: .paramBoolFalse,
            roomFurniture: .paramBoolFalse,
            kitchenFurniture: .paramBoolFalse,
            buildInTech: .paramBoolFalse,
            aircondiotion: .paramBoolFalse,
            ventilation: .paramBoolFalse,
            refrigerator: .paramBoolFalse,
            noFurniture: .paramBoolFalse,
            flatAlarm: .paramBoolFalse,
            fireAlarm: .paramBoolFalse,
            dishwasher: .paramBoolFalse,
            washingMachine: .paramBoolFalse,
            television: .paramBoolFalse,
            addingPhoneOnRequest: .paramBoolFalse,
            responsibleStorage: .paramBoolFalse
        )
    }

    static func makeApartmentUnknown() -> YREApartment {
        Self.makeApartment()
    }

    private static func makeApartment(
        phone: ConstantParamBool = .paramBoolUnknown,
        internet: ConstantParamBool = .paramBoolUnknown,
        selfSelectionTelecom: ConstantParamBool = .paramBoolUnknown,
        roomFurniture: ConstantParamBool = .paramBoolUnknown,
        kitchenFurniture: ConstantParamBool = .paramBoolUnknown,
        buildInTech: ConstantParamBool = .paramBoolUnknown,
        aircondiotion: ConstantParamBool = .paramBoolUnknown,
        ventilation: ConstantParamBool = .paramBoolUnknown,
        refrigerator: ConstantParamBool = .paramBoolUnknown,
        noFurniture: ConstantParamBool = .paramBoolUnknown,
        flatAlarm: ConstantParamBool = .paramBoolUnknown,
        fireAlarm: ConstantParamBool = .paramBoolUnknown,
        dishwasher: ConstantParamBool = .paramBoolUnknown,
        washingMachine: ConstantParamBool = .paramBoolUnknown,
        television: ConstantParamBool = .paramBoolUnknown,
        addingPhoneOnRequest: ConstantParamBool = .paramBoolUnknown,
        responsibleStorage: ConstantParamBool = .paramBoolUnknown
    ) -> YREApartment {
        YREApartment(
            renovation: .unknown,
            quality: .unknown, 
            decoration: .unknown,
            phone: phone,
            internet: internet,
            selfSelectionTelecom: selfSelectionTelecom,
            roomFurniture: roomFurniture,
            kitchenFurniture: kitchenFurniture,
            buildInTech: buildInTech,
            aircondiotion: aircondiotion,
            ventilation: ventilation,
            refrigerator: refrigerator,
            noFurniture: noFurniture,
            flatAlarm: flatAlarm,
            fireAlarm: fireAlarm,
            dishwasher: dishwasher,
            washingMachine: washingMachine,
            television: television,
            addingPhoneOnRequest: addingPhoneOnRequest,
            responsibleStorage: responsibleStorage,
            flatPlanImage: nil
        )
    }
}

// MARK: - House

extension OfferCardComfortViewModelGeneratorTests {
    static func makeHousePositive() -> YREHouse {
        Self.makeHouse(
            studio: .paramBoolTrue,
            apartments: .paramBoolTrue,
            pmg: .paramBoolTrue,
            housePart: .paramBoolTrue,
            kitchen: .paramBoolTrue,
            pool: .paramBoolTrue,
            billiard: .paramBoolTrue,
            sauna: .paramBoolTrue
        )
    }

    static func makeHouseNegative() -> YREHouse {
        Self.makeHouse(
            studio: .paramBoolFalse,
            apartments: .paramBoolFalse,
            pmg: .paramBoolFalse,
            housePart: .paramBoolFalse,
            kitchen: .paramBoolFalse,
            pool: .paramBoolFalse,
            billiard: .paramBoolFalse,
            sauna: .paramBoolFalse
        )
    }

    static func makeHouseUnknown() -> YREHouse {
        Self.makeHouse()
    }

    private static func makeHouse(
        studio: ConstantParamBool = .paramBoolUnknown,
        apartments: ConstantParamBool = .paramBoolUnknown,
        pmg: ConstantParamBool = .paramBoolUnknown,
        housePart: ConstantParamBool = .paramBoolUnknown,
        kitchen: ConstantParamBool = .paramBoolUnknown,
        pool: ConstantParamBool = .paramBoolUnknown,
        billiard: ConstantParamBool = .paramBoolUnknown,
        sauna: ConstantParamBool = .paramBoolUnknown
    ) -> YREHouse {
        YREHouse(
            bathroomUnit: .unknown,
            windowView: .unknown,
            windowType: .unknown,
            balconyType: .unknown,
            entranceType: .unknown,
            studio: studio,
            apartments: apartments,
            pmg: pmg,
            housePart: housePart,
            houseType: .unknown,
            kitchen: kitchen,
            pool: pool,
            billiard: billiard,
            sauna: sauna,
            toilet: .unknown,
            shower: .unknown,
            electricCapacity: nil,
            phoneLinesCount: nil
        )
    }
}

// MARK: - Garage

extension OfferCardComfortViewModelGeneratorTests {
    static func makeGaragePositive() -> Garage {
        Self.makeGarage(
            hasAutomaticGates: .paramBoolTrue,
            hasInspectionPit: .paramBoolTrue,
            hasCarWash: .paramBoolTrue,
            hasAutoRepair: .paramBoolTrue,
            hasCellar: .paramBoolTrue
        )
    }

    static func makeGarageNegative() -> Garage {
        Self.makeGarage(
            hasAutomaticGates: .paramBoolFalse,
            hasInspectionPit: .paramBoolFalse,
            hasCarWash: .paramBoolFalse,
            hasAutoRepair: .paramBoolFalse,
            hasCellar: .paramBoolFalse
        )
    }

    static func makeGarageUnknown() -> Garage {
        Self.makeGarage()
    }

    private static func makeGarage(
        hasAutomaticGates: ConstantParamBool = .paramBoolUnknown,
        hasInspectionPit: ConstantParamBool = .paramBoolUnknown,
        hasCarWash: ConstantParamBool = .paramBoolUnknown,
        hasAutoRepair: ConstantParamBool = .paramBoolUnknown,
        hasCellar: ConstantParamBool = .paramBoolUnknown
    ) -> Garage {
        Garage(
            cooperativeName: nil,
            type: .unknown,
            ownershipType: .unknown,
            hasAutomaticGates: hasAutomaticGates,
            hasInspectionPit: hasInspectionPit,
            hasCarWash: hasCarWash,
            hasAutoRepair: hasAutoRepair,
            hasCellar: hasCellar
        )
    }
}

// MARK: - SiteInfo

extension OfferCardComfortViewModelGeneratorTests {
    static func makeOfferSiteInfoPositive() -> OfferSiteInfo {
        Self.makeOfferSiteInfo(security: .paramBoolTrue)
    }

    static func makeOfferSiteInfoNegative() -> OfferSiteInfo {
        Self.makeOfferSiteInfo(security: .paramBoolFalse)
    }

    static func makeOfferSiteInfoUnknown() -> OfferSiteInfo {
        Self.makeOfferSiteInfo()
    }

    private static func makeOfferSiteInfo(
        security: ConstantParamBool = .paramBoolUnknown
    ) -> OfferSiteInfo {
        OfferSiteInfo(
            identifier: NSNumber(value: 0),
            name: "",
            fullName: "",
            developers: nil,
            specialProposals: nil,
            buildingClass: .unknown,
            buildingState: .unknown,
            security: security,
            apartmentType: .unknown,
            deliveryDates: nil,
            priceInfo: nil,
            totalFloors: nil,
            minTotalFloors: nil,
            parkings: []
        )
    }
}

// MARK: - CommercialDescription

extension OfferCardComfortViewModelGeneratorTests {
    static func makeCommercialDescriptionPositive(
        buildingType: ConstantParamCommercialBuildingType = .unknown
    ) -> YRECommercialDescription {
        Self.makeCommercialDescription(
            buildingType: buildingType,
            hasRailwayNearby: .paramBoolTrue,
            hasTruckEntrance: .paramBoolTrue,
            hasRamp: .paramBoolTrue,
            hasOfficeWarehouse: .paramBoolTrue,
            hasOpenArea: .paramBoolTrue,
            hasThreePLService: .paramBoolTrue,
            hasFreightElevator: .paramBoolTrue 
        )
    }

    static func makeCommercialDescriptionNegative(
        buildingType: ConstantParamCommercialBuildingType = .unknown
    ) -> YRECommercialDescription {
        Self.makeCommercialDescription(
            buildingType: buildingType,
            hasRailwayNearby: .paramBoolFalse,
            hasTruckEntrance: .paramBoolFalse,
            hasRamp: .paramBoolFalse,
            hasOfficeWarehouse: .paramBoolFalse,
            hasOpenArea: .paramBoolFalse,
            hasThreePLService: .paramBoolFalse,
            hasFreightElevator: .paramBoolFalse 
        )
    }

    static func makeCommercialDescriptionUnknown(
        buildingType: ConstantParamCommercialBuildingType = .unknown
    ) -> YRECommercialDescription {
        Self.makeCommercialDescription(buildingType: buildingType)
    }

    private static func makeCommercialDescription(
        buildingType: ConstantParamCommercialBuildingType = .unknown,
        hasRailwayNearby: ConstantParamBool = .paramBoolUnknown,
        hasTruckEntrance: ConstantParamBool = .paramBoolUnknown,
        hasRamp: ConstantParamBool = .paramBoolUnknown,
        hasOfficeWarehouse: ConstantParamBool = .paramBoolUnknown,
        hasOpenArea: ConstantParamBool = .paramBoolUnknown,
        hasThreePLService: ConstantParamBool = .paramBoolUnknown,
        hasFreightElevator: ConstantParamBool = .paramBoolUnknown 
    ) -> YRECommercialDescription {
        YRECommercialDescription(
            buildingType: buildingType,
            types: nil,
            purposes: nil,
            warehousePurposes: nil,
            hasRailwayNearby: hasRailwayNearby,
            hasTruckEntrance: hasTruckEntrance,
            hasRamp: hasRamp,
            hasOfficeWarehouse: hasOfficeWarehouse,
            hasOpenArea: hasOpenArea,
            hasThreePLService: hasThreePLService,
            hasFreightElevator: hasFreightElevator
        )
    }
}

// MARK: - Offer

extension OfferCardComfortViewModelGeneratorTests {
    static func makeOfferPositive(
        type: kYREOfferType,
        category: kYREOfferCategory,
        price: YREPrice? = nil,
        building: YREBuilding? = nil,
        apartment: YREApartment? = nil,
        house: YREHouse? = nil,
        garage: Garage? = nil,
        siteInfo: OfferSiteInfo? = nil,
        commercialDescription: YRECommercialDescription? = nil
    ) -> YREOffer {
        Self.makeOffer(
            type: type,
            category: category,
            price: price,
            electricity: .paramBoolTrue,
            heating: .paramBoolTrue,
            water: .paramBoolTrue,
            sewerage: .paramBoolTrue,
            gas: .paramBoolTrue,
            withPets: .paramBoolTrue,
            withKids: .paramBoolTrue,
            building: building,
            apartment: apartment,
            house: house,
            garage: garage,
            siteInfo: siteInfo,
            commercialDescription: commercialDescription
        )
    }

    static func makeOfferNegative(
        type: kYREOfferType,
        category: kYREOfferCategory,
        price: YREPrice? = nil,
        building: YREBuilding? = nil,
        apartment: YREApartment? = nil,
        house: YREHouse? = nil,
        garage: Garage? = nil,
        siteInfo: OfferSiteInfo? = nil,
        commercialDescription: YRECommercialDescription? = nil
    ) -> YREOffer {
        Self.makeOffer(
            type: type,
            category: category,
            price: price,
            electricity: .paramBoolFalse,
            heating: .paramBoolFalse,
            water: .paramBoolFalse,
            sewerage: .paramBoolFalse,
            gas: .paramBoolFalse,
            withPets: .paramBoolFalse,
            withKids: .paramBoolFalse,
            building: building,
            apartment: apartment,
            house: house,
            garage: garage,
            siteInfo: siteInfo,
            commercialDescription: commercialDescription
        )
    }

    static func makeOfferUnknown(
        type: kYREOfferType,
        category: kYREOfferCategory,
        price: YREPrice? = nil,
        building: YREBuilding? = nil,
        apartment: YREApartment? = nil,
        house: YREHouse? = nil,
        garage: Garage? = nil,
        siteInfo: OfferSiteInfo? = nil,
        commercialDescription: YRECommercialDescription? = nil
    ) -> YREOffer {
        Self.makeOffer(
            type: type,
            category: category,
            price: price,
            building: building,
            apartment: apartment,
            house: house,
            garage: garage,
            siteInfo: siteInfo,
            commercialDescription: commercialDescription
        )
    }

    // swiftlint:disable:next function_body_length
    private static func makeOffer(
        type: kYREOfferType,
        category: kYREOfferCategory,
        price: YREPrice? = nil,
        electricity: ConstantParamBool = .paramBoolUnknown,
        heating: ConstantParamBool = .paramBoolUnknown,
        water: ConstantParamBool = .paramBoolUnknown,
        sewerage: ConstantParamBool = .paramBoolUnknown,
        gas: ConstantParamBool = .paramBoolUnknown,
        withPets: ConstantParamBool = .paramBoolUnknown,
        withKids: ConstantParamBool = .paramBoolUnknown,
        building: YREBuilding? = nil,
        apartment: YREApartment? = nil,
        house: YREHouse? = nil,
        garage: Garage? = nil,
        siteInfo: OfferSiteInfo? = nil,
        commercialDescription: YRECommercialDescription? = nil
    ) -> YREOffer {
        YREOffer(
            identifier: "test",
            type: type,
            category: category,
            partnerId: nil,
            internal: .paramBoolFalse,
            creationDate: Date(),
            newFlatSale: .paramBoolFalse,
            primarySale: .paramBoolUnknown,
            flatType: .unknown,
            urlString: nil,
            update: nil,
            roomsTotal: 1,
            roomsOffered: 1,
            floorsTotal: 12,
            floorsOffered: [2],
            author: nil,
            isFullTrustedOwner: .paramBoolFalse,
            trust: .yreConstantParamOfferTrustUnknown,
            viewsCount: 0,
            floorCovering: .unknown,
            area: YREArea(unit: .m2, value: 100),
            livingSpace: YREArea(unit: .m2, value: 100),
            kitchenSpace: YREArea(unit: .m2, value: 100),
            roomSpace: [YREArea(unit: .m2, value: 100)],
            large1242ImageURLs: nil,
            appLargeImageURLs: nil,
            minicardImageURLs: nil,
            middleImageURLs: nil,
            largeImageURLs: nil,
            fullImageURLs: nil,
            previewImages: nil,
            offerPlanImages: nil,
            floorPlanImages: nil,
            youtubeVideoReviewURL: nil,
            suspicious: .paramBoolFalse,
            active: .paramBoolFalse,
            hasAlarm: .paramBoolFalse,
            housePart: .paramBoolFalse,
            price: price,
            offerPriceInfo: nil,
            location: nil,
            metro: nil,
            house: house,
            building: building,
            garage: garage,
            lotArea: YREArea(unit: .are, value: 100),
            lotType: .garden,
            offerDescription: nil,
            commissioningDate: nil,
            ceilingHeight: nil,
            haggle: .paramBoolFalse,
            mortgage: .paramBoolFalse,
            rentPledge: .paramBoolFalse,
            electricityIncluded: .paramBoolFalse,
            cleaningIncluded: .paramBoolFalse,
            withKids: withKids,
            withPets: withPets,
            supportsOnlineView: .paramBoolFalse,
            zhkDisplayName: nil,
            apartment: apartment,
            dealStatus: .primarySale,
            agentFee: nil,
            commission: nil,
            prepayment: nil,
            securityPayment: nil,
            taxationForm: .nds,
            isFreeReportAvailable: .paramBoolFalse,
            isPurchasingReportAvailable: .paramBoolFalse,
            paidExcerptsInfo: nil,
            generatedFromSnippet: false,
            heating: heating,
            water: water,
            sewerage: sewerage,
            electricity: electricity,
            gas: gas,
            utilitiesIncluded: .paramBoolFalse,
            salesDepartments: nil,
            isOutdated: false,
            uid: nil,
            share: nil,
            enrichedFields: nil,
            history: nil,
            commercialDescription: commercialDescription,
            villageInfo: nil,
            siteInfo: siteInfo,
            vas: nil,
            queryContext: nil,
            chargeForCallsType: .unknown,
            yandexRent: .paramBoolFalse,
            userNote: nil,
            virtualTours: nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}
