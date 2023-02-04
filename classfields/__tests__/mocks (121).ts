import { DeepPartial } from 'utility-types';

import {
    IOfferCard,
    OfferType,
    AuthorCategoryTypes,
    OfferCategory,
} from 'realty-core/types/offerCard';
import {
    AccessMode,
    AirConditioningSystem,
    BuildingClass,
    CommercialBuildingType,
    FireSafetySystem,
    GroundParkingType,
    HeatingType,
    SafetySystem,
    StreetParkingType,
    TelecommunicationSystem,
    UndergroundParkingType,
    VentilationSystem,
    Facility,
} from 'realty-core/types/commercialBuilding';

const offerCommon = {
    offerId: '1',
    offerType: OfferType.SELL,
    offerCategory: OfferCategory.COMMERCIAL,
    location: { rgid: 1 },
    author: {
        category: AuthorCategoryTypes.OWNER,
        creationDate: '',
    },
};

export const offer1: DeepPartial<IOfferCard> = {
    ...offerCommon,
    commercialInfo: {
        commercialBuilding: {
            id: '423',
            name: 'Бизнес-центр "Свиноферма"',
            buildingType: CommercialBuildingType.COMMERCIAL_BUILDING_BUSINESS_CENTER,
            buildingClass: BuildingClass.GRADE_C,
            totalArea: 3500,
            safetySystem: [SafetySystem.SS_ALARM_BUTTON],
            accessMode: [AccessMode.AM_CLOSED_ON_WEEKENDS],
            airConditioningSystem: AirConditioningSystem.ACS_FEASIBLE,
            ventilationSystem: VentilationSystem.VS_SUPPLY_AND_EXHAUST,
            fireSafetySystem: [FireSafetySystem.FSS_FIRE_SPRINKLER_SYSTEM, FireSafetySystem.FSS_FIRE_ALARM_SYSTEM],
            telecommunications: [TelecommunicationSystem.TS_DIGITAL_TELEVISION],
            parking: {
                groundParkingType: GroundParkingType.GPT_MULTILEVEL,
                groundParkingInfo: {
                    parkingPlaces: 210,
                },
                undergroundParkingType: UndergroundParkingType.UPT_NORMAL,
                undergroundParkingInfo: {
                    parkingPrice: 999,
                },
                streetParkingType: StreetParkingType.SPT_NORMAL,
            },
            facilities: [Facility.F_ATM, Facility.F_CAFE_DINING_ROOM, Facility.F_PUB, Facility.F_STORE],
            housesCount: 1,
        },
    },
};

export const offer2: DeepPartial<IOfferCard> = {
    ...offerCommon,
    commercialInfo: {
        commercialBuilding: {
            id: '2257021',
            name: 'Офисный центр «ARCUS»',
            buildingType: CommercialBuildingType.COMMERCIAL_BUILDING_BUSINESS_CENTER,
            buildingClass: BuildingClass.GRADE_A_PLUS,
            numberOfFloors: 15,
            minNumberOfFloors: 7,
            yearConstruct: 2004,
            yearReconstruct: 2020,
            totalArea: 46472.9,
            accessMode: [AccessMode.AM_FULL_24_7],
            airConditioningSystem: AirConditioningSystem.ACS_CENTRAL,
            ventilationSystem: VentilationSystem.VS_CENTRAL,
            fireSafetySystem: [FireSafetySystem.FSS_POWDER_FIRE_EXTINGUISHING_SYSTEM],
            heatingType: HeatingType.HEATING_TYPE_AUTONOMOUS,
            telecommunications: [
                TelecommunicationSystem.TS_DIGITAL_TELEVISION,
                TelecommunicationSystem.TS_IP_TELEPHONY,
            ],
            elevatorsNumber: 7,
            elevatorsBrand: 'Otis',
            parking: {
                groundParkingType: GroundParkingType.GPT_COVERED,
                groundParkingInfo: {
                    parkingPlaces: 280,
                    parkingPrice: 5000,
                },
                undergroundParkingType: UndergroundParkingType.UPT_MULTILEVEL,
                streetParkingType: StreetParkingType.SPT_PAID,
            },
            facilities: Object.values(Facility),
            housesCount: 5,
        },
    },
};

export const offer3: DeepPartial<IOfferCard> = {
    ...offerCommon,
    commercialInfo: {
        commercialBuilding: {
            id: '2257321',
            name: 'Дворец важного человека',
            buildingType: CommercialBuildingType.COMMERCIAL_BUILDING_RESIDENTIAL,
            buildingClass: BuildingClass.GRADE_B_MINUS,
            numberOfFloors: 10,
            minNumberOfFloors: 10,
            yearConstruct: 2015,
            yearReconstruct: 2019,
            totalArea: 1525.45,
            accessMode: [AccessMode.AM_FREE_ACCESS],
            airConditioningSystem: AirConditioningSystem.ACS_PARTIAL,
            ventilationSystem: VentilationSystem.VS_NATURAL,
            fireSafetySystem: [FireSafetySystem.FSS_HYDRANT_FIRE_EXTINGUISHING_SYSTEM],
            heatingType: HeatingType.HEATING_TYPE_CENTRAL,
            telecommunications: [
                TelecommunicationSystem.TS_HIGH_SPEED_INTERNET,
                TelecommunicationSystem.TS_IP_TELEPHONY,
            ],
            elevatorsNumber: 2,
            elevatorsBrand: 'ThyssenKrupp',
            parking: {
                groundParkingType: GroundParkingType.GPT_NORMAL,
                groundParkingInfo: {
                    parkingPlaces: 50,
                },
                streetParkingType: StreetParkingType.SPT_NORMAL,
            },
            facilities: Object.values(Facility),
            housesCount: 2,
        },
    },
};

export const offer4: DeepPartial<IOfferCard> = {
    ...offerCommon,
    commercialInfo: {
        commercialBuilding: {
            id: '423',
            name: 'Бизнес-центр "Свиноферма"',
            buildingType: CommercialBuildingType.COMMERCIAL_BUILDING_BUSINESS_CENTER,
            buildingClass: BuildingClass.GRADE_C,
            totalArea: 3500,
            safetySystem: [SafetySystem.SS_ALARM_BUTTON],
            accessMode: [AccessMode.AM_CLOSED_ON_WEEKENDS],
            airConditioningSystem: AirConditioningSystem.ACS_FEASIBLE,
            ventilationSystem: VentilationSystem.VS_SUPPLY_AND_EXHAUST,
            fireSafetySystem: [FireSafetySystem.FSS_FIRE_SPRINKLER_SYSTEM, FireSafetySystem.FSS_FIRE_ALARM_SYSTEM],
            telecommunications: [TelecommunicationSystem.TS_DIGITAL_TELEVISION],
            parking: {
                groundParkingType: GroundParkingType.GPT_MULTILEVEL,
                groundParkingInfo: {
                    parkingPlaces: 210,
                },
            },
            facilities: [Facility.F_ATM, Facility.F_CAFE_DINING_ROOM, Facility.F_STORE],
            housesCount: 1,
        },
    },
};
