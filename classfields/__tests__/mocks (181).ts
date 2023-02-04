const flatStatus = {
    NOT_ON_SALE: 'NOT_ON_SALE',
};

import { FlatStatus } from 'realty-core/types/common';

/* eslint-disable max-len */
export const geo = {
    id: 1,
    type: 'SUBJECT_FEDERATION',
    rgid: 741964,
    populatedRgid: 741964,
    name: 'Москва и МО',
    locative: 'в Москве и МО',
    parents: [
        {
            id: 225,
            rgid: '143',
            name: 'Россия',
            type: 'COUNTRY',
            genitive: 'России',
        },
        {
            id: 0,
            rgid: '0',
            name: 'Весь мир',
            type: 'UNKNOWN',
        },
    ],
    heatmaps: ['infrastructure', 'price-rent', 'price-sell', 'profitability', 'transport', 'carsharing'],
    refinements: ['metro', 'directions', 'sub-localities', 'stations', 'map-area'],
    searchFilters: {
        'ROOMS:RENT': {
            categoryType: 'ROOMS',
            offerType: 'RENT',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                ceilingHeight: {
                    name: 'ceilingHeight',
                },
                livingSpace: {
                    name: 'livingSpace',
                },
                floor: {
                    name: 'floor',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                minFloors: {
                    name: 'minFloors',
                },
                maxFee: {
                    name: 'maxFee',
                },
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                parking: {
                    name: 'parking',
                    values: ['CLOSED', 'OPEN', 'UNDERGROUND'],
                },
                renovation: {
                    name: 'renovation',
                    values: ['EURO', 'COSMETIC_DONE', 'NON_GRANDMOTHER'],
                },
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['2', '3', 'PLUS_4', '5'],
                },
                rentTime: {
                    name: 'rentTime',
                    values: ['LARGE', 'SHORT'],
                },
                buildingType: {
                    name: 'buildingType',
                    values: ['BRICK', 'PANEL'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasTelevision: {
                    name: 'hasTelevision',
                },
                floorExceptLast: {
                    name: 'floorExceptLast',
                },
                hasPhoto: {
                    name: 'hasPhoto',
                },
                hasAircondition: {
                    name: 'hasAircondition',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                hasPhone: {
                    name: 'hasPhone',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                floorLast: {
                    name: 'floorLast',
                },
                hasRefrigerator: {
                    name: 'hasRefrigerator',
                },
                hasPark: {
                    name: 'hasPark',
                },
                withChildren: {
                    name: 'withChildren',
                },
            },
        },
        'COMMERCIAL:RENT': {
            categoryType: 'COMMERCIAL',
            offerType: 'RENT',
            rangeFilters: {
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['NO'],
                    tipsValue: 'NO',
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['1', '3', 'PLUS_4', 'PLUS_7'],
                },
                rentTime: {
                    name: 'rentTime',
                    values: ['LARGE'],
                    tipsValue: 'LARGE',
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
            },
        },
        'APARTMENT:RENT': {
            categoryType: 'APARTMENT',
            offerType: 'RENT',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                kitchenSpace: {
                    name: 'kitchenSpace',
                },
                ceilingHeight: {
                    name: 'ceilingHeight',
                },
                livingSpace: {
                    name: 'livingSpace',
                },
                floor: {
                    name: 'floor',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                minFloors: {
                    name: 'minFloors',
                },
                maxFee: {
                    name: 'maxFee',
                },
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                parking: {
                    name: 'parking',
                    values: ['CLOSED', 'OPEN', 'UNDERGROUND'],
                },
                renovation: {
                    name: 'renovation',
                    values: ['EURO', 'COSMETIC_DONE', 'DESIGNER_RENOVATION', 'NEEDS_RENOVATION', 'NON_GRANDMOTHER'],
                },
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['1', '2', '3', 'PLUS_4', 'STUDIO', '4', '5', '6', 'PLUS_7'],
                },
                rentTime: {
                    name: 'rentTime',
                    values: ['LARGE', 'SHORT'],
                },
                buildingType: {
                    name: 'buildingType',
                    values: ['BRICK', 'MONOLIT', 'PANEL', 'MONOLIT_BRICK', 'BLOCK'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasTelevision: {
                    name: 'hasTelevision',
                },
                floorExceptLast: {
                    name: 'floorExceptLast',
                },
                hasWashingMachine: {
                    name: 'hasWashingMachine',
                },
                hasAircondition: {
                    name: 'hasAircondition',
                },
                hasDishwasher: {
                    name: 'hasDishwasher',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                floorLast: {
                    name: 'floorLast',
                },
                hasRefrigerator: {
                    name: 'hasRefrigerator',
                },
                hasPark: {
                    name: 'hasPark',
                },
                floorExceptFirst: {
                    name: 'floorExceptFirst',
                },
                withPets: {
                    name: 'withPets',
                },
                hasPhoto: {
                    name: 'hasPhoto',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPhone: {
                    name: 'hasPhone',
                },
                yandexRent: {
                    name: 'yandexRent',
                },
                expectDemolition: {
                    name: 'expectDemolition',
                },
                withChildren: {
                    name: 'withChildren',
                },
                apartments: {
                    name: 'apartments',
                },
            },
        },
        'HOUSE:RENT': {
            categoryType: 'HOUSE',
            offerType: 'RENT',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                lotArea: {
                    name: 'lotArea',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['PLUS_4', '4', '5', '6', 'PLUS_7'],
                },
                housePart: {
                    name: 'housePart',
                    values: ['NO'],
                    tipsValue: 'NO',
                },
                rentTime: {
                    name: 'rentTime',
                    values: ['LARGE', 'SHORT'],
                },
                lotType: {
                    name: 'lotType',
                    values: ['IGS', 'GARDEN'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                hasPark: {
                    name: 'hasPark',
                },
            },
        },
        'GARAGE:RENT': {
            categoryType: 'GARAGE',
            offerType: 'RENT',
            rangeFilters: {
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH'],
                },
                garageType: {
                    name: 'garageType',
                    values: ['GARAGE'],
                    tipsValue: 'GARAGE',
                },
                rentTime: {
                    name: 'rentTime',
                    values: ['LARGE'],
                    tipsValue: 'LARGE',
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                hasElectricityIncluded: {
                    name: 'hasElectricityIncluded',
                },
                hasUtilitiesIncluded: {
                    name: 'hasUtilitiesIncluded',
                },
            },
        },
        'APARTMENT:SELL:NEWBUILDING': {
            categoryType: 'APARTMENT',
            offerType: 'SELL',
            newFlat: 'YES',
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
            },
            flagFilters: {
                hasSiteMortgage: {
                    name: 'hasSiteMortgage',
                },
                hasSpecialProposal: {
                    name: 'hasSpecialProposal',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                hasInstallment: {
                    name: 'hasInstallment',
                },
                onlySamolet: {
                    name: 'onlySamolet',
                },
                hasPark: {
                    name: 'hasPark',
                },
            },
        },
        'APARTMENT:SELL': {
            categoryType: 'APARTMENT',
            offerType: 'SELL',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                kitchenSpace: {
                    name: 'kitchenSpace',
                },
                ceilingHeight: {
                    name: 'ceilingHeight',
                },
                livingSpace: {
                    name: 'livingSpace',
                },
                floor: {
                    name: 'floor',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                minFloors: {
                    name: 'minFloors',
                },
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                parking: {
                    name: 'parking',
                    values: ['CLOSED', 'OPEN', 'UNDERGROUND'],
                },
                renovation: {
                    name: 'renovation',
                    values: ['EURO', 'COSMETIC_DONE', 'DESIGNER_RENOVATION', 'NEEDS_RENOVATION', 'NON_GRANDMOTHER'],
                },
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['1', '2', '3', 'PLUS_4', 'STUDIO', '4', '5', '6', 'PLUS_7'],
                },
                bathroomUnit: {
                    name: 'bathroomUnit',
                    values: ['MATCHED', 'SEPARATED', 'TWO_AND_MORE'],
                },
                newFlat: {
                    name: 'newFlat',
                    values: ['YES', 'NO'],
                },
                balcony: {
                    name: 'balcony',
                    values: ['BALCONY', 'LOGGIA', 'ANY'],
                },
                buildingType: {
                    name: 'buildingType',
                    values: ['BRICK', 'MONOLIT', 'PANEL', 'MONOLIT_BRICK', 'BLOCK'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                floorExceptFirst: {
                    name: 'floorExceptFirst',
                },
                floorExceptLast: {
                    name: 'floorExceptLast',
                },
                hasPhoto: {
                    name: 'hasPhoto',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                floorLast: {
                    name: 'floorLast',
                },
                hasPark: {
                    name: 'hasPark',
                },
                expectDemolition: {
                    name: 'expectDemolition',
                },
                apartments: {
                    name: 'apartments',
                },
            },
        },
        'HOUSE:SELL': {
            categoryType: 'HOUSE',
            offerType: 'SELL',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                lotArea: {
                    name: 'lotArea',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['1', '2', '3', 'PLUS_4', '4', '5', '6', 'PLUS_7'],
                },
                housePart: {
                    name: 'housePart',
                    values: ['YES', 'NO'],
                },
                lotType: {
                    name: 'lotType',
                    values: ['IGS', 'GARDEN'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                hasHeatingSupply: {
                    name: 'hasHeatingSupply',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasElectricitySupply: {
                    name: 'hasElectricitySupply',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                hasWaterSupply: {
                    name: 'hasWaterSupply',
                },
                hasPark: {
                    name: 'hasPark',
                },
                hasSewerageSupply: {
                    name: 'hasSewerageSupply',
                },
                hasGasSupply: {
                    name: 'hasGasSupply',
                },
            },
        },
        'GARAGE:SELL': {
            categoryType: 'GARAGE',
            offerType: 'SELL',
            rangeFilters: {
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                garageType: {
                    name: 'garageType',
                    values: ['BOX', 'PARKING_PLACE', 'GARAGE'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                hasHeatingSupply: {
                    name: 'hasHeatingSupply',
                },
                hasElectricitySupply: {
                    name: 'hasElectricitySupply',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                hasWaterSupply: {
                    name: 'hasWaterSupply',
                },
                hasSecurity: {
                    name: 'hasSecurity',
                },
            },
        },
        'COMMERCIAL:SELL': {
            categoryType: 'COMMERCIAL',
            offerType: 'SELL',
            rangeFilters: {
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['1', '2', '3', 'PLUS_4', '4', 'PLUS_7'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
            },
        },
        'ROOMS:SELL': {
            categoryType: 'ROOMS',
            offerType: 'SELL',
            rangeFilters: {
                area: {
                    name: 'area',
                },
                kitchenSpace: {
                    name: 'kitchenSpace',
                },
                ceilingHeight: {
                    name: 'ceilingHeight',
                },
                livingSpace: {
                    name: 'livingSpace',
                },
                floor: {
                    name: 'floor',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                minFloors: {
                    name: 'minFloors',
                },
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                parking: {
                    name: 'parking',
                    values: ['OPEN', 'UNDERGROUND'],
                },
                renovation: {
                    name: 'renovation',
                    values: ['COSMETIC_DONE', 'DESIGNER_RENOVATION', 'NEEDS_RENOVATION', 'NON_GRANDMOTHER'],
                },
                hasFurniture: {
                    name: 'hasFurniture',
                    values: ['YES', 'NO'],
                },
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                roomsTotal: {
                    name: 'roomsTotal',
                    values: ['2', '3', 'PLUS_4', '4', '5', 'PLUS_7'],
                },
                bathroomUnit: {
                    name: 'bathroomUnit',
                    values: ['MATCHED', 'SEPARATED', 'TWO_AND_MORE'],
                },
                buildingType: {
                    name: 'buildingType',
                    values: ['BRICK', 'PANEL', 'BLOCK'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                floorExceptFirst: {
                    name: 'floorExceptFirst',
                },
                floorExceptLast: {
                    name: 'floorExceptLast',
                },
                hasPhoto: {
                    name: 'hasPhoto',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                floorLast: {
                    name: 'floorLast',
                },
                hasPark: {
                    name: 'hasPark',
                },
                apartments: {
                    name: 'apartments',
                },
            },
        },
        'LOT:SELL': {
            categoryType: 'LOT',
            offerType: 'SELL',
            rangeFilters: {
                lotArea: {
                    name: 'lotArea',
                },
                directionDistance: {
                    name: 'directionDistance',
                },
            },
            numericFilters: {
                timeToMetro: {
                    name: 'timeToMetro',
                },
            },
            enumFilters: {
                metroTransport: {
                    name: 'metroTransport',
                    values: ['ON_FOOT', 'ON_TRANSPORT'],
                },
                placementPeriod: {
                    name: 'placementPeriod',
                    values: ['DAY', 'THREE_DAYS', 'WEEK', 'TWO_WEEKS', 'MONTH', 'THREE_MONTH'],
                },
                lotType: {
                    name: 'lotType',
                    values: ['IGS', 'GARDEN'],
                },
                agents: {
                    name: 'agents',
                    values: ['YES', 'NO'],
                },
                buildingEpoch: {
                    name: 'buildingEpoch',
                    values: ['KHRUSHCHEV', 'STALIN', 'BREZHNEV'],
                },
            },
            flagFilters: {
                hasPhoto: {
                    name: 'hasPhoto',
                },
                expectMetro: {
                    name: 'expectMetro',
                },
                hasPond: {
                    name: 'hasPond',
                },
                showOnMap: {
                    name: 'showOnMap',
                },
                hasPark: {
                    name: 'hasPark',
                },
            },
        },
    },
    sitesRgids: {
        district: 741964,
        mainCity: 587795,
    },
    latitude: 55.53113,
    longitude: 38.87476,
    zoom: 8,
    ridWithMetro: 213,
    administrativeDistrict: [
        {
            id: '17200983',
            name: 'Юго-Восточный административный округ',
            sublocalities: [193378, 12449, 12452, 12453, 193351, 193348, 193388, 12431, 193293, 193357, 12434, 193364],
            type: 'CITY_DISTRICT',
        },
        {
            id: '17202094',
            name: 'Северный административный округ',
            sublocalities: [
                193314,
                193379,
                193280,
                193319,
                193290,
                193291,
                193326,
                193295,
                193298,
                12433,
                193328,
                193302,
                12441,
                193307,
                193279,
                193373,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '17202391',
            name: 'Северо-Восточный административный округ',
            sublocalities: [
                193282,
                193346,
                193347,
                193345,
                193377,
                193350,
                193285,
                193349,
                17394073,
                193392,
                12435,
                193393,
                193371,
                193278,
                193374,
                193340,
                193341,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '183312',
            name: 'Восточный административный округ',
            sublocalities: [
                193283,
                193313,
                193380,
                12425,
                193288,
                193358,
                193391,
                193292,
                193362,
                193394,
                193363,
                193296,
                193367,
                193305,
                12444,
                12447,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '183313',
            name: 'Западный административный округ',
            sublocalities: [
                193286,
                193383,
                193323,
                193320,
                12427,
                193359,
                196718,
                193300,
                193370,
                12442,
                193401,
                12445,
                17367988,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '183316',
            name: 'Северо-Западный административный округ',
            sublocalities: [12432, 12448, 12451, 193366, 12439, 193317, 193352, 193327],
            type: 'CITY_DISTRICT',
        },
        {
            id: '596687',
            name: 'Центральный административный округ',
            sublocalities: [193344, 193318, 12437, 12438, 193301, 193368, 193321, 193308, 193324, 193389],
            type: 'CITY_DISTRICT',
        },
        {
            id: '596686',
            name: 'Южный административный округ',
            sublocalities: [
                12450,
                193281,
                193284,
                193355,
                193384,
                193289,
                193385,
                193356,
                193299,
                193297,
                193361,
                12440,
                193304,
                193336,
                12443,
                193337,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '596689',
            name: 'Юго-Западный административный округ',
            sublocalities: [
                193386,
                193387,
                193294,
                196717,
                193360,
                193334,
                193303,
                193332,
                193338,
                193339,
                193375,
                12446,
            ],
            type: 'CITY_DISTRICT',
        },
        {
            id: '2962',
            name: 'Новомосковский административный округ',
            sublocalities: [324289, 17367500, 230739, 17385368, 230737, 227292],
            type: 'CITY_DISTRICT',
        },
        {
            id: '2963',
            name: 'Троицкий административный округ',
            sublocalities: [17385369, 17385371, 230753, 17385370, 17385373, 17385372, 17385375, 17385374, 17385365],
            type: 'CITY_DISTRICT',
        },
        {
            id: '587654',
            name: 'Московская область',
            sublocalities: [
                2305,
                587658,
                2307,
                2308,
                2309,
                587661,
                587662,
                587663,
                2312,
                2317,
                2318,
                2319,
                587655,
                587672,
                2321,
                587673,
                2322,
                587674,
                2323,
                587677,
                2326,
                587679,
                587664,
                17385873,
                17385872,
                587666,
                17385875,
                17385874,
                587668,
                17385877,
                587669,
                17385876,
                17385879,
                587671,
                17385878,
                2339,
                587695,
                587680,
                2345,
                587681,
                587682,
                587683,
                587686,
                17383353,
                17383352,
                17383354,
                587697,
                17383351,
                17383350,
                587721,
                2290,
                2291,
                2292,
                2293,
                2294,
                2295,
                2296,
                2297,
                2298,
                2299,
                2300,
                2301,
            ],
            type: 'SUBJECT_FEDERATION',
        },
    ],
    sublocalities: [
        {
            id: '230739',
            name: 'Поселение Сосенское',
            locative: 'в Поселении Сосенском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17394073',
            name: 'Северный',
            locative: 'в Северном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2295',
            name: 'округ Дзержинский',
            locative: 'в Городском округе Дзержинском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587686',
            name: 'округ Подольск',
            locative: 'в Городском округе Подольск',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '324289',
            name: 'Поселение Воскресенское',
            locative: 'в Поселении Воскресенское',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193349',
            name: 'Марфино',
            locative: 'в Марфино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '227292',
            name: 'Поселение Внуковское',
            locative: 'в Поселении Внуковское',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12440',
            name: 'Царицыно',
            locative: 'в Царицыно',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193297',
            name: 'Даниловский',
            locative: 'в Даниловском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193296',
            name: 'Гольяново',
            locative: 'в Гольяново',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2309',
            name: 'округ Лосино-Петровский',
            locative: 'в Городском округе Лосино-Петровском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193328',
            name: 'Беговой',
            locative: 'в Беговом',
            type: 'CITY_DISTRICT',
        },
        {
            id: '230737',
            name: 'Поселение Мосрентген',
            locative: 'в Поселении Мосрентген',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367991',
            name: 'Старое Крюково',
            locative: 'в Старом Крюкове',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385874',
            name: 'округ Коломна',
            locative: 'в Коломенском городском округе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193285',
            name: 'Отрадное',
            locative: 'в Отрадном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193339',
            name: 'Южное Бутово',
            locative: 'в Южном Бутово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385872',
            name: 'Дмитровский округ',
            locative: 'в Дмитровском городском округе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193292',
            name: 'Восточное Измайлово',
            locative: 'в Восточном Измайлово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587662',
            name: 'округ Шаховская',
            locative: 'в Городском округе Шаховская',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193307',
            name: 'Коптево',
            locative: 'в Коптево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193393',
            name: 'Алексеевский',
            locative: 'в Алексеевском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193388',
            name: 'Лефортово',
            locative: 'в Лефортово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193375',
            name: 'Тёплый Стан',
            locative: 'в Тёплом Стане',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12447',
            name: 'Новокосино',
            locative: 'в Новокосино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193356',
            name: 'Нагорный',
            locative: 'в Нагорном районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193371',
            name: 'Останкинский',
            locative: 'в Останкинском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193324',
            name: 'Арбат',
            locative: 'в Арбате',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193361',
            name: 'Орехово-Борисово Северное',
            locative: 'в Орехово-Борисово Северном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193317',
            name: 'Строгино',
            locative: 'в Строгино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193289',
            name: 'Москворечье-Сабурово',
            locative: 'в Москворечье-Сабурово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17383352',
            name: 'округ Пущино',
            locative: 'в Городском округе Пущино',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587677',
            name: 'округ Ступино',
            locative: 'в Городском округе Ступино',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193394',
            name: 'Метрогородок',
            locative: 'в Метрогородке',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385372',
            name: 'Поселение Вороновское',
            locative: 'в Поселении Вороновском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193288',
            name: 'Косино-Ухтомский',
            locative: 'в Косино-Ухтомском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2339',
            name: 'Озёры (городской округ)',
            locative: 'в Городском округе Озёры',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193279',
            name: 'Аэропорт',
            locative: 'в Аэропорту',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193392',
            name: 'Ростокино',
            locative: 'в Ростокино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385875',
            name: 'Наро-Фоминский округ',
            locative: 'в Наро-Фоминском городском округе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587671',
            name: 'округ Серебряные Пруды',
            locative: 'в Городском округе Серебряные Пруды',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193320',
            name: 'Фили-Давыдково',
            locative: 'в Фили-Давыдково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12443',
            name: 'Чертаново Северное',
            locative: 'в Чертаново Северном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193360',
            name: 'Обручевский',
            locative: 'в Обручевском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193298',
            name: 'Дмитровский',
            locative: 'в Дмитровском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587682',
            name: 'Ленинский округ',
            locative: 'в Ленинском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2301',
            name: 'Звенигород (городской округ)',
            locative: 'в Городском округе Звенигород',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '12453',
            name: 'Рязанский',
            locative: 'в Рязанском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2291',
            name: 'округ Молодёжный',
            locative: 'в Городском округе ЗАТО Молодёжный',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193362',
            name: 'Измайлово',
            locative: 'в Измайлово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385365',
            name: 'Поселение Первомайское',
            locative: 'в Поселении Первомайском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587695',
            name: 'округ Солнечногорск',
            locative: 'в Солнечногорском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17385878',
            name: 'Раменский округ',
            locative: 'в Раменском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17383351',
            name: 'округ Лыткарино',
            locative: 'в Городском округе Лыткарино',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193367',
            name: 'Преображенское',
            locative: 'в Преображенском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385873',
            name: 'округ Клин',
            locative: 'в Городском округе Клин',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193295',
            name: 'Головинский',
            locative: 'в Головинском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12446',
            name: 'Академический',
            locative: 'в Академическом районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12425',
            name: 'Сокольники',
            locative: 'в Сокольниках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193327',
            name: 'Хорошёво-Мнёвники',
            locative: 'в Хорошёво-Мнёвниках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587658',
            name: 'Сергиево-Посадский округ',
            locative: 'в Сергиево-Посадском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2296',
            name: 'округ Долгопрудный',
            locative: 'в Городском округе Долгопрудном',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2307',
            name: 'округ Краснознаменск',
            locative: 'в Городском округе ЗАТО Краснознаменск',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '12437',
            name: 'Мещанский',
            locative: 'в Мещанском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193355',
            name: 'Нагатинский Затон',
            locative: 'в Нагатинском Затоне',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193374',
            name: 'Свиблово',
            locative: 'в Свиблово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '196717',
            name: 'Котловка',
            locative: 'в Котловке',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587668',
            name: 'округ Зарайск',
            locative: 'в Городском округе Зарайске',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193313',
            name: 'Северное Измайлово',
            locative: 'в Северном Измайлово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587655',
            name: 'округ Мытищи',
            locative: 'в Городском округе Мытищи',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587661',
            name: 'округ Лотошино',
            locative: 'в Лотошинском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587680',
            name: 'округ Воскресенск',
            locative: 'в Воскресенском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193377',
            name: 'Лианозово',
            locative: 'в Лианозово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193338',
            name: 'Северное Бутово',
            locative: 'в Северном Бутово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193291',
            name: 'Восточное Дегунино',
            locative: 'в Восточном Дегунино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193345',
            name: 'Ярославский',
            locative: 'в Ярославском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385373',
            name: 'Поселение Киевский',
            locative: 'в Поселении Киевском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2345',
            name: 'Рузский округ',
            locative: 'в Рузском городском округе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17385368',
            name: 'Поселение Московский',
            locative: 'в Поселении Московском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193323',
            name: 'Можайский',
            locative: 'в Можайском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12450',
            name: 'Нагатино-Садовники',
            locative: 'в Нагатино-Садовниках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2300',
            name: 'округ Звёздный городок',
            locative: 'в Городском округе ЗАТО Звёздный городок',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193305',
            name: 'Ивановское',
            locative: 'в Ивановском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193387',
            name: 'Коньково',
            locative: 'в Коньково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193370',
            name: 'Раменки',
            locative: 'в Раменках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367992',
            name: 'Силино',
            locative: 'в Силино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193359',
            name: 'Ново-Переделкино',
            locative: 'в Ново-Переделкино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193314',
            name: 'Сокол',
            locative: 'в Соколе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12449',
            name: 'Южнопортовый',
            locative: 'в Южнопортовом районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2323',
            name: 'округ Чехов',
            locative: 'в Городском округе Чехов',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2312',
            name: 'округ Протвино',
            locative: 'в Городском округе Протвино',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17385369',
            name: 'Поселение Краснопахорское',
            locative: 'в Поселении Краснопахорское',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2317',
            name: 'округ Фрязино',
            locative: 'в Городском округе Фрязино',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193366',
            name: 'Покровское-Стрешнево',
            locative: 'в Покровское-Стрешнево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587683',
            name: 'округ Красногорск',
            locative: 'в Городском округе Красногорск',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193391',
            name: 'Богородское',
            locative: 'в Богородском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193282',
            name: 'Бутырский',
            locative: 'в Бутырском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193321',
            name: 'Хамовники',
            locative: 'в Хамовниках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367988',
            name: 'Тропарёво-Никулино',
            locative: 'в Тропарёво-Никулино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193363',
            name: 'Перово',
            locative: 'в Перово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193385',
            name: 'Бирюлёво Западное',
            locative: 'в Бирюлёво Западном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12442',
            name: 'Проспект Вернадского',
            locative: 'в Проспекте Вернадского',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385877',
            name: 'округ Пушкинский',
            locative: 'в Пушкинском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193346',
            name: 'Лосиноостровский',
            locative: 'в Лосиноостровском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193281',
            name: 'Братеево',
            locative: 'в Братеево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2292',
            name: 'округ Балашиха',
            locative: 'в Городском округе Балашиха',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193302',
            name: 'Западное Дегунино',
            locative: 'в Западном Дегунино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193334',
            name: 'Ломоносовский',
            locative: 'в Ломоносовском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587672',
            name: 'округ Луховицы',
            locative: 'в Городском округе Луховицы',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193278',
            name: 'Алтуфьевский',
            locative: 'в Алтуфьевском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193378',
            name: 'Некрасовка',
            locative: 'в Некрасовке',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193299',
            name: 'Донской',
            locative: 'в Донском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193358',
            name: 'Новогиреево',
            locative: 'в Новогиреево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12441',
            name: 'Хорошёвский',
            locative: 'в Хорошёвском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587674',
            name: 'округ Шатура',
            locative: 'в Городском округе Шатура',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587679',
            name: 'Орехово-Зуевский округ',
            locative: 'в Городском округе Орехово-Зуево',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2326',
            name: 'округ Щёлково',
            locative: 'в Щёлковском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2293',
            name: 'округ Власиха',
            locative: 'в Городском округе ЗАТО Власиха',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '196718',
            name: 'Филёвский Парк',
            locative: 'в Филёвском Парке',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2308',
            name: 'округ Лобня',
            locative: 'в Городском округе Лобня',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193383',
            name: 'Крылатское',
            locative: 'в Крылатском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12451',
            name: 'Митино',
            locative: 'в Митино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193368',
            name: 'Пресненский',
            locative: 'в Пресненском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2319',
            name: 'округ Черноголовка',
            locative: 'в Городском округе Черноголовка',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193290',
            name: 'Войковский',
            locative: 'в Войковском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12445',
            name: 'Кунцево',
            locative: 'в Кунцево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193326',
            name: 'Ховрино',
            locative: 'в Ховрино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367989',
            name: 'Матушкино',
            locative: 'в Матушкино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193336',
            name: 'Чертаново Центральное',
            locative: 'в Чертаново Центральном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2299',
            name: 'округ Жуковский',
            locative: 'в Городском округе Жуковском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17367500',
            name: 'Поселение Рязановское',
            locative: 'в Поселении Рязановском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17383350',
            name: 'округ Королёв',
            locative: 'в Городском округе Королёв',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17385374',
            name: 'Поселение Новофёдоровское',
            locative: 'в Поселении Новофедоровском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12433',
            name: 'Левобережный',
            locative: 'в Левобережном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17383353',
            name: 'округ Реутов',
            locative: 'в Городском округе Реутов',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587664',
            name: 'Можайский округ',
            locative: 'в Городском округе Можайском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587721',
            name: 'Одинцовский округ',
            locative: 'в Одинцовском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '12438',
            name: 'Тверской',
            locative: 'в Тверском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193386',
            name: 'Ясенево',
            locative: 'в Ясенево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193294',
            name: 'Гагаринский',
            locative: 'в Гагаринском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193341',
            name: 'Южное Медведково',
            locative: 'в Южном Медведково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193350',
            name: 'Марьина Роща',
            locative: 'в Марьиной Роще',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587697',
            name: 'округ Люберцы',
            locative: 'в Городском округе Люберцы',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '12432',
            name: 'Куркино',
            locative: 'в Куркино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193373',
            name: 'Савёловский',
            locative: 'в Савёловском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193332',
            name: 'Черёмушки',
            locative: 'в Черёмушках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385370',
            name: 'Поселение Михайлово-Ярцевское',
            locative: 'в Поселении Михайлово-Ярцевском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193337',
            name: 'Чертаново Южное',
            locative: 'в Чертаново Южном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2318',
            name: 'округ Химки',
            locative: 'в Городском округе Химки',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193347',
            name: 'Бабушкинский',
            locative: 'в Бабушкинском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12434',
            name: 'Кузьминки',
            locative: 'в Кузьминках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193303',
            name: 'Зюзино',
            locative: 'в Зюзино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2298',
            name: 'округ Дубна',
            locative: 'в Городском округе Дубна',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193300',
            name: 'Дорогомилово',
            locative: 'в Дорогомилово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587669',
            name: 'округ Кашира',
            locative: 'в Городском округе Кашира',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193318',
            name: 'Таганский',
            locative: 'в Таганском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193401',
            name: 'Внуково',
            locative: 'во Внуково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193283',
            name: 'Вешняки',
            locative: 'в Вешняках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193379',
            name: 'Молжаниновский',
            locative: 'в Молжаниновском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193286',
            name: 'Очаково-Матвеевское',
            locative: 'в Очаково-Матвеевском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2322',
            name: 'Талдомский округ',
            locative: 'в Городском округе Талдомском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193364',
            name: 'Печатники',
            locative: 'в Печатниках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12439',
            name: 'Щукино',
            locative: 'в Щукино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587673',
            name: 'округ Егорьевск',
            locative: 'в Городском округе Егорьевск',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193384',
            name: 'Бирюлёво Восточное',
            locative: 'в Бирюлёво Восточном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '587663',
            name: 'Волоколамский округ',
            locative: 'в Волоколамском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193352',
            name: 'Северное Тушино',
            locative: 'в Северном Тушино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385371',
            name: 'Поселение Щаповское',
            locative: 'в Поселении Щаповском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2305',
            name: 'округ Котельники',
            locative: 'в Городском округе Котельники',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '230753',
            name: 'Поселение Кленовское',
            locative: 'в Поселении Кленовском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12452',
            name: 'Текстильщики',
            locative: 'в Текстильщиках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2290',
            name: 'округ Бронницы',
            locative: 'в Городском округе Бронницы',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193344',
            name: 'Якиманка',
            locative: 'в Якиманке',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193301',
            name: 'Замоскворечье',
            locative: 'в Замоскворечье',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367990',
            name: 'Савёлки',
            locative: 'в Савёлках',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193308',
            name: 'Красносельский',
            locative: 'в Красносельском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193340',
            name: 'Северное Медведково',
            locative: 'в Северном Медведково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17367987',
            name: 'Крюково',
            locative: 'в Крюково',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12448',
            name: 'Южное Тушино',
            locative: 'в Южном Тушино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12427',
            name: 'Солнцево',
            locative: 'в Солнцево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385375',
            name: 'Поселение Роговское',
            locative: 'в Поселении Роговском',
            type: 'CITY_DISTRICT',
        },
        {
            id: '2294',
            name: 'округ Восход',
            locative: 'в Городском округе ЗАТО Восход',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587681',
            name: 'округ Павловский Посад',
            locative: 'в Городском округе Павловский Посад',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193389',
            name: 'Басманный',
            locative: 'в Басманном районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193304',
            name: 'Зябликово',
            locative: 'в Зябликово',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17383354',
            name: 'округ Электрогорск',
            locative: 'в Городском округе Электрогорск',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '587666',
            name: 'округ Истра',
            locative: 'в Городском округе Истра',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '17385876',
            name: 'Богородский округ',
            locative: 'в Городском округе Богородском',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193280',
            name: 'Бескудниковский',
            locative: 'в Бескудниковском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193319',
            name: 'Тимирязевский',
            locative: 'в Тимирязевском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193357',
            name: 'Нижегородский',
            locative: 'в Нижегородском районе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193293',
            name: 'Выхино-Жулебино',
            locative: 'в Выхино-Жулебино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12431',
            name: 'Капотня',
            locative: 'в Капотне',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12444',
            name: 'Соколиная Гора',
            locative: 'в Соколиной горе',
            type: 'CITY_DISTRICT',
        },
        {
            id: '12435',
            name: 'Бибирево',
            locative: 'в Бибирево',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193284',
            name: 'Орехово-Борисово Южное',
            locative: 'в Орехово-Борисово Южное',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193380',
            name: 'Восточный',
            locative: 'в Восточном',
            type: 'CITY_DISTRICT',
        },
        {
            id: '17385879',
            name: 'округ Серпухов',
            locative: 'в Серпуховском районе',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2321',
            name: 'округ Электросталь',
            locative: 'в Городском округе Электросталь',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '2297',
            name: 'округ Домодедово',
            locative: 'в Городском округе Домодедово',
            type: 'SUBJECT_FEDERATION_DISTRICT',
        },
        {
            id: '193351',
            name: 'Марьино',
            locative: 'в Марьино',
            type: 'CITY_DISTRICT',
        },
        {
            id: '193348',
            name: 'Люблино',
            locative: 'в Люблино',
            type: 'CITY_DISTRICT',
        },
    ],
    highwayDirections: [
        {
            id: '1',
            name: 'Ярославское направление',
            highways: [
                {
                    id: '1',
                    name: 'Осташковское шоссе',
                    locative: 'на Осташковском шоссе',
                    genitive: 'Осташковского шоссе',
                },
                {
                    id: '2',
                    name: 'Ярославское шоссе',
                    locative: 'на Ярославском шоссе',
                    genitive: 'Ярославского шоссе',
                },
                {
                    id: '3',
                    name: 'Фряновское шоссе',
                    locative: 'на Фряновском шоссе',
                    genitive: 'Фряновского шоссе',
                },
                {
                    id: '4',
                    name: 'Щёлковское шоссе',
                    locative: 'на Щёлковском шоссе',
                    genitive: 'Щёлковского шоссе',
                },
            ],
        },
        {
            id: '2',
            name: 'Ленинградское направление',
            highways: [
                {
                    id: '5',
                    name: 'Куркинское шоссе',
                    locative: 'на Куркинском шоссе',
                    genitive: 'Куркинского шоссе',
                },
                {
                    id: '6',
                    name: 'Новокуркинское шоссе',
                    locative: 'на Новокуркинском шоссе',
                    genitive: 'Новокуркинского шоссе',
                },
                {
                    id: '7',
                    name: 'Ленинградское шоссе',
                    locative: 'на Ленинградском шоссе',
                    genitive: 'Ленинградского шоссе',
                },
                {
                    id: '8',
                    name: 'Пятницкое шоссе',
                    locative: 'на Пятницком шоссе',
                    genitive: 'Пятницкого шоссе',
                },
                {
                    id: '9',
                    name: 'Новосходненское шоссе',
                    locative: 'на Новосходненском шоссе',
                    genitive: 'Новосходненского шоссе',
                },
                {
                    id: '10',
                    name: 'Машкинское шоссе',
                    locative: 'на Машкинском шоссе',
                    genitive: 'Машкинского шоссе',
                },
            ],
        },
        {
            id: '3',
            name: 'Киевское направление',
            highways: [
                {
                    id: '11',
                    name: 'Киевское шоссе',
                    locative: 'на Киевском шоссе',
                    genitive: 'Киевского шоссе',
                },
                {
                    id: '12',
                    name: 'Боровское шоссе',
                    locative: 'на Боровском шоссе',
                    genitive: 'Боровского шоссе',
                },
            ],
        },
        {
            id: '4',
            name: 'Павелецкое направление',
            highways: [
                {
                    id: '13',
                    name: 'Новокаширское шоссе',
                    locative: 'на Новокаширском шоссе',
                    genitive: 'Новокаширского шоссе',
                },
                {
                    id: '14',
                    name: 'Каширское шоссе',
                    locative: 'на Каширском шоссе',
                    genitive: 'Каширского шоссе',
                },
            ],
        },
        {
            id: '5',
            name: 'Казанское направление',
            highways: [
                {
                    id: '15',
                    name: 'Егорьевское шоссе',
                    locative: 'на Егорьевском шоссе',
                    genitive: 'Егорьевского шоссе',
                },
                {
                    id: '16',
                    name: 'Новорязанское шоссе',
                    locative: 'на Новорязанском шоссе',
                    genitive: 'Новорязанского шоссе',
                },
                {
                    id: '17',
                    name: 'Быковское шоссе',
                    locative: 'на Быковском шоссе',
                    genitive: 'Быковского шоссе',
                },
                {
                    id: '18',
                    name: 'Рязанское шоссе',
                    locative: 'на Рязанском шоссе',
                    genitive: 'Рязанского шоссе',
                },
            ],
        },
        {
            id: '6',
            name: 'Рижское направление',
            highways: [
                {
                    id: '19',
                    name: 'Волоколамское шоссе',
                    locative: 'на Волоколамском шоссе',
                    genitive: 'Волоколамского шоссе',
                },
                {
                    id: '20',
                    name: 'Новорижское шоссе',
                    locative: 'на Новорижском шоссе',
                    genitive: 'Новорижского шоссе',
                },
                {
                    id: '21',
                    name: 'Путилковское шоссе',
                    locative: 'на Путилковском шоссе',
                    genitive: 'Путилковского шоссе',
                },
            ],
        },
        {
            id: '7',
            name: 'Савеловское направление',
            highways: [
                {
                    id: '22',
                    name: 'Алтуфьевское шоссе',
                    locative: 'на Алтуфьевском шоссе',
                    genitive: 'Алтуфьевского шоссе',
                },
                {
                    id: '23',
                    name: 'Дмитровское шоссе',
                    locative: 'на Дмитровском шоссе',
                    genitive: 'Дмитровского шоссе',
                },
                {
                    id: '24',
                    name: 'Рогачёвское шоссе',
                    locative: 'на Рогачёвском шоссе',
                    genitive: 'Рогачёвского шоссе',
                },
            ],
        },
        {
            id: '8',
            name: 'Курское направление',
            highways: [
                {
                    id: '25',
                    name: 'Калужское шоссе',
                    locative: 'на Калужском шоссе',
                    genitive: 'Калужского шоссе',
                },
                {
                    id: '26',
                    name: 'Варшавское шоссе',
                    locative: 'на Варшавском шоссе',
                    genitive: 'Варшавского шоссе',
                },
                {
                    id: '27',
                    name: 'Симферопольское шоссе',
                    locative: 'на Симферопольском шоссе',
                    genitive: 'Симферопольского шоссе',
                },
                {
                    id: '28',
                    name: 'Домодедовское шоссе',
                    locative: 'на Домодедовском шоссе',
                    genitive: 'Домодедовского шоссе',
                },
            ],
        },
        {
            id: '9',
            name: 'Горьковское направление',
            highways: [
                {
                    id: '29',
                    name: 'Горьковское шоссе',
                    locative: 'на Горьковском шоссе',
                    genitive: 'Горьковского шоссе',
                },
                {
                    id: '30',
                    name: 'Носовихинское шоссе',
                    locative: 'на Носовихинском шоссе',
                    genitive: 'Носовихинского шоссе',
                },
            ],
        },
        {
            id: '10',
            name: 'Белорусское направление',
            highways: [
                {
                    id: '31',
                    name: 'Ильинское шоссе',
                    locative: 'на Ильинском шоссе',
                    genitive: 'Ильинского шоссе',
                },
                {
                    id: '32',
                    name: 'Минское шоссе',
                    locative: 'на Минском шоссе',
                    genitive: 'Минского шоссе',
                },
                {
                    id: '33',
                    name: 'Можайское шоссе',
                    locative: 'на Можайском шоссе',
                    genitive: 'Можайского шоссе',
                },
                {
                    id: '34',
                    name: 'Рублёво-Успенское шоссе',
                    locative: 'на Рублёво-Успенском шоссе',
                    genitive: 'Рублёво-Успенского шоссе',
                },
                {
                    id: '35',
                    name: 'Рублёвское шоссе',
                    locative: 'на Рублёвском шоссе',
                    genitive: 'Рублёвского шоссе',
                },
                {
                    id: '36',
                    name: 'Сколковское шоссе',
                    locative: 'на Сколковском шоссе',
                    genitive: 'Сколковского шоссе',
                },
                {
                    id: '37',
                    name: '1-е Успенское шоссе',
                    locative: 'на 1-ом Успенском шоссе',
                    genitive: '1-ого Успенского шоссе',
                },
                {
                    id: '38',
                    name: 'Подушкинское шоссе',
                    locative: 'на Подушкинском шоссе',
                    genitive: 'Подушкинского шоссе',
                },
                {
                    id: '39',
                    name: 'Красногорское шоссе',
                    locative: 'на Красногорском шоссе',
                    genitive: 'Красногорского шоссе',
                },
                {
                    id: '40',
                    name: '2-е Успенское шоссе',
                    locative: 'на 2-ом Успенском шоссе',
                    genitive: '2-ого Успенского шоссе',
                },
            ],
        },
    ],
    railwayDirections: [
        {
            name: 'Рижское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '196108',
                            name: 'Москва (Рижский вокзал)',
                        },
                        {
                            id: '191621',
                            name: 'Рижская (МЦД-2)',
                        },
                        {
                            id: '196112',
                            name: 'Дмитровская',
                        },
                        {
                            id: '196201',
                            name: 'Гражданская',
                        },
                        {
                            id: '196216',
                            name: 'Красный Балтиец',
                        },
                        {
                            id: '196220',
                            name: 'Стрешнево (МЦД-2)',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '196316',
                            name: 'Покровско-Стрешнево',
                        },
                        {
                            id: '196339',
                            name: 'Щукинская',
                        },
                        {
                            id: '196409',
                            name: 'Тушинская (Тушино)',
                        },
                        {
                            id: '196413',
                            name: 'Трикотажная',
                        },
                        {
                            id: '346',
                            name: 'Волоколамская',
                        },
                        {
                            id: '347',
                            name: 'Пенягино',
                        },
                        {
                            id: '196502',
                            name: 'Павшино',
                        },
                        {
                            id: '196517',
                            name: 'Красногорская',
                        },
                        {
                            id: '196521',
                            name: 'Опалиха',
                        },
                        {
                            id: '196536',
                            name: 'Аникеевка',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '196606',
                            name: 'Нахабино',
                        },
                        {
                            id: '196610',
                            name: 'Малиновка',
                        },
                        {
                            id: '196803',
                            name: 'Дедовск',
                        },
                        {
                            id: '196818',
                            name: 'Миитовская',
                        },
                        {
                            id: '196907',
                            name: 'Снегири',
                        },
                        {
                            id: '196911',
                            name: '50 км',
                        },
                        {
                            id: '197007',
                            name: 'Манихино 1',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '197238',
                            name: 'Троицкая',
                        },
                        {
                            id: '197223',
                            name: 'Истра',
                        },
                        {
                            id: '197204',
                            name: 'Новоиерусалимская',
                        },
                        {
                            id: '197219',
                            name: 'Чеховская',
                        },
                        {
                            id: '197308',
                            name: 'Холщёвики',
                        },
                        {
                            id: '197312',
                            name: '73 км',
                        },
                        {
                            id: '197327',
                            name: 'Ядрошино',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '197331',
                            name: 'Курсаковская',
                        },
                        {
                            id: '197401',
                            name: 'Румянцево',
                        },
                        {
                            id: '197416',
                            name: 'Новопетровская',
                        },
                        {
                            id: '197420',
                            name: 'Устиновка',
                        },
                        {
                            id: '197435',
                            name: '91 км',
                        },
                        {
                            id: '197445',
                            name: 'Лесодолгоруково',
                        },
                        {
                            id: '197505',
                            name: 'Чисмена',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '197511',
                            name: 'Матрёнино',
                        },
                        {
                            id: '197524',
                            name: 'Дубосеково',
                        },
                        {
                            id: '197609',
                            name: 'Волоколамск',
                        },
                        {
                            id: '316',
                            name: '133 км',
                        },
                        {
                            id: '197613',
                            name: 'Благовещенское',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '317',
                            name: '141 км',
                        },
                        {
                            id: '197628',
                            name: 'Бухолово',
                        },
                        {
                            id: '318',
                            name: '149 км',
                        },
                        {
                            id: '197702',
                            name: 'Шаховская',
                        },
                        {
                            id: '63325',
                            name: 'Муриково',
                        },
                        {
                            id: '610',
                            name: '165 км',
                        },
                        {
                            id: '609',
                            name: '167 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '63315',
                            name: 'Княжьи Горы',
                        },
                        {
                            id: '608',
                            name: '176 км',
                        },
                        {
                            id: '607',
                            name: '180 км',
                        },
                        {
                            id: '63349',
                            name: 'Обовражье',
                        },
                        {
                            id: '606',
                            name: '188 км',
                        },
                        {
                            id: '63404',
                            name: 'Погорелое Городище',
                        },
                        {
                            id: '63419',
                            name: 'Курково',
                        },
                        {
                            id: '605',
                            name: '208 км',
                        },
                        {
                            id: '63423',
                            name: 'Бартенево',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '63508',
                            name: 'Зубцов',
                        },
                        {
                            id: '63512',
                            name: 'Аристово',
                        },
                        {
                            id: '63006',
                            name: 'Ржев-Балтийский',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Горьковское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '995',
                            name: 'Москва (Восточный вокзал)',
                        },
                        {
                            id: '193735',
                            name: 'Серп и Молот',
                        },
                        {
                            id: '191602',
                            name: 'Москва (Курский вокзал)',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '193720',
                            name: 'Нижегородская (Горьковское направление)',
                        },
                        {
                            id: '193716',
                            name: 'Чухлинка',
                        },
                        {
                            id: '193701',
                            name: 'Кусково',
                        },
                        {
                            id: '193743',
                            name: 'Новогиреево',
                        },
                        {
                            id: '230101',
                            name: 'Реутов',
                        },
                        {
                            id: '230116',
                            name: 'Никольское',
                        },
                        {
                            id: '230120',
                            name: 'Салтыковская',
                        },
                        {
                            id: '230205',
                            name: 'Стройка',
                        },
                        {
                            id: '230135',
                            name: 'Кучино',
                        },
                        {
                            id: '230436',
                            name: 'Ольгино',
                        },
                        {
                            id: '230402',
                            name: 'Железнодорожная',
                        },
                        {
                            id: '230214',
                            name: 'Горенки',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '230309',
                            name: 'Балашиха',
                        },
                        {
                            id: '230417',
                            name: 'Чёрное',
                        },
                        {
                            id: '305',
                            name: 'Заря',
                        },
                        {
                            id: '230506',
                            name: 'Купавна',
                        },
                        {
                            id: '230510',
                            name: '33 км',
                        },
                        {
                            id: '230600',
                            name: 'Электроугли',
                        },
                        {
                            id: '230614',
                            name: '43 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '230703',
                            name: 'Храпуново',
                        },
                        {
                            id: '230718',
                            name: 'Есино',
                        },
                        {
                            id: '230807',
                            name: 'Фрязево',
                        },
                        {
                            id: '230830',
                            name: 'Металлург',
                        },
                        {
                            id: '230900',
                            name: 'Электросталь',
                        },
                        {
                            id: '230915',
                            name: 'Машиностроитель',
                        },
                        {
                            id: '230811',
                            name: 'Казанское',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '231000',
                            name: 'Ногинск',
                        },
                        {
                            id: '230826',
                            name: 'Вохна',
                        },
                        {
                            id: '231015',
                            name: 'Захарово',
                        },
                        {
                            id: '231104',
                            name: 'Павловский Посад',
                        },
                        {
                            id: '231123',
                            name: 'Ленская',
                        },
                        {
                            id: '231138',
                            name: 'Ковригино',
                        },
                        {
                            id: '231119',
                            name: 'Назарьево',
                        },
                        {
                            id: '231231',
                            name: 'Дрезна',
                        },
                        {
                            id: '231142',
                            name: '14 км',
                        },
                        {
                            id: '231157',
                            name: 'Электрогорск',
                        },
                        {
                            id: '231212',
                            name: 'Кабаново',
                        },
                        {
                            id: '306',
                            name: '87 км',
                        },
                        {
                            id: '230008',
                            name: 'Орехово-Зуево',
                        },
                        {
                            id: '230012',
                            name: 'Крутое',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '230027',
                            name: 'Войново',
                        },
                        {
                            id: '231405',
                            name: 'Усад',
                        },
                        {
                            id: '231410',
                            name: 'Глубоково',
                        },
                        {
                            id: '231509',
                            name: 'Покров',
                        },
                        {
                            id: '231513',
                            name: '113 км',
                        },
                        {
                            id: '231528',
                            name: 'Омутище',
                        },
                        {
                            id: '231532',
                            name: 'Леоново',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '262102',
                            name: 'Петушки',
                        },
                        {
                            id: '262206',
                            name: 'Костерёво',
                        },
                        {
                            id: '262303',
                            name: 'Болдино',
                        },
                        {
                            id: '262314',
                            name: 'Сушнево',
                        },
                        {
                            id: '262329',
                            name: 'Красная Охота',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '262403',
                            name: 'Ундол',
                        },
                        {
                            id: '262418',
                            name: '170 км',
                        },
                        {
                            id: '262511',
                            name: 'Колокша',
                        },
                        {
                            id: '262600',
                            name: 'Юрьевец',
                        },
                        {
                            id: '262704',
                            name: 'Владимир',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Московский монорельс',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '399',
                            name: 'Тимирязевская, монорельс',
                        },
                        {
                            id: '398',
                            name: 'Улица Милашенкова',
                        },
                        {
                            id: '397',
                            name: 'Телецентр',
                        },
                        {
                            id: '396',
                            name: 'Улица Академика Королёва',
                        },
                        {
                            id: '395',
                            name: 'Выставочный центр',
                        },
                        {
                            id: '394',
                            name: 'Улица Сергея Эйзенштейна',
                        },
                    ],
                },
            ],
        },
        {
            name: 'МЦД-1',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '182209',
                            name: 'Одинцово',
                        },
                        {
                            id: '181757',
                            name: 'Баковка',
                        },
                        {
                            id: '181742',
                            name: 'Сколково',
                        },
                        {
                            id: '181738',
                            name: 'Немчиновка',
                        },
                        {
                            id: '181723',
                            name: 'Сетунь',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '181719',
                            name: 'Рабочий Посёлок',
                        },
                        {
                            id: '181704',
                            name: 'Кунцевская',
                        },
                        {
                            id: '181615',
                            name: 'Славянский Бульвар',
                        },
                        {
                            id: '181600',
                            name: 'Фили',
                        },
                        {
                            id: '198226',
                            name: 'Тестовская',
                        },
                        {
                            id: '198211',
                            name: 'Беговая',
                        },
                        {
                            id: '198230',
                            name: 'Москва (Белорусский вокзал)',
                        },
                        {
                            id: '196004',
                            name: 'Москва (Савёловский вокзал)',
                        },
                        {
                            id: '196019',
                            name: 'Тимирязевская',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '196023',
                            name: 'Окружная',
                        },
                        {
                            id: '196038',
                            name: 'Дегунино',
                        },
                        {
                            id: '195800',
                            name: 'Бескудниково',
                        },
                        {
                            id: '195815',
                            name: 'Лианозово',
                        },
                        {
                            id: '237806',
                            name: 'Марк',
                        },
                        {
                            id: '237810',
                            name: 'Новодачная',
                        },
                        {
                            id: '237825',
                            name: 'Долгопрудная',
                        },
                        {
                            id: '237831',
                            name: 'Водники',
                        },
                        {
                            id: '237844',
                            name: 'Хлебниково',
                        },
                        {
                            id: '237859',
                            name: 'Шереметьевская',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '237908',
                            name: 'Лобня',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Киевское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '198103',
                            name: 'Москва (Киевский вокзал)',
                        },
                        {
                            id: '197907',
                            name: 'Поклонная',
                        },
                        {
                            id: '197933',
                            name: 'Минская',
                        },
                        {
                            id: '180218',
                            name: 'Матвеевская',
                        },
                        {
                            id: '180237',
                            name: 'Аминьевская',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '180203',
                            name: 'Очаково',
                        },
                        {
                            id: '180222',
                            name: 'Мещерская',
                        },
                        {
                            id: '180504',
                            name: 'Солнечная',
                        },
                        {
                            id: '180519',
                            name: 'Переделкино',
                        },
                        {
                            id: '374',
                            name: 'Новопеределкино',
                        },
                        {
                            id: '180523',
                            name: 'Мичуринец',
                        },
                        {
                            id: '180608',
                            name: 'Внуково',
                        },
                        {
                            id: '180612',
                            name: 'Лесной Городок',
                        },
                        {
                            id: '180805',
                            name: 'Толстопальцево',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '372',
                            name: 'Аэропорт (старая платф.)',
                        },
                        {
                            id: '180812',
                            name: 'Кокошкино',
                        },
                        {
                            id: '345',
                            name: 'Санино',
                        },
                        {
                            id: '180909',
                            name: 'Крёкшино',
                        },
                        {
                            id: '180913',
                            name: 'Победа',
                        },
                        {
                            id: '181009',
                            name: 'Апрелевка',
                        },
                        {
                            id: '181013',
                            name: 'Дачная',
                        },
                        {
                            id: '181028',
                            name: 'Алабино',
                        },
                        {
                            id: '181102',
                            name: 'Селятино',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '181117',
                            name: 'Рассудово',
                        },
                        {
                            id: '181121',
                            name: 'Ожигово',
                        },
                        {
                            id: '180025',
                            name: 'Посёлок Киевский',
                        },
                        {
                            id: '180006',
                            name: 'Бекасово-сортировочное',
                        },
                        {
                            id: '180010',
                            name: 'Бекасово-1',
                        },
                        {
                            id: '180031',
                            name: 'Бекасово-центральное',
                        },
                        {
                            id: '180044',
                            name: 'Зосимова Пустынь',
                        },
                        {
                            id: '181441',
                            name: '240 км',
                        },
                        {
                            id: '181437',
                            name: '241 км',
                        },
                        {
                            id: '183305',
                            name: 'Нара',
                        },
                        {
                            id: '181422',
                            name: 'Мачихино',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '183409',
                            name: 'Латышская',
                        },
                        {
                            id: '312',
                            name: '250 км',
                        },
                        {
                            id: '181418',
                            name: '252 км',
                        },
                        {
                            id: '183413',
                            name: 'Башкино',
                        },
                        {
                            id: '181403',
                            name: 'Кресты',
                        },
                        {
                            id: '183428',
                            name: 'Ворсино',
                        },
                        {
                            id: '183606',
                            name: 'Балабаново',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '183704',
                            name: 'Обнинское',
                        },
                        {
                            id: '183803',
                            name: 'Шемякино',
                        },
                        {
                            id: '183907',
                            name: 'Малоярославец',
                        },
                        {
                            id: '184026',
                            name: 'Ерденево',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '184011',
                            name: '140 км',
                        },
                        {
                            id: '184100',
                            name: 'Суходрев',
                        },
                        {
                            id: '184115',
                            name: 'Родинка',
                        },
                        {
                            id: '184129',
                            name: 'Сляднево',
                        },
                        {
                            id: '184134',
                            name: '167 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '184204',
                            name: 'Тихонова Пустынь',
                        },
                        {
                            id: '188718',
                            name: 'Муратовка',
                        },
                        {
                            id: '184219',
                            name: 'Горенская',
                        },
                        {
                            id: '188614',
                            name: 'Садовая',
                        },
                        {
                            id: '188601',
                            name: 'Азарово',
                        },
                        {
                            id: '188205',
                            name: 'Калуга-1',
                        },
                        {
                            id: '184401',
                            name: 'Калуга-2',
                        },
                        {
                            id: '184420',
                            name: '188 км',
                        },
                        {
                            id: '184505',
                            name: 'Воротынск',
                        },
                        {
                            id: '184514',
                            name: '196 км',
                        },
                        {
                            id: '184524',
                            name: '200 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '184539',
                            name: 'Дяглевка',
                        },
                        {
                            id: '184609',
                            name: 'Бабынино',
                        },
                        {
                            id: '184543',
                            name: '218 км',
                        },
                        {
                            id: '184721',
                            name: 'Домашёвка',
                        },
                        {
                            id: '184717',
                            name: 'Липицы',
                        },
                        {
                            id: '184806',
                            name: 'Кудринская',
                        },
                        {
                            id: '184810',
                            name: '245 км',
                        },
                        {
                            id: '184825',
                            name: 'Хотень',
                        },
                        {
                            id: '185005',
                            name: 'Сухиничи-узловые',
                        },
                        {
                            id: '185207',
                            name: 'Сухиничи-главные',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Ярославское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '195506',
                            name: 'Москва (Ярославский вокзал)',
                        },
                        {
                            id: '195514',
                            name: 'Москва-3',
                        },
                        {
                            id: '195529',
                            name: 'Маленковская',
                        },
                        {
                            id: '195533',
                            name: 'Яуза',
                        },
                        {
                            id: '195548',
                            name: 'Ростокино (Ярославское направление)',
                        },
                        {
                            id: '195406',
                            name: 'Лосиноостровская',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '195410',
                            name: 'Лось',
                        },
                        {
                            id: '195425',
                            name: 'Перловская',
                        },
                        {
                            id: '195430',
                            name: 'Тайнинская',
                        },
                        {
                            id: '234808',
                            name: 'Мытищи',
                        },
                        {
                            id: '234812',
                            name: 'Строитель',
                        },
                        {
                            id: '234827',
                            name: 'Челюскинская',
                        },
                        {
                            id: '234901',
                            name: 'Подлипки-Дачные',
                        },
                        {
                            id: '234831',
                            name: 'Тарасовская',
                        },
                        {
                            id: '235001',
                            name: 'Болшево',
                        },
                        {
                            id: '234846',
                            name: 'Клязьма',
                        },
                        {
                            id: '235016',
                            name: 'Валентиновка',
                        },
                        {
                            id: '235035',
                            name: 'Фабрика 1 Мая',
                        },
                        {
                            id: '234850',
                            name: 'Мамонтовская',
                        },
                        {
                            id: '235020',
                            name: 'Загорянская',
                        },
                        {
                            id: '235105',
                            name: 'Зелёный Бор',
                        },
                        {
                            id: '235904',
                            name: 'Пушкино',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '235406',
                            name: 'Соколовская',
                        },
                        {
                            id: '235124',
                            name: 'Ивантеевка-2',
                        },
                        {
                            id: '235410',
                            name: 'Воронок',
                        },
                        {
                            id: '235919',
                            name: 'Заветы Ильича',
                        },
                        {
                            id: '235209',
                            name: 'Ивантеевка',
                        },
                        {
                            id: '235507',
                            name: 'Щёлково',
                        },
                        {
                            id: '235213',
                            name: 'Детская',
                        },
                        {
                            id: '235923',
                            name: 'Правда',
                        },
                        {
                            id: '235514',
                            name: 'Гагаринская',
                        },
                        {
                            id: '235603',
                            name: 'Чкаловская',
                        },
                        {
                            id: '235302',
                            name: 'Фрязино-Тов.',
                        },
                        {
                            id: '235938',
                            name: 'Зеленоградская',
                        },
                        {
                            id: '235317',
                            name: 'Фрязино-Пасс.',
                        },
                        {
                            id: '235618',
                            name: 'Бахчиванджи',
                        },
                        {
                            id: '235942',
                            name: '43 км',
                        },
                        {
                            id: '235622',
                            name: 'Циолковская',
                        },
                        {
                            id: '236004',
                            name: 'Софрино',
                        },
                        {
                            id: '235637',
                            name: 'Осеевская',
                        },
                        {
                            id: '236112',
                            name: 'Посёлок Дальний',
                        },
                        {
                            id: '236127',
                            name: 'Рахманово',
                        },
                        {
                            id: '236019',
                            name: 'Ашукинская',
                        },
                        {
                            id: '235707',
                            name: 'Монино',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '236023',
                            name: 'Калистово',
                        },
                        {
                            id: '236131',
                            name: 'Фёдоровское',
                        },
                        {
                            id: '235711',
                            name: 'Кашино',
                        },
                        {
                            id: '236038',
                            name: 'Радонеж',
                        },
                        {
                            id: '236146',
                            name: 'Путилово',
                        },
                        {
                            id: '236057',
                            name: 'Абрамцево',
                        },
                        {
                            id: '235726',
                            name: 'Колонтаево',
                        },
                        {
                            id: '236108',
                            name: 'Красноармейск',
                        },
                        {
                            id: '236201',
                            name: 'Хотьково',
                        },
                        {
                            id: '235730',
                            name: 'Лесная (64 км)',
                        },
                        {
                            id: '236216',
                            name: 'Семхоз',
                        },
                        {
                            id: '236305',
                            name: 'Сергиев Посад',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '230807',
                            name: 'Фрязево',
                        },
                        {
                            id: '236317',
                            name: '76 км',
                        },
                        {
                            id: '236324',
                            name: '81 км',
                        },
                        {
                            id: '236339',
                            name: '83 км',
                        },
                        {
                            id: '236702',
                            name: 'Бужаниново',
                        },
                        {
                            id: '236714',
                            name: '90 км',
                        },
                        {
                            id: '236803',
                            name: 'Арсаки',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '236907',
                            name: 'Струнино',
                        },
                        {
                            id: '237007',
                            name: 'Александров-1',
                        },
                        {
                            id: '102',
                            name: '117 км',
                        },
                        {
                            id: '313319',
                            name: 'Мошнино',
                        },
                        {
                            id: '313304',
                            name: 'Балакирево',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '313323',
                            name: 'Багримово',
                        },
                        {
                            id: '313338',
                            name: '142 км',
                        },
                        {
                            id: '313408',
                            name: 'Берендеево',
                        },
                        {
                            id: '313412',
                            name: '147 км',
                        },
                        {
                            id: '313639',
                            name: 'Шушково',
                        },
                        {
                            id: '313624',
                            name: '155 км',
                        },
                        {
                            id: '313615',
                            name: 'Рокша',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '313709',
                            name: 'Рязанцево',
                        },
                        {
                            id: '313802',
                            name: 'Беклемишево',
                        },
                        {
                            id: '313910',
                            name: 'Итларь',
                        },
                        {
                            id: '313817',
                            name: '187 км',
                        },
                        {
                            id: '314006',
                            name: 'Сильницы',
                        },
                        {
                            id: '314106',
                            name: 'Петровск',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '314203',
                            name: 'Деболовская',
                        },
                        {
                            id: '314307',
                            name: 'Ростов-Ярославский',
                        },
                        {
                            id: '314311',
                            name: '231 км',
                        },
                        {
                            id: '314400',
                            name: 'Семибратово',
                        },
                        {
                            id: '314415',
                            name: 'Цибирино',
                        },
                        {
                            id: '314425',
                            name: 'Коромыслово',
                        },
                        {
                            id: '314434',
                            name: 'Кудрявцево',
                        },
                        {
                            id: '314468',
                            name: '259 км',
                        },
                        {
                            id: '314701',
                            name: 'Козьмодемьянск',
                        },
                        {
                            id: '314716',
                            name: 'Река',
                        },
                        {
                            id: '314754',
                            name: '265 км',
                        },
                        {
                            id: '314744',
                            name: '268 км',
                        },
                        {
                            id: '314735',
                            name: '274 км',
                        },
                        {
                            id: '314805',
                            name: 'Полянки',
                        },
                        {
                            id: '310019',
                            name: 'Которосль',
                        },
                        {
                            id: '310109',
                            name: 'Ярославль (Московский вокзал)',
                        },
                        {
                            id: '310005',
                            name: 'Ярославль-Главный',
                        },
                        {
                            id: '310024',
                            name: 'Ярославль (депо)',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Белорусское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '198230',
                            name: 'Москва (Белорусский вокзал)',
                        },
                        {
                            id: '198211',
                            name: 'Беговая',
                        },
                        {
                            id: '198226',
                            name: 'Тестовская',
                        },
                        {
                            id: '181600',
                            name: 'Фили',
                        },
                        {
                            id: '181615',
                            name: 'Славянский Бульвар',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '181704',
                            name: 'Кунцевская',
                        },
                        {
                            id: '181719',
                            name: 'Рабочий Посёлок',
                        },
                        {
                            id: '181723',
                            name: 'Сетунь',
                        },
                        {
                            id: '181808',
                            name: 'Кунцево 2',
                        },
                        {
                            id: '181738',
                            name: 'Немчиновка',
                        },
                        {
                            id: '181827',
                            name: 'Ромашково',
                        },
                        {
                            id: '181742',
                            name: 'Сколково',
                        },
                        {
                            id: '181757',
                            name: 'Баковка',
                        },
                        {
                            id: '181831',
                            name: 'Раздоры',
                        },
                        {
                            id: '182209',
                            name: 'Одинцово',
                        },
                        {
                            id: '181846',
                            name: 'Барвиха',
                        },
                        {
                            id: '181850',
                            name: 'Ильинское',
                        },
                        {
                            id: '182213',
                            name: 'Отрадное',
                        },
                        {
                            id: '182105',
                            name: 'Усово',
                        },
                        {
                            id: '182228',
                            name: 'Пионерская',
                        },
                        {
                            id: '182232',
                            name: 'Перхушково',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '182247',
                            name: 'Здравница',
                        },
                        {
                            id: '182251',
                            name: 'Жаворонки',
                        },
                        {
                            id: '182266',
                            name: 'Дачное',
                        },
                        {
                            id: '182270',
                            name: 'Малые Вязёмы',
                        },
                        {
                            id: '182302',
                            name: 'Голицыно',
                        },
                        {
                            id: '182321',
                            name: 'Захарово',
                        },
                        {
                            id: '182336',
                            name: 'Хлюпино',
                        },
                        {
                            id: '182317',
                            name: 'Сушкинская',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '182340',
                            name: 'Скоротово',
                        },
                        {
                            id: '182509',
                            name: 'Петелино',
                        },
                        {
                            id: '182406',
                            name: 'Звенигород',
                        },
                        {
                            id: '182514',
                            name: 'Часцовская',
                        },
                        {
                            id: '375',
                            name: 'Парк «Патриот»',
                        },
                        {
                            id: '182529',
                            name: 'Портновская',
                        },
                        {
                            id: '376',
                            name: 'Технический центр',
                        },
                        {
                            id: '182603',
                            name: 'Кубинка 1',
                        },
                        {
                            id: '182618',
                            name: 'Чапаевка',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '182622',
                            name: 'Полушкино',
                        },
                        {
                            id: '182637',
                            name: 'Санаторная',
                        },
                        {
                            id: '182904',
                            name: 'Тучково',
                        },
                        {
                            id: '182919',
                            name: 'Театральная',
                        },
                        {
                            id: '182923',
                            name: 'Садовая',
                        },
                        {
                            id: '183004',
                            name: 'Дорохово',
                        },
                        {
                            id: '183019',
                            name: 'Партизанская',
                        },
                        {
                            id: '183023',
                            name: 'Шаликово',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '183112',
                            name: 'Кукаринская',
                        },
                        {
                            id: '183127',
                            name: '109 км',
                        },
                        {
                            id: '183201',
                            name: 'Можайск',
                        },
                        {
                            id: '175807',
                            name: 'Бородино',
                        },
                        {
                            id: '175718',
                            name: 'Колочь',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '175604',
                            name: 'Уваровка',
                        },
                        {
                            id: '175510',
                            name: '144 км',
                        },
                        {
                            id: '313',
                            name: '147 км',
                        },
                        {
                            id: '175525',
                            name: 'Дровнино',
                        },
                        {
                            id: '175421',
                            name: 'Батюшково',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '175417',
                            name: 'Колесники',
                        },
                        {
                            id: '175402',
                            name: 'Гагарин',
                        },
                        {
                            id: '175313',
                            name: 'Василисино',
                        },
                        {
                            id: '175309',
                            name: 'Серго-Ивановская',
                        },
                        {
                            id: '314',
                            name: '205 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '175135',
                            name: 'Туманово',
                        },
                        {
                            id: '315',
                            name: '215 км',
                        },
                        {
                            id: '175116',
                            name: '218 км',
                        },
                        {
                            id: '175120',
                            name: 'Мещёрская',
                        },
                        {
                            id: '174332',
                            name: 'Подъёлки',
                        },
                        {
                            id: '174325',
                            name: 'Комягино',
                        },
                        {
                            id: '174310',
                            name: 'Зубарёвка',
                        },
                        {
                            id: '174306',
                            name: 'Вязьма',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Курское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '191655',
                            name: 'Савёловская',
                        },
                        {
                            id: '191640',
                            name: 'Станколит',
                        },
                        {
                            id: '191621',
                            name: 'Рижская (МЦД-2)',
                        },
                        {
                            id: '191617',
                            name: 'Москва-Каланчёвская',
                        },
                        {
                            id: '191602',
                            name: 'Москва (Курский вокзал)',
                        },
                        {
                            id: '191509',
                            name: 'Москва-Товарная',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '190031',
                            name: 'Калитники',
                        },
                        {
                            id: '342',
                            name: 'Новохохловская',
                        },
                        {
                            id: '190027',
                            name: 'Текстильщики',
                        },
                        {
                            id: '190258',
                            name: 'Печатники',
                        },
                        {
                            id: '190012',
                            name: 'Люблино',
                        },
                        {
                            id: '191458',
                            name: 'Депо',
                        },
                        {
                            id: '191443',
                            name: 'Перерва',
                        },
                        {
                            id: '191462',
                            name: 'Курьяново',
                        },
                        {
                            id: '191439',
                            name: 'Москворечье',
                        },
                        {
                            id: '191424',
                            name: 'Царицыно',
                        },
                        {
                            id: '191419',
                            name: 'Покровское',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '191405',
                            name: 'Красный Строитель',
                        },
                        {
                            id: '191212',
                            name: 'Битца',
                        },
                        {
                            id: '191208',
                            name: 'Бутово',
                        },
                        {
                            id: '191104',
                            name: 'Щербинка',
                        },
                        {
                            id: '344',
                            name: 'Остафьево',
                        },
                        {
                            id: '191000',
                            name: 'Силикатная',
                        },
                        {
                            id: '190900',
                            name: 'Подольск',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '190826',
                            name: 'Кутузовская',
                        },
                        {
                            id: '190811',
                            name: 'Весенняя',
                        },
                        {
                            id: '190807',
                            name: 'Гривно',
                        },
                        {
                            id: '190629',
                            name: 'Львовская',
                        },
                        {
                            id: '190614',
                            name: 'Молоди',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '190609',
                            name: 'Столбовая',
                        },
                        {
                            id: '190525',
                            name: '66 км',
                        },
                        {
                            id: '190510',
                            name: 'Чепелёво',
                        },
                        {
                            id: '190506',
                            name: 'Чехов',
                        },
                        {
                            id: '190417',
                            name: 'Луч',
                        },
                        {
                            id: '190402',
                            name: 'Шарапова Охота',
                        },
                        {
                            id: '190224',
                            name: '92 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '190213',
                            name: 'Авангард',
                        },
                        {
                            id: '190205',
                            name: 'Серпухов',
                        },
                        {
                            id: '190101',
                            name: 'Ока',
                        },
                        {
                            id: '210930',
                            name: '107 км',
                        },
                        {
                            id: '210926',
                            name: 'Приокская',
                        },
                        {
                            id: '210911',
                            name: 'Романовские дачи',
                        },
                        {
                            id: '210907',
                            name: 'Тарусская',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '210818',
                            name: '132 км',
                        },
                        {
                            id: '210790',
                            name: 'Пахомово',
                        },
                        {
                            id: '210729',
                            name: 'Шульгино',
                        },
                        {
                            id: '210714',
                            name: '153 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '210708',
                            name: 'Ясногорск',
                        },
                        {
                            id: '210625',
                            name: 'Шеметово',
                        },
                        {
                            id: '210610',
                            name: 'Бараново',
                        },
                        {
                            id: '210606',
                            name: 'Ревякино',
                        },
                        {
                            id: '210517',
                            name: 'Байдики',
                        },
                        {
                            id: '210502',
                            name: 'Хомяково',
                        },
                        {
                            id: '210216',
                            name: '191 км',
                        },
                        {
                            id: '210201',
                            name: 'Тула (Московский вокзал)',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Павелецкое',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '193519',
                            name: 'Москва (Павелецкий вокзал)',
                        },
                        {
                            id: '193504',
                            name: 'Дербеневская',
                        },
                        {
                            id: '193424',
                            name: 'Тульская',
                        },
                        {
                            id: '343',
                            name: 'Верхние Котлы',
                        },
                        {
                            id: '193415',
                            name: 'Нагатинская',
                        },
                        {
                            id: '193400',
                            name: 'Варшавская',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '193203',
                            name: 'Чертаново',
                        },
                        {
                            id: '193307',
                            name: 'Бирюлёво-Тов.',
                        },
                        {
                            id: '192959',
                            name: 'Бирюлёво-Пасс.',
                        },
                        {
                            id: '192944',
                            name: 'Булатниково',
                        },
                        {
                            id: '192933',
                            name: 'Расторгуево',
                        },
                        {
                            id: '192925',
                            name: 'Калинина',
                        },
                        {
                            id: '192910',
                            name: 'Ленинская',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '307',
                            name: '32 км',
                        },
                        {
                            id: '192906',
                            name: 'Домодедово',
                        },
                        {
                            id: '193006',
                            name: 'Авиационная',
                        },
                        {
                            id: '193105',
                            name: 'Космос',
                        },
                        {
                            id: '192821',
                            name: 'Взлётная',
                        },
                        {
                            id: '192817',
                            name: 'Востряково',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '192802',
                            name: 'Белые Столбы',
                        },
                        {
                            id: '192713',
                            name: '52 км',
                        },
                        {
                            id: '192709',
                            name: 'Барыбино',
                        },
                        {
                            id: '192728',
                            name: 'Вельяминово',
                        },
                        {
                            id: '192732',
                            name: 'Привалово',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '192605',
                            name: 'Михнево',
                        },
                        {
                            id: '192614',
                            name: '328 км',
                        },
                        {
                            id: '192624',
                            name: '332 км',
                        },
                        {
                            id: '192215',
                            name: 'Шугарово',
                        },
                        {
                            id: '192639',
                            name: 'Малино',
                        },
                        {
                            id: '308',
                            name: '85 км',
                        },
                        {
                            id: '192643',
                            name: '341 км',
                        },
                        {
                            id: '192200',
                            name: 'Жилёво',
                        },
                        {
                            id: '192380',
                            name: 'Яганово',
                        },
                        {
                            id: '192111',
                            name: 'Ситенка',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '192107',
                            name: 'Ступино',
                        },
                        {
                            id: '192003',
                            name: 'Акри',
                        },
                        {
                            id: '191829',
                            name: 'Белопесоцкий',
                        },
                        {
                            id: '191804',
                            name: 'Кашира',
                        },
                        {
                            id: '191814',
                            name: 'Тесна',
                        },
                        {
                            id: '229706',
                            name: 'Ожерелье',
                        },
                        {
                            id: '309',
                            name: '121 км',
                        },
                        {
                            id: '226854',
                            name: '123 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '226840',
                            name: '126 км',
                        },
                        {
                            id: '229602',
                            name: 'Пурлово',
                        },
                        {
                            id: '226835',
                            name: 'Пчеловодное',
                        },
                        {
                            id: '330',
                            name: '131 км',
                        },
                        {
                            id: '229513',
                            name: 'Топканово',
                        },
                        {
                            id: '310',
                            name: '137 км',
                        },
                        {
                            id: '226820',
                            name: '137 км',
                        },
                        {
                            id: '229509',
                            name: 'Богатищево',
                        },
                        {
                            id: '226816',
                            name: '142 км',
                        },
                        {
                            id: '228921',
                            name: '146 км',
                        },
                        {
                            id: '226801',
                            name: 'Мордвес',
                        },
                        {
                            id: '228915',
                            name: 'Коровино',
                        },
                        {
                            id: '311',
                            name: '152 км',
                        },
                        {
                            id: '226746',
                            name: '156 км',
                        },
                        {
                            id: '228900',
                            name: 'Узуново',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '226731',
                            name: 'Настасьино',
                        },
                        {
                            id: '360',
                            name: '7 км',
                        },
                        {
                            id: '226727',
                            name: '163 км',
                        },
                        {
                            id: '226712',
                            name: '166 км',
                        },
                        {
                            id: '229142',
                            name: 'Клёмово',
                        },
                        {
                            id: '228826',
                            name: '162 км',
                        },
                        {
                            id: '331',
                            name: '12 км',
                        },
                        {
                            id: '405',
                            name: '168 км',
                        },
                        {
                            id: '228811',
                            name: 'Дудино',
                        },
                        {
                            id: '229138',
                            name: 'Лошатовка',
                        },
                        {
                            id: '332',
                            name: '18 км',
                        },
                        {
                            id: '226708',
                            name: 'Венёв',
                        },
                        {
                            id: '228807',
                            name: 'Серебряные Пруды',
                        },
                        {
                            id: '333',
                            name: '21 км',
                        },
                        {
                            id: '334',
                            name: '24 км',
                        },
                        {
                            id: '226638',
                            name: '180 км',
                        },
                        {
                            id: '228718',
                            name: 'Кораблёвка',
                        },
                        {
                            id: '229123',
                            name: 'Жоково',
                        },
                        {
                            id: '335',
                            name: '28 км',
                        },
                        {
                            id: '226623',
                            name: 'Ольховка',
                        },
                        {
                            id: '229119',
                            name: 'Верейкино',
                        },
                        {
                            id: '228722',
                            name: 'Треполье',
                        },
                        {
                            id: '336',
                            name: '35 км',
                        },
                        {
                            id: '226619',
                            name: '193 км',
                        },
                        {
                            id: '229104',
                            name: 'Макеево-московское',
                        },
                        {
                            id: '228602',
                            name: 'Виленки',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '226604',
                            name: 'Грицово',
                        },
                        {
                            id: '337',
                            name: '47 км',
                        },
                        {
                            id: '228417',
                            name: 'Донцы',
                        },
                        {
                            id: '228949',
                            name: 'Латыгоры',
                        },
                        {
                            id: '338',
                            name: 'Костёнково',
                        },
                        {
                            id: '226303',
                            name: 'Новомосковск-2',
                        },
                        {
                            id: '228402',
                            name: 'Михайлов',
                        },
                        {
                            id: '339',
                            name: 'Козловка',
                        },
                        {
                            id: '225902',
                            name: 'Ключёвка',
                        },
                        {
                            id: '228934',
                            name: '59 км',
                        },
                        {
                            id: '226500',
                            name: 'Маклец',
                        },
                        {
                            id: '340',
                            name: 'Житово',
                        },
                        {
                            id: '225809',
                            name: 'Урванка',
                        },
                        {
                            id: '228328',
                            name: 'Бояринцево',
                        },
                        {
                            id: '225710',
                            name: 'Новомосковск-1',
                        },
                        {
                            id: '341',
                            name: 'Валищево',
                        },
                        {
                            id: '225705',
                            name: 'Сборная-Угольная',
                        },
                        {
                            id: '225616',
                            name: '23 км',
                        },
                        {
                            id: '225601',
                            name: 'Бобрик-Донской',
                        },
                        {
                            id: '228313',
                            name: 'Голдино',
                        },
                        {
                            id: '224219',
                            name: 'Руднево',
                        },
                        {
                            id: '224204',
                            name: 'Узловая-1',
                        },
                        {
                            id: '406',
                            name: 'Депо',
                        },
                        {
                            id: '228309',
                            name: 'Лужковская',
                        },
                        {
                            id: '224308',
                            name: 'Узловая-2',
                        },
                        {
                            id: '228258',
                            name: 'Волшута',
                        },
                        {
                            id: '228243',
                            name: 'Катино',
                        },
                        {
                            id: '228239',
                            name: 'Мшанка',
                        },
                        {
                            id: '228224',
                            name: 'Кремлево',
                        },
                        {
                            id: '228205',
                            name: 'Павелец-1-Тульский',
                        },
                        {
                            id: '603023',
                            name: '259 км',
                        },
                        {
                            id: '603004',
                            name: 'Топиллы',
                        },
                        {
                            id: '603019',
                            name: '269 км',
                        },
                        {
                            id: '603108',
                            name: 'Спасское',
                        },
                        {
                            id: '603201',
                            name: 'Милославское',
                        },
                        {
                            id: '603216',
                            name: 'Гротовский',
                        },
                        {
                            id: '603220',
                            name: '294 км',
                        },
                        {
                            id: '603235',
                            name: 'Урусово',
                        },
                        {
                            id: '603247',
                            name: '302 км',
                        },
                        {
                            id: '603339',
                            name: 'Троекурово',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Казанское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '194013',
                            name: 'Москва (Казанский вокзал)',
                        },
                        {
                            id: '194028',
                            name: 'Электрозаводская',
                        },
                        {
                            id: '194032',
                            name: 'Сортировочная',
                        },
                        {
                            id: '193913',
                            name: 'Авиамоторная',
                        },
                        {
                            id: '193928',
                            name: 'Андроновка (Казанское направление)',
                        },
                        {
                            id: '193805',
                            name: 'Перово',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '193810',
                            name: 'Плющево',
                        },
                        {
                            id: '193824',
                            name: 'Вешняки',
                        },
                        {
                            id: '193839',
                            name: 'Выхино',
                        },
                        {
                            id: '193843',
                            name: 'Косино',
                        },
                        {
                            id: '193858',
                            name: 'Ухтомская',
                        },
                        {
                            id: '194206',
                            name: 'Люберцы 1',
                        },
                        {
                            id: '194210',
                            name: 'Панки',
                        },
                        {
                            id: '194300',
                            name: 'Люберцы 2',
                        },
                        {
                            id: '194225',
                            name: 'Томилино',
                        },
                        {
                            id: '194234',
                            name: 'Красково',
                        },
                        {
                            id: '194244',
                            name: 'Малаховка',
                        },
                        {
                            id: '194314',
                            name: 'Коренёво',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '194259',
                            name: 'Удельная',
                        },
                        {
                            id: '194600',
                            name: 'Овражки',
                        },
                        {
                            id: '194808',
                            name: 'Быково',
                        },
                        {
                            id: '194615',
                            name: 'Родники',
                        },
                        {
                            id: '194812',
                            name: 'Ильинская',
                        },
                        {
                            id: '194620',
                            name: 'Вялки',
                        },
                        {
                            id: '194827',
                            name: 'Отдых',
                        },
                        {
                            id: '1072',
                            name: 'Юность (ММДЖД)',
                        },
                        {
                            id: '1071',
                            name: 'Школьная (ММДЖД)',
                        },
                        {
                            id: '194634',
                            name: 'Хрипань',
                        },
                        {
                            id: '1070',
                            name: 'Пионерская (ММДЖД)',
                        },
                        {
                            id: '194831',
                            name: 'Кратово',
                        },
                        {
                            id: '194649',
                            name: '41 км',
                        },
                        {
                            id: '194846',
                            name: 'Есенинская',
                        },
                        {
                            id: '194850',
                            name: 'Фабричная',
                        },
                        {
                            id: '194901',
                            name: 'Раменское',
                        },
                        {
                            id: '194916',
                            name: 'Ипподром',
                        },
                        {
                            id: '194653',
                            name: 'Донино',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '194668',
                            name: '49 км',
                        },
                        {
                            id: '194920',
                            name: 'Совхоз',
                        },
                        {
                            id: '194672',
                            name: '52 км',
                        },
                        {
                            id: '194687',
                            name: 'Григорово',
                        },
                        {
                            id: '194935',
                            name: 'Загорново',
                        },
                        {
                            id: '194776',
                            name: '55 км',
                        },
                        {
                            id: '195001',
                            name: 'Бронницы',
                        },
                        {
                            id: '194704',
                            name: 'Гжель',
                        },
                        {
                            id: '195035',
                            name: 'Радуга',
                        },
                        {
                            id: '195016',
                            name: '63 км',
                        },
                        {
                            id: '194719',
                            name: 'Игнатьево',
                        },
                        {
                            id: '195020',
                            name: 'Белоозёрская',
                        },
                        {
                            id: '194723',
                            name: 'Кузяево',
                        },
                        {
                            id: '195105',
                            name: 'Фаустово',
                        },
                        {
                            id: '194738',
                            name: 'Шевлягино',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '195110',
                            name: 'Золотово',
                        },
                        {
                            id: '195209',
                            name: 'Виноградово',
                        },
                        {
                            id: '194742',
                            name: '73 км',
                        },
                        {
                            id: '194757',
                            name: 'Анциферово',
                        },
                        {
                            id: '195213',
                            name: 'Конобеево',
                        },
                        {
                            id: '194761',
                            name: 'Подосинки',
                        },
                        {
                            id: '300',
                            name: 'Трофимово (84 км)',
                        },
                        {
                            id: '231829',
                            name: 'Нерская',
                        },
                        {
                            id: '195228',
                            name: '88 км',
                        },
                        {
                            id: '233608',
                            name: 'Воскресенск',
                        },
                        {
                            id: '231805',
                            name: 'Куровская',
                        },
                        {
                            id: '231833',
                            name: '90 км',
                        },
                        {
                            id: '233612',
                            name: 'Шиферная',
                        },
                        {
                            id: '233627',
                            name: 'Москворецкая',
                        },
                        {
                            id: '233364',
                            name: 'Ильинский Погост',
                        },
                        {
                            id: '231814',
                            name: '95 км',
                        },
                        {
                            id: '233631',
                            name: 'Цемгигант',
                        },
                        {
                            id: '233311',
                            name: '32 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '233701',
                            name: 'Пески',
                        },
                        {
                            id: '231903',
                            name: 'Авсюнино',
                        },
                        {
                            id: '233716',
                            name: 'Конев Бор',
                        },
                        {
                            id: '233326',
                            name: 'Егорьевск',
                        },
                        {
                            id: '231918',
                            name: 'Заполицы',
                        },
                        {
                            id: '233720',
                            name: 'Хорошово',
                        },
                        {
                            id: '233735',
                            name: '113 км',
                        },
                        {
                            id: '231922',
                            name: 'Запутная',
                        },
                        {
                            id: '233744',
                            name: 'Коломна',
                        },
                        {
                            id: '233805',
                            name: 'Голутвин',
                        },
                        {
                            id: '233810',
                            name: 'Бачманово',
                        },
                        {
                            id: '233824',
                            name: '6 км',
                        },
                        {
                            id: '232003',
                            name: 'Шатурторф',
                        },
                        {
                            id: '234102',
                            name: 'Щурово',
                        },
                        {
                            id: '233839',
                            name: 'Сычёво',
                        },
                        {
                            id: '233843',
                            name: 'Лысцовская',
                        },
                        {
                            id: '233858',
                            name: 'Семёновский',
                        },
                        {
                            id: '234121',
                            name: 'Чёрная',
                        },
                        {
                            id: '232107',
                            name: 'Шатура',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '233862',
                            name: '18 км',
                        },
                        {
                            id: '232111',
                            name: 'Ботино',
                        },
                        {
                            id: '233909',
                            name: 'Карасёво',
                        },
                        {
                            id: '233913',
                            name: 'Кудрявцево',
                        },
                        {
                            id: '234206',
                            name: 'Луховицы',
                        },
                        {
                            id: '232304',
                            name: 'Кривандино',
                        },
                        {
                            id: '233928',
                            name: 'Даниловская',
                        },
                        {
                            id: '234210',
                            name: '142 км',
                        },
                        {
                            id: '233932',
                            name: '30 км',
                        },
                        {
                            id: '232319',
                            name: 'Туголесье',
                        },
                        {
                            id: '234225',
                            name: 'Подлипки',
                        },
                        {
                            id: '232431',
                            name: 'Осаново',
                        },
                        {
                            id: '233006',
                            name: 'Воймежный',
                        },
                        {
                            id: '371',
                            name: '15 км',
                        },
                        {
                            id: '233947',
                            name: '38 км',
                        },
                        {
                            id: '234403',
                            name: 'Фруктовая',
                        },
                        {
                            id: '234009',
                            name: 'Озёры',
                        },
                        {
                            id: '232412',
                            name: 'Пожога',
                        },
                        {
                            id: '233106',
                            name: 'Черусти',
                        },
                        {
                            id: '234507',
                            name: 'Алпатьево',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '234511',
                            name: 'Слёмы',
                        },
                        {
                            id: '232427',
                            name: '29 км',
                        },
                        {
                            id: '242720',
                            name: 'Струя',
                        },
                        {
                            id: '234620',
                            name: 'Дивово',
                        },
                        {
                            id: '242715',
                            name: 'Тасин',
                        },
                        {
                            id: '232501',
                            name: 'Бармино',
                        },
                        {
                            id: '234615',
                            name: 'Истодники',
                        },
                        {
                            id: '232605',
                            name: 'Сазоново',
                        },
                        {
                            id: '242823',
                            name: 'Ильичёв',
                        },
                        {
                            id: '301',
                            name: '179 км',
                        },
                        {
                            id: '220006',
                            name: 'Рыбное',
                        },
                        {
                            id: '232615',
                            name: '47 км',
                        },
                        {
                            id: '220010',
                            name: 'Ходынино',
                        },
                        {
                            id: '302',
                            name: 'Депо',
                        },
                        {
                            id: '220032',
                            name: 'Рязань-Сорт. (Жилые дома)',
                        },
                        {
                            id: '242804',
                            name: 'Торфопродукт',
                        },
                        {
                            id: '220044',
                            name: '187 км',
                        },
                        {
                            id: '232709',
                            name: 'Рязановка',
                        },
                        {
                            id: '303',
                            name: '189 км',
                        },
                        {
                            id: '220059',
                            name: 'Недостоево',
                        },
                        {
                            id: '242819',
                            name: 'Мильцево',
                        },
                        {
                            id: '220203',
                            name: 'Дягилево',
                        },
                        {
                            id: '220218',
                            name: 'Лагерный',
                        },
                        {
                            id: '222800',
                            name: 'Рязань-2',
                        },
                        {
                            id: '242908',
                            name: 'Нечаевская',
                        },
                        {
                            id: '220307',
                            name: 'Рязань-1',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '304',
                            name: 'Депо (Вековка-западная)',
                        },
                        {
                            id: '241604',
                            name: 'Вековка',
                        },
                    ],
                },
            ],
        },
        {
            name: 'МЦК: Московское центральное кольцо',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '199208',
                            name: 'Локомотив (Черкизово)',
                        },
                        {
                            id: '495',
                            name: 'Бульвар Рокоссовского',
                        },
                        {
                            id: '470',
                            name: 'Измайлово',
                        },
                        {
                            id: '471',
                            name: 'Соколиная Гора',
                        },
                        {
                            id: '199106',
                            name: 'Белокаменная',
                        },
                        {
                            id: '472',
                            name: 'Шоссе Энтузиастов',
                        },
                        {
                            id: '199318',
                            name: 'Андроновка (МЦК)',
                        },
                        {
                            id: '494',
                            name: 'Ростокино (МЦК)',
                        },
                        {
                            id: '473',
                            name: 'Нижегородская (МЦК)',
                        },
                        {
                            id: '493',
                            name: 'Ботанический сад',
                        },
                        {
                            id: '474',
                            name: 'Новохохловская, МЦК',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '475',
                            name: 'Угрешская',
                        },
                        {
                            id: '476',
                            name: 'Дубровка',
                        },
                        {
                            id: '477',
                            name: 'Автозаводская',
                        },
                        {
                            id: '478',
                            name: 'ЗИЛ, МЦК',
                        },
                        {
                            id: '479',
                            name: 'Верхние Котлы, МЦК',
                        },
                        {
                            id: '480',
                            name: 'Крымская',
                        },
                        {
                            id: '481',
                            name: 'Площадь Гагарина',
                        },
                        {
                            id: '482',
                            name: 'Лужники',
                        },
                        {
                            id: '198423',
                            name: 'Кутузовская, МЦК',
                        },
                        {
                            id: '483',
                            name: 'Деловой центр',
                        },
                        {
                            id: '484',
                            name: 'Шелепиха',
                        },
                        {
                            id: '485',
                            name: 'Хорошёво',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '486',
                            name: 'Зорге',
                        },
                        {
                            id: '487',
                            name: 'Панфиловская',
                        },
                        {
                            id: '488',
                            name: 'Стрешнево (МЦК)',
                        },
                        {
                            id: '489',
                            name: 'Балтийская',
                        },
                        {
                            id: '490',
                            name: 'Коптево',
                        },
                        {
                            id: '491',
                            name: 'Лихоборы (МЦК)',
                        },
                        {
                            id: '492',
                            name: 'Окружная, МЦК',
                        },
                        {
                            id: '198902',
                            name: 'Владыкино',
                        },
                    ],
                },
            ],
        },
        {
            name: 'МЦД-2',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '190900',
                            name: 'Подольск',
                        },
                        {
                            id: '191000',
                            name: 'Силикатная',
                        },
                        {
                            id: '344',
                            name: 'Остафьево',
                        },
                        {
                            id: '191104',
                            name: 'Щербинка',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '191208',
                            name: 'Бутово',
                        },
                        {
                            id: '191212',
                            name: 'Битца',
                        },
                        {
                            id: '191405',
                            name: 'Красный Строитель',
                        },
                        {
                            id: '191419',
                            name: 'Покровское',
                        },
                        {
                            id: '191424',
                            name: 'Царицыно',
                        },
                        {
                            id: '191439',
                            name: 'Москворечье',
                        },
                        {
                            id: '191462',
                            name: 'Курьяново',
                        },
                        {
                            id: '191443',
                            name: 'Перерва',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '191458',
                            name: 'Депо',
                        },
                        {
                            id: '190012',
                            name: 'Люблино',
                        },
                        {
                            id: '190258',
                            name: 'Печатники',
                        },
                        {
                            id: '190027',
                            name: 'Текстильщики',
                        },
                        {
                            id: '342',
                            name: 'Новохохловская',
                        },
                        {
                            id: '190031',
                            name: 'Калитники',
                        },
                        {
                            id: '191509',
                            name: 'Москва-Товарная',
                        },
                        {
                            id: '191602',
                            name: 'Москва (Курский вокзал)',
                        },
                        {
                            id: '191617',
                            name: 'Москва-Каланчёвская',
                        },
                        {
                            id: '191621',
                            name: 'Рижская (МЦД-2)',
                        },
                        {
                            id: '196112',
                            name: 'Дмитровская',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '196201',
                            name: 'Гражданская',
                        },
                        {
                            id: '196216',
                            name: 'Красный Балтиец',
                        },
                        {
                            id: '196220',
                            name: 'Стрешнево (МЦД-2)',
                        },
                        {
                            id: '196316',
                            name: 'Покровско-Стрешнево',
                        },
                        {
                            id: '196339',
                            name: 'Щукинская',
                        },
                        {
                            id: '196409',
                            name: 'Тушинская (Тушино)',
                        },
                        {
                            id: '196413',
                            name: 'Трикотажная',
                        },
                        {
                            id: '346',
                            name: 'Волоколамская',
                        },
                        {
                            id: '347',
                            name: 'Пенягино',
                        },
                        {
                            id: '196502',
                            name: 'Павшино',
                        },
                        {
                            id: '196517',
                            name: 'Красногорская',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '196521',
                            name: 'Опалиха',
                        },
                        {
                            id: '196536',
                            name: 'Аникеевка',
                        },
                        {
                            id: '196606',
                            name: 'Нахабино',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Ленинградское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '60073',
                            name: 'Москва (Ленинградский вокзал)',
                        },
                        {
                            id: '60069',
                            name: 'Рижская (Ленинградское направление)',
                        },
                        {
                            id: '60054',
                            name: 'Останкино',
                        },
                        {
                            id: '60048',
                            name: 'Петровско-Разумовская',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '60035',
                            name: 'Лихоборы (бывш. НАТИ)',
                        },
                        {
                            id: '60020',
                            name: 'Моссельмаш',
                        },
                        {
                            id: '60001',
                            name: 'Грачёвская (бывш. Ховрино)',
                        },
                        {
                            id: '60088',
                            name: 'Ховрино',
                        },
                        {
                            id: '60016',
                            name: 'Левобережная',
                        },
                        {
                            id: '60302',
                            name: 'Химки',
                        },
                        {
                            id: '60317',
                            name: 'Молжаниново (бывш. Планерная)',
                        },
                        {
                            id: '60321',
                            name: 'Новоподрезково',
                        },
                        {
                            id: '60336',
                            name: 'Подрезково',
                        },
                        {
                            id: '60406',
                            name: 'Сходня',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '60410',
                            name: 'Фирсановская',
                        },
                        {
                            id: '60425',
                            name: 'Малино',
                        },
                        {
                            id: '60500',
                            name: 'Крюково',
                        },
                        {
                            id: '60618',
                            name: 'Алабушево',
                        },
                        {
                            id: '60622',
                            name: 'Радищево',
                        },
                        {
                            id: '60637',
                            name: 'Поваровка',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '60707',
                            name: 'Поварово-1',
                        },
                        {
                            id: '60711',
                            name: 'Берёзки-Дачные',
                        },
                        {
                            id: '60800',
                            name: 'Подсолнечная',
                        },
                        {
                            id: '60815',
                            name: 'Сенеж',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '60829',
                            name: 'Головково',
                        },
                        {
                            id: '60834',
                            name: 'Покровка',
                        },
                        {
                            id: '60849',
                            name: 'Фроловское',
                        },
                        {
                            id: '60853',
                            name: 'Стреглово',
                        },
                        {
                            id: '60904',
                            name: 'Клин',
                        },
                        {
                            id: '60919',
                            name: 'Ямуга',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '61108',
                            name: 'Решетниково',
                        },
                        {
                            id: '61127',
                            name: 'Путепроводная',
                        },
                        {
                            id: '61112',
                            name: 'Черничная',
                        },
                        {
                            id: '61305',
                            name: 'Завидово',
                        },
                        {
                            id: '61131',
                            name: 'Конаковский Мох',
                        },
                        {
                            id: '61146',
                            name: 'Донховка',
                        },
                        {
                            id: '61310',
                            name: 'Московское Море',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '61409',
                            name: 'Редкино',
                        },
                        {
                            id: '61201',
                            name: 'Конаково ГРЭС',
                        },
                        {
                            id: '61413',
                            name: 'Межево',
                        },
                        {
                            id: '61428',
                            name: 'Кузьминка',
                        },
                        {
                            id: '61432',
                            name: 'Чуприяновка',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '61447',
                            name: 'Лазурная',
                        },
                        {
                            id: '61502',
                            name: 'Тверь',
                        },
                        {
                            id: '61517',
                            name: 'Пролетарская',
                        },
                        {
                            id: '61659',
                            name: 'Дорошиха',
                        },
                        {
                            id: '61610',
                            name: 'Брянцево',
                        },
                        {
                            id: '61625',
                            name: 'Санаторий',
                        },
                        {
                            id: '61639',
                            name: 'Тверца',
                        },
                        {
                            id: '61644',
                            name: 'Кулицкая',
                        },
                        {
                            id: '61911',
                            name: 'Кулицкий Мох',
                        },
                        {
                            id: '61926',
                            name: 'Трубино',
                        },
                        {
                            id: '62026',
                            name: 'Крючково',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '62011',
                            name: 'Поршинец',
                        },
                        {
                            id: '62100',
                            name: 'Лихославль',
                        },
                        {
                            id: '62115',
                            name: 'Виноколы',
                        },
                        {
                            id: '602',
                            name: '7 км',
                        },
                        {
                            id: '62120',
                            name: 'Шлюз',
                        },
                        {
                            id: '62134',
                            name: 'Локотцы',
                        },
                        {
                            id: '601',
                            name: 'Ильинская (9 км)',
                        },
                        {
                            id: '64214',
                            name: 'Лазари',
                        },
                        {
                            id: '62149',
                            name: 'Барановка',
                        },
                        {
                            id: '64201',
                            name: 'Терешкино',
                        },
                        {
                            id: '62153',
                            name: 'Муташелиха',
                        },
                        {
                            id: '62308',
                            name: 'Калашниково',
                        },
                        {
                            id: '64125',
                            name: 'Колодези',
                        },
                        {
                            id: '600',
                            name: '30 км (Сокол)',
                        },
                        {
                            id: '62312',
                            name: 'Бухаловский переезд',
                        },
                        {
                            id: '64106',
                            name: 'Торжок',
                        },
                        {
                            id: '62327',
                            name: 'Левошинка',
                        },
                        {
                            id: '62401',
                            name: 'Спирово',
                        },
                        {
                            id: '62416',
                            name: 'Любинка',
                        },
                        {
                            id: '62420',
                            name: 'Индустрия',
                        },
                        {
                            id: '62505',
                            name: 'Осеченка',
                        },
                        {
                            id: '62515',
                            name: 'Терелесовская',
                        },
                        {
                            id: '62609',
                            name: 'Елизаровка',
                        },
                        {
                            id: '62702',
                            name: 'Вышний Волочёк',
                        },
                        {
                            id: '62721',
                            name: 'Цна',
                        },
                        {
                            id: '62806',
                            name: 'Леонтьево',
                        },
                        {
                            id: '62810',
                            name: 'Почвино',
                        },
                        {
                            id: '62900',
                            name: 'Академическая',
                        },
                        {
                            id: '52857',
                            name: 'Соболево',
                        },
                        {
                            id: '52838',
                            name: 'Костромцовская',
                        },
                        {
                            id: '52823',
                            name: 'Бочановка',
                        },
                        {
                            id: '52819',
                            name: 'Петерсоновка (331 км)',
                        },
                        {
                            id: '52804',
                            name: 'Бушевец',
                        },
                        {
                            id: '603',
                            name: 'Виноградовская (322 км)',
                        },
                        {
                            id: '52842',
                            name: 'Веерное депо',
                        },
                        {
                            id: '50009',
                            name: 'Бологое',
                        },
                    ],
                },
            ],
        },
        {
            name: 'Савёловское',
            stationGroups: [
                {
                    minDistanceKm: 0,
                    maxDistanceKm: 10,
                    stations: [
                        {
                            id: '196004',
                            name: 'Москва (Савёловский вокзал)',
                        },
                        {
                            id: '196019',
                            name: 'Тимирязевская',
                        },
                        {
                            id: '196023',
                            name: 'Окружная',
                        },
                        {
                            id: '196038',
                            name: 'Дегунино',
                        },
                        {
                            id: '195800',
                            name: 'Бескудниково',
                        },
                    ],
                },
                {
                    minDistanceKm: 10,
                    maxDistanceKm: 30,
                    stations: [
                        {
                            id: '195815',
                            name: 'Лианозово',
                        },
                        {
                            id: '237806',
                            name: 'Марк',
                        },
                        {
                            id: '237810',
                            name: 'Новодачная',
                        },
                        {
                            id: '237825',
                            name: 'Долгопрудная',
                        },
                        {
                            id: '237831',
                            name: 'Водники',
                        },
                        {
                            id: '237844',
                            name: 'Хлебниково',
                        },
                        {
                            id: '237859',
                            name: 'Шереметьевская',
                        },
                        {
                            id: '237908',
                            name: 'Лобня',
                        },
                        {
                            id: '237914',
                            name: 'Депо',
                        },
                        {
                            id: '237878',
                            name: 'Аэропорт Шереметьево - Северный терминал (B, C)',
                        },
                        {
                            id: '321',
                            name: 'Аэропорт  Шереметьево - Юг (D, E, F)',
                        },
                        {
                            id: '237929',
                            name: 'Луговая',
                        },
                    ],
                },
                {
                    minDistanceKm: 30,
                    maxDistanceKm: 50,
                    stations: [
                        {
                            id: '237933',
                            name: 'Некрасовская',
                        },
                        {
                            id: '238003',
                            name: 'Катуар',
                        },
                        {
                            id: '238014',
                            name: 'Трудовая',
                        },
                        {
                            id: '238103',
                            name: 'Икша',
                        },
                        {
                            id: '238118',
                            name: 'Морозки',
                        },
                    ],
                },
                {
                    minDistanceKm: 50,
                    maxDistanceKm: 70,
                    stations: [
                        {
                            id: '238122',
                            name: 'Турист',
                        },
                        {
                            id: '238300',
                            name: 'Яхрома',
                        },
                        {
                            id: '238322',
                            name: 'Иванцево',
                        },
                        {
                            id: '238404',
                            name: 'Дмитров',
                        },
                        {
                            id: '320',
                            name: '80 км',
                        },
                        {
                            id: '238349',
                            name: 'Драчёво',
                        },
                        {
                            id: '238508',
                            name: 'Каналстрой',
                        },
                        {
                            id: '238353',
                            name: '74 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 70,
                    maxDistanceKm: 100,
                    stations: [
                        {
                            id: '238368',
                            name: '71 км',
                        },
                        {
                            id: '238512',
                            name: 'Имени Барсученко',
                        },
                        {
                            id: '238372',
                            name: '68 км',
                        },
                        {
                            id: '238601',
                            name: 'Орудьево',
                        },
                        {
                            id: '236409',
                            name: 'Костино',
                        },
                        {
                            id: '236413',
                            name: '62 км',
                        },
                        {
                            id: '238902',
                            name: 'Соревнование',
                        },
                        {
                            id: '236502',
                            name: 'Жёлтиково',
                        },
                        {
                            id: '238917',
                            name: 'Запрудня',
                        },
                        {
                            id: '238809',
                            name: 'Вербилки',
                        },
                    ],
                },
                {
                    minDistanceKm: 100,
                    maxDistanceKm: 130,
                    stations: [
                        {
                            id: '238813',
                            name: '94 км',
                        },
                        {
                            id: '238921',
                            name: 'Темпы',
                        },
                        {
                            id: '238936',
                            name: 'Мельдино',
                        },
                        {
                            id: '238828',
                            name: 'Власово',
                        },
                        {
                            id: '238955',
                            name: '119 км',
                        },
                        {
                            id: '238940',
                            name: 'Карманово',
                        },
                        {
                            id: '239002',
                            name: 'Большая Волга',
                        },
                        {
                            id: '239106',
                            name: 'Талдом',
                        },
                        {
                            id: '239017',
                            name: 'Дубна',
                        },
                        {
                            id: '239125',
                            name: 'Лебзино',
                        },
                        {
                            id: '319',
                            name: '124 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 130,
                    maxDistanceKm: 160,
                    stations: [
                        {
                            id: '50300',
                            name: 'Савёлово',
                        },
                        {
                            id: '50315',
                            name: '139 км',
                        },
                        {
                            id: '50404',
                            name: 'Белый Городок',
                        },
                        {
                            id: '611',
                            name: '151 км',
                        },
                        {
                            id: '50419',
                            name: 'Стрельчиха',
                        },
                    ],
                },
                {
                    minDistanceKm: 160,
                    maxDistanceKm: 200,
                    stations: [
                        {
                            id: '50423',
                            name: 'Скнятино',
                        },
                        {
                            id: '50438',
                            name: 'Новокатово',
                        },
                        {
                            id: '50442',
                            name: '177 км',
                        },
                        {
                            id: '50457',
                            name: 'Калязин-пост',
                        },
                        {
                            id: '50508',
                            name: 'Калязин',
                        },
                        {
                            id: '50512',
                            name: '5 км',
                        },
                        {
                            id: '50713',
                            name: '195 км',
                        },
                        {
                            id: '50527',
                            name: 'Чигирёво',
                        },
                        {
                            id: '612',
                            name: '12 км',
                        },
                        {
                            id: '50724',
                            name: '201 км',
                        },
                    ],
                },
                {
                    minDistanceKm: 200,
                    stations: [
                        {
                            id: '613',
                            name: 'Высокое',
                        },
                        {
                            id: '50809',
                            name: 'Кашин',
                        },
                        {
                            id: '50531',
                            name: 'Кулишки',
                        },
                        {
                            id: '614',
                            name: '24 км',
                        },
                        {
                            id: '50813',
                            name: '213 км',
                        },
                        {
                            id: '615',
                            name: '29 км',
                        },
                        {
                            id: '50828',
                            name: '220 км',
                        },
                        {
                            id: '50546',
                            name: 'Красное',
                        },
                        {
                            id: '50832',
                            name: 'Мюд',
                        },
                        {
                            id: '50550',
                            name: '40 км',
                        },
                        {
                            id: '50847',
                            name: '233 км',
                        },
                        {
                            id: '50601',
                            name: 'Углич',
                        },
                        {
                            id: '51002',
                            name: 'Кесова Гора',
                        },
                        {
                            id: '51017',
                            name: 'Золотково',
                        },
                        {
                            id: '51021',
                            name: '254 км',
                        },
                        {
                            id: '51106',
                            name: 'Сонково',
                        },
                    ],
                },
            ],
        },
    ],
    country: 225,
    currencies: ['RUR', 'USD', 'EUR'],
    heatmapInfos: [
        {
            name: 'infrastructure-1',
            type: 'infrastructure',
            palette: [
                {
                    level: 1,
                    from: 1,
                    to: 2,
                    color: 'ee4613',
                },
                {
                    level: 2,
                    from: 2,
                    to: 3,
                    color: 'f87c19',
                },
                {
                    level: 3,
                    from: 3,
                    to: 4,
                    color: 'fbae1e',
                },
                {
                    level: 4,
                    from: 4,
                    to: 5,
                    color: 'ffe424',
                },
                {
                    level: 5,
                    from: 5,
                    to: 6,
                    color: '9ada1b',
                },
                {
                    level: 6,
                    from: 6,
                    to: 7,
                    color: '30ce12',
                },
                {
                    level: 7,
                    from: 7,
                    to: 8,
                    color: '20a70a',
                },
                {
                    level: 8,
                    from: 8,
                    to: 9,
                    color: '128000',
                },
            ],
        },
        {
            name: 'price-rent-1',
            type: 'price-rent',
            palette: [
                {
                    level: 1,
                    from: 128478,
                    to: 73416,
                    color: '620801',
                },
                {
                    level: 2,
                    from: 73416,
                    to: 62358,
                    color: 'ee4613',
                },
                {
                    level: 3,
                    from: 62358,
                    to: 53751,
                    color: 'f87c19',
                },
                {
                    level: 4,
                    from: 53751,
                    to: 47500,
                    color: 'fbae1e',
                },
                {
                    level: 5,
                    from: 47500,
                    to: 42541,
                    color: 'ffe424',
                },
                {
                    level: 6,
                    from: 42541,
                    to: 38380,
                    color: '9ada1b',
                },
                {
                    level: 7,
                    from: 38380,
                    to: 34542,
                    color: '30ce12',
                },
                {
                    level: 8,
                    from: 34542,
                    to: 29184,
                    color: '20a70a',
                },
                {
                    level: 9,
                    from: 29184,
                    to: 10374,
                    color: '128000',
                },
            ],
        },
        {
            name: 'price-sell-1',
            type: 'price-sell',
            palette: [
                {
                    level: 1,
                    from: 1858678,
                    to: 538026,
                    color: '620801',
                },
                {
                    level: 2,
                    from: 538026,
                    to: 411297,
                    color: 'ee4613',
                },
                {
                    level: 3,
                    from: 411297,
                    to: 347648,
                    color: 'f87c19',
                },
                {
                    level: 4,
                    from: 347648,
                    to: 308167,
                    color: 'fbae1e',
                },
                {
                    level: 5,
                    from: 308167,
                    to: 274577,
                    color: 'ffe424',
                },
                {
                    level: 6,
                    from: 274577,
                    to: 240300,
                    color: '9ada1b',
                },
                {
                    level: 7,
                    from: 240300,
                    to: 208315,
                    color: '30ce12',
                },
                {
                    level: 8,
                    from: 208315,
                    to: 139831,
                    color: '20a70a',
                },
                {
                    level: 9,
                    from: 139831,
                    to: 18182,
                    color: '128000',
                },
            ],
        },
        {
            name: 'profitability-1',
            type: 'profitability',
            palette: [
                {
                    level: 1,
                    from: 82,
                    to: 33,
                    color: '620801',
                },
                {
                    level: 2,
                    from: 33,
                    to: 29,
                    color: 'ee4613',
                },
                {
                    level: 3,
                    from: 29,
                    to: 27,
                    color: 'f87c19',
                },
                {
                    level: 4,
                    from: 27,
                    to: 26,
                    color: 'fbae1e',
                },
                {
                    level: 5,
                    from: 26,
                    to: 25,
                    color: 'ffe424',
                },
                {
                    level: 6,
                    from: 25,
                    to: 23,
                    color: '9ada1b',
                },
                {
                    level: 7,
                    from: 23,
                    to: 22,
                    color: '30ce12',
                },
                {
                    level: 8,
                    from: 22,
                    to: 20,
                    color: '20a70a',
                },
                {
                    level: 9,
                    from: 20,
                    to: 5,
                    color: '128000',
                },
            ],
        },
        {
            name: 'transport-1',
            type: 'transport',
            palette: [
                {
                    level: 1,
                    from: 1,
                    to: 2,
                    color: '620801',
                },
                {
                    level: 2,
                    from: 2,
                    to: 3,
                    color: 'ee4613',
                },
                {
                    level: 3,
                    from: 3,
                    to: 4,
                    color: 'f87c19',
                },
                {
                    level: 4,
                    from: 4,
                    to: 5,
                    color: 'fbae1e',
                },
                {
                    level: 5,
                    from: 5,
                    to: 6,
                    color: 'ffe424',
                },
                {
                    level: 6,
                    from: 6,
                    to: 7,
                    color: '9ada1b',
                },
                {
                    level: 7,
                    from: 7,
                    to: 8,
                    color: '30ce12',
                },
                {
                    level: 8,
                    from: 8,
                    to: 9,
                    color: '20a70a',
                },
                {
                    level: 9,
                    from: 9,
                    to: 10,
                    color: '128000',
                },
            ],
        },
        {
            name: 'carsharing-1',
            type: 'carsharing',
            palette: [
                {
                    level: 0,
                    from: 0,
                    to: 1,
                    color: '620801',
                },
                {
                    level: 1,
                    from: 1,
                    to: 2,
                    color: 'ee4613',
                },
                {
                    level: 2,
                    from: 2,
                    to: 3,
                    color: 'f87c19',
                },
                {
                    level: 3,
                    from: 3,
                    to: 4,
                    color: 'fbae1e',
                },
                {
                    level: 4,
                    from: 4,
                    to: 5,
                    color: 'ffe424',
                },
                {
                    level: 5,
                    from: 5,
                    to: 6,
                    color: '9ada1b',
                },
                {
                    level: 6,
                    from: 6,
                    to: 7,
                    color: '30ce12',
                },
                {
                    level: 7,
                    from: 7,
                    to: 8,
                    color: '20a70a',
                },
                {
                    level: 8,
                    from: 8,
                    to: 9,
                    color: '128000',
                },
            ],
        },
    ],
    hasConcierge: true,
    hasCommercialBuildings: true,
    hasMetro: true,
    hasSites: true,
    hasVillages: true,
    hasPik: false,
    hasYandexRent: true,
    showMap: true,
    schoolInfo: {
        total: 399,
        highRatingColor: 'fa9b00',
        lowRatingColor: 'ffcc00',
    },
    isInOrlovskObl: false,
    isInPrimorskiyKrai: false,
    isInHabarovskKrai: false,
    isInTomskObl: false,
    isInKurganskObl: false,
    isInKirovskObl: false,
    isInUlyanovskObl: false,
    isInVologodskObl: false,
    isInPskovskObl: false,
    isInKalinigradskObl: false,
    isInIvanovskObl: false,
    isInChuvashskiya: false,
    isInUdmurtiya: false,
    isInSmolenskObl: false,
    isInKemerovskObl: false,
    isInTambovskObl: false,
    isInOmskObl: false,
    isInVladimirskObl: false,
    isInAltayskKrai: false,
    isInOrenburgObl: false,
    isInIrkutskObl: false,
    isInBashkortostan: false,
    isInStavropolskKrai: false,
    isInYaroslavskObl: false,
    isInVolgogradskObl: false,
    isInSevastopol: false,
    isInCrimea: false,
    isInBryanskObl: false,
    isInRyazanObl: false,
    isInLipetskObl: false,
    isInTverObl: false,
    isInTulaObl: false,
    isInTatarstan: false,
    isInKurskObl: false,
    isInKrasnoyarskKrai: false,
    isInTyumenObl: false,
    isInSaratovObl: false,
    isInChelyabinskObl: false,
    isInNovosibirskObl: false,
    isInSamaraObl: false,
    isInVoronezhObl: false,
    isInNizhnyNovgorodObl: false,
    isInPermKrai: false,
    isInKalugaObl: false,
    isInYaroslavlObl: false,
    isInAdygeya: false,
    isInSverdObl: false,
    isInRostovObl: false,
    isInBelgorodskObl: false,
    isInPenzenskObl: false,
    isInArhangelskObl: false,
    isInAstrahanskObl: false,
    isInMO: true,
    isInLO: false,
    isInKK: false,
    isObninsk: false,
    isRostovOnDon: false,
    isMsk: false,
    isSpb: false,
    rgidVoronezh: 569171,
    rgidVoronezhObl: 475531,
    rgidSverdObl: 326698,
    rgidRostovObl: 211571,
    rgidKK: 353118,
    rgidMsk: 587795,
    rgidMO: 741964,
    rgidLO: 741965,
    rgidSpb: 417899,
    rgidTatarstan: 426660,
    rgidNizhnyNovgorodObl: 426764,
};

export const newbuildings = [
    {
        id: 1685695,
        name: 'Цветочные поляны',
        fullName: 'ЖК «Цветочные поляны»',
        locativeFullName: 'в ЖК «Цветочные поляны»',
        location: {
            ponds: [
                {
                    pondId: '4372670196',
                    name: 'Дорохов ручей',
                    timeOnFoot: 265,
                    distanceOnFoot: 368,
                    latitude: 55.55525,
                    longitude: 37.309376,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 4,
                            distance: 368,
                        },
                    ],
                    pondType: 'RIVER',
                },
                {
                    pondId: '137669596',
                    name: 'река Незнайка',
                    timeOnFoot: 470,
                    distanceOnFoot: 653,
                    latitude: 55.552994,
                    longitude: 37.31159,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 7,
                            distance: 653,
                        },
                    ],
                    pondType: 'RIVER',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ee4613',
                    description: 'минимальная',
                    level: 1,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        regionType: 'CITY',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Москва',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Новомосковский административный округ',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Новомосковский административный округ',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ',
                        },
                    },
                    {
                        value: 'поселение Филимонковское',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'поселение Филимонковское',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Новомосковский административный округ, поселение Филимонковское',
                        },
                    },
                    {
                        value: 'квартал № 165',
                        regionType: 'UNKNOWN',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'квартал № 165',
                        queryParams: {
                            rgid: '587795',
                            address:
                                'Россия, Москва, Новомосковский административный округ, поселение Филимонковское, квартал № 165',
                        },
                    },
                    {
                        value: '7',
                        regionType: 'HOUSE',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: '7',
                        queryParams: {
                            rgid: '587795',
                            address:
                                'Россия, Москва, Новомосковский административный округ, поселение Филимонковское, квартал № 165, 7',
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['e4402d'],
                    metroGeoId: 218467,
                    rgbColor: 'e4402d',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Филатов Луг',
                    metroCityRgid: 587795,
                    timeToMetro: 31,
                },
                {
                    lineColors: ['e4402d'],
                    metroGeoId: 218466,
                    rgbColor: 'e4402d',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Прокшино',
                    metroCityRgid: 587795,
                    timeToMetro: 39,
                },
                {
                    lineColors: ['e4402d'],
                    metroGeoId: 218465,
                    rgbColor: 'e4402d',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Ольховая',
                    metroCityRgid: 587795,
                    timeToMetro: 42,
                },
                {
                    lineColors: ['ffe400'],
                    metroGeoId: 190121,
                    rgbColor: 'ffe400',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Рассказовка',
                    metroCityRgid: 587795,
                    timeToMetro: 51,
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.610943,
                        longitude: 37.491734,
                        defined: true,
                    },
                    distance: 18155,
                },
            ],
            insideMKAD: false,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2574,
                    distance: 35298,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 42,
                            distance: 35298,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            point: {
                latitude: 55.557026,
                precision: 'EXACT',
                longitude: 37.312317,
            },
            parks: [],
            distanceFromRingRoad: 18155,
            geoId: 114619,
            populatedRgid: 587795,
            address: 'пос. Филимонковское, ЖК «Цветочные Поляны»',
            rgid: 2962,
            subjectFederationName: 'Москва и МО',
            subjectFederationRgid: 741964,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ee4613',
                    description: 'минимальная',
                    level: 1,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            airports: [
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 1601,
                    distanceOnCar: 14315,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 26,
                            distance: 14315,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 3643,
                    distanceOnCar: 63322,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 60,
                            distance: 63322,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3700,
                    distanceOnCar: 60560,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 61,
                            distance: 60560,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 5583,
                    distanceOnCar: 80445,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 93,
                            distance: 80445,
                        },
                    ],
                },
            ],
            schools: [],
            settlementGeoId: 213,
            metro: {
                lineColors: ['e4402d'],
                metroGeoId: 218467,
                rgbColor: 'e4402d',
                metroTransport: 'ON_TRANSPORT',
                name: 'Филатов Луг',
                metroCityRgid: 587795,
                timeToMetro: 31,
            },
            settlementRgid: 587795,
            expectedMetroList: [],
        },
        viewTypes: ['GENERAL', 'GENERAL', 'GENPLAN', 'GENERAL', 'GENERAL', 'GENERAL', 'COURTYARD', 'GENERAL'],
        images: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c485719c3e1818ed416f64cb20/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e9fa625fb529e81713800ccbfbf/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017e9c0e42b83109d6dcdf2d182e01/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c4833f51fe7bad778b86e5e9d8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017e9fa6bfa5693ea52fab55494a46/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e9fa6f1db390e4911649d6c3127/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a00000174c482e0e9e7b5bcd975ae51f17d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a00000174c482c7d039e9f0a27f614cb6cf/realty_minicard',
        ],
        buildingClass: 'COMFORT',
        state: 'UNFINISHED',
        finishedApartments: true,
        price: {
            from: 4479750,
            to: 20519300,
            currency: 'RUR',
            minPricePerMeter: 144600,
            maxPricePerMeter: 261700,
            rooms: {
                '1': {
                    soldout: false,
                    from: 4973400,
                    to: 9213000,
                    currency: 'RUR',
                    areas: {
                        from: '26.9',
                        to: '43.5',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: undefined,
                },
                '2': {
                    soldout: false,
                    from: 6430006,
                    to: 13462350,
                    currency: 'RUR',
                    areas: {
                        from: '38.7',
                        to: '61.7',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: undefined,
                },
                '3': {
                    soldout: false,
                    from: 9268860,
                    to: 20519300,
                    currency: 'RUR',
                    areas: {
                        from: '64.1',
                        to: '89.8',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: undefined,
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    from: 4479750,
                    to: 8086530,
                    currency: 'RUR',
                    areas: {
                        from: '20.9',
                        to: '30.9',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: undefined,
                },
                PLUS_4: {
                    soldout: true,
                    from: 19794288,
                    to: 19794288,
                    currency: 'RUR',
                    areas: {
                        from: '109.2',
                        to: '109.2',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: undefined,
                },
            },
            totalOffers: 0,
            priceRatioToMarket: 0,
            minArea: 20.9,
            maxArea: 89.8,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '8165695',
                        meanPricePerM2: '185294',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '8102405',
                        meanPricePerM2: '183986',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '7896414',
                        meanPricePerM2: '181721',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '7879445',
                        meanPricePerM2: '181265',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 269952,
                name: 'ГК МИЦ',
                legalName: 'ООО «Специализированный застройщик «Староселье»',
                legalNames: ['ООО «Специализированный застройщик «Староселье»', 'ООО «СЗ «СЕРЕДНЕВО»'],
                url: 'http://www.gk-mic.ru/',
                logo: '//avatars.mdst.yandex.net/get-realty/2899/company.269952.2255520692855034810/builder_logo_info',
                objects: {
                    all: 15,
                    salesOpened: 9,
                    finished: 7,
                    unfinished: 8,
                    suspended: 0,
                },
                address: 'Москва, Космодамианская наб., 52, стр. 1Б',
                born: '1998-12-31T21:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJ0MLzEN3NPjMR3',
                    },
                ],
            },
        ],
        description:
            'Приобретая жилье в ЖК «Цветочные Поляны», вы выбираете удобную инфраструктуру, высокий уровень экологии и безопасности, а также благоустроенный двор, в котором можно заниматься спортом и отдыхать всей семьей. В жилом квартале «Цветочные Поляны» на ваш выбор предлагаются различные варианты планировок: от удобных студий до просторных четырехкомнатных квартир, с продуманными планировками. Предлагаем на выбор 4 варианта готовой отделки квартир.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+74951049612',
                    redirectId: '+74951049612',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+74951049612',
                    redirectId: '+74951049612',
                },
                {
                    tag: 'serp',
                    phone: '+74951049612',
                    redirectId: '+74951049612',
                },
                {
                    tag: 'personalDefault',
                    phone: '+74951049612',
                    redirectId: '+74951049612',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+74951049612',
                    redirectId: '+74951049612',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            name: 'ГК «МИЦ»',
            logo: '//avatars.mdst.yandex.net/get-realty/3013/company.1685726.7852543862427090628/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 1685726,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvZBG+aUY9O237wZVIuG7dIXCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15g/H3/Sju1f3PF6NrSgsOXh0Ix1uY7CPQIlz5xm/GWYWJJmcsn6Dkmdynv0NU0ndHcowlOakPFI7r3xO5YJCJgeiDx4rdZOyK68OP/+zyFMZ01iErK46+WaOAqAljw3oMjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDSyAFxOpEs3kf221hiFpmqiaU3hfl9g7uFoBQeffYw+1EPN/SuZzesa2Tzer2yWyuJEIOVTzT/12aCfwh9qknlY39PuqcawGzvIFIhQ/3d8ItKnZK5YqAfQXdordb7xBwElyB60gf8pY5XPE5701rPZhTPz3Tpo1VEZru8sXPCS25sRUYITuxx1Bfp6tsf5yDBUcG9emswn0TlCo7siXnRptdFhWWpUyzNDz6fkjuorul9mluBgR8P58+NKMpzGp4Xix0Fiow2Ph00rTcj7ZrlmoL3GC9aaEITfZdb5jYjhnTeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vro2ntG4Rcfeugh+ClrXMb23kFWgTQt0RtPqq1ZAYTvgEhf6SPvxnkJ4r4udpCWS4ptjXb3Pgxne/HtKBf0OGFwwuXGHecpNppBFJLAdNj+YRRnevdBjBnuQ38Ick9Yhym8vrC5FVIih3H2j7wM4qF1bqqj1+wv0izp4Fbb5exxP1NzuipOdboxwIeBWipTPAafUX99syaPUDOpxuOU+nRwHHR8r7SJjtmo6UFGa/JTzCwJyxkRXjwzipE7JHf9+CBb08EurUSWsJQhNCPtj44LxcRiQnxnbRKLWSzrSJVaytdbtaqJ1MbyuTWV4MbaQmywHQjHW5jsI9AiXPnGb8ZZhWxuoAkVeaOXYDRpv4BoFRg1hgg6sHdO0mEgvFmeoexpphoRsw58DgdkhFJWy/USGFJS67vrUxEuqiFlVsIXFwIIq1ah1qySbzU8zuw9tItpsOzRybz3rvlUEWon+rDdbcFGgT1wcjgQXU0dn7yQSDWzLPAymfDO+nh+ic2f2NE0y28NqnpMEwKsVXdsN6f0J7W6dOIbDOpJ0Vi/NCD2bJTQspk5BtvFgFiU+tKRZHRVPi/kGCdlhMlM8N05aqJL8+yuxtvp3Til3dKM+hAsdomWe8poHfsmirXw4TQkfm+8tgGzV0MQS9g0dfIdzePBRabcR4SSgLRlyt5/JW611JmVlg1xIsv0zwSdbLTkZEJ2MDqtNfv5u9lnYzzzW0L+iSedAhMK7PjAu0eVlsFwZlifwX7zrqhkzoT2fHn9VzqmFps9Uk7rOp5m20Os2oto4P3H+sGsvKSgmzPcid5lZp2dVDRCHA1wel6kXCPHQgtxohS3UIc+VxwMPET/PvgbWRz+ymgC+4wko9M5qmk5X+vJo9ZzVqxVhJWgurLGalw6syzwMpnwzvp4fonNn9jRNMtvDap6TBMCrFV3bDen9Cfca/flUEEy2HPKH8og4PQU0rPWhsdZc9kqqjC4Y3BRxc+pjsLnwF3tSyTvXqAr6c7WC6b7l1h55K0MppAzIDtwJtWGF2zon4FCY1W6BkLmA2/8NirIcAERk2AIRgoWTJiq+SkmpbX9zNjphrAjQMbiSNLJveENkal5sEBFdarrS0y/8Zf8UkdmF7Z9UDXm2krezmfSvuVhMsXIGVieqWkSawBUNBCkL+yFn1B4hg8jyywSuVtTXIEQtSqDj1gN7owCjLQBdr8dRgyzmH24PxBGSSRr1Zwqh4tK/esYBQ5QlkNjYwC4yeIqK09w/Vc6Lj/rbD6E/vH8dWCmX547vw5hYz3hGRlUjR1Pc8feEuI5hRqibELS7Ng5N5yubv0R5QHA1C4+gj5njiZpXzpNrORdUlmbZBD1v8cWy0Gtp1ua9HepzXJB6SXig54/gt71NV6du5p2OEFdE3VpqJ6iGskEt91DdGvPoSt7c23RiLHcpEjeMPlUgUYkrACrRGzWmTW9Q/YpZC9F0zOWhdfytN2Psj5Dh53wpMapqTTP04sEzVduF+pyQBgLPxsDY9FddwO6g9ZTfT9KTeDJZUcgAedpnza1e8XGMmBy3xCWyRfYg5LZbqwvNoEw7MVXXE4Ny21/JEJEnPdXRpqWcEONWkkPzqXYaqRLsAxoA2Y9iun4fsdNCFONPRnWV1wwxoqxSY+rZP0q6MsFshs7VvlhOKhKDUGLzTmvyez1l0pA/RdTJvwWl5Ub1pVDxAMx1oFqTjfSQp2oVXGHqiW/YYyE2B+H41oR5fgjt23199PReqv46OlZg4eqxqyJQ0XVN+LxE9zAG0PIbaJp85DwuvcNTWirxAtio0vppcjjOBZYtCMOPzFVZGes23jXVM6o9RiX9IhCsIH1gDj9SQO2OUJ8x0GXLD3v2JC53+BO0KOR8XLNqvvUIw2XjoDsl0jomj4RNe0ChdF4FC25sjeSu/8INGqECmxpZBEA2wXMeDb/hfATwvZVV5xOzJjysRIbqrHH3xfx9KidAOT6yVhJ84ZadFA9dOvJ8+Ri+Wcb7rXqB82TmyKsofRSvQbHnJKRv903+rk2RWirYr+LdjDMIPawfRqE8nsitrIR7LpKNsbwx2XV5w==',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'Vq/CclfLb7Zo7V1lQUUQKvqYd1MkkdvudUL6vYO5fp3k0f6KFahPMegJAXsDMDk0664X1yR3AfxcJTVSzc9eGCZ1FDQPfDJG7RyO2ITNILoKkDX5rzBiX8uO+NO2crfWaW6NiOV33SQ5DosI1Vv0wNXRL+lKGC50o/gTu5kOSWR/B4P/nAhm5uGmieN49M4ODi1j9/ku8iX3MAaknf6rdX6nS5KVEEO3oWGPsmh9hAxP0NtKjTJds3ItIv92QZCKRa2/UIKC92uAruOIZJiztwrGAXlP390D1QSgB2zhvk0V2IcaVs1hmLxtj/x+596m9ZtxAR79ea2omNV3jXGHAM8lihK0aUnKWpeUX9eGSICrSTqLRk4fEzISwwpItllQsT4Gbd3xZHnsBEMK/ovpV2L326itHNjIvWzucZ3XvvSLX+rdXuZoM2Tt3+1TM+M/m960ZsW/Wcl47j4lw/Ydh9VYcV3icwgtci6VzkSi+pxjrvKq5RnOdLEALhhI8E2YOmGbyJzD8z8469MN1ykxg3tel7WAvO4GPOa9BUMrx1Q3CyvccnHXLaXyIQQ/CfI9KnZZxRYWXa7UgVU9UGW5R4e3vEovKPiGdPyBi5oC4r3HHlRh0O7mXTZAz0H6eOqxJK2cYdOAUWn9YjHEHuqnDfTu9FcRZaMYTI2qxxTB8hOejAjnSmnKkj8Dc4ez1zcJUmPAQESBY03WHD9F7kSwVjDhIv6kMYWhl41UxWoa9HRltF21peKhyQyv8ypwHuDlAL0jxQt+Ojre+KOVBoFIhT8zHhDUUXLqpICfWBvFs5L2TJFTaNyGZXunkHAnhAoHFCJKQP3J10WyE4cUxRgA8bJ/EJ/3dR2W7mVbZRSlSK8aqoXJfFruLSbUZQ/EaLrM0V8xDMpAR51AMqt4RpZyPzllp9U62CJUXbht',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF0OHTUJxMLDQN5NPjERy',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=1685695',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
    {
        id: 166185,
        name: 'Бунинские луга',
        fullName: 'Бунинские луга',
        locativeFullName: 'в  «Бунинские луга»',
        location: {
            ponds: [
                {
                    pondId: '164159447',
                    name: 'Ивановский пруд',
                    timeOnFoot: 493,
                    distanceOnFoot: 685,
                    latitude: 55.5486,
                    longitude: 37.48011,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 8,
                            distance: 685,
                        },
                    ],
                    pondType: 'POND',
                },
                {
                    pondId: '1996028781',
                    name: 'река Варварка',
                    timeOnFoot: 879,
                    distanceOnFoot: 1221,
                    latitude: 55.549606,
                    longitude: 37.479183,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 14,
                            distance: 1221,
                        },
                    ],
                    pondType: 'RIVER',
                },
                {
                    pondId: '164161720',
                    name: 'Потаповские пруды',
                    timeOnFoot: 890,
                    distanceOnFoot: 1236,
                    latitude: 55.537823,
                    longitude: 37.484837,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 14,
                            distance: 1236,
                        },
                    ],
                    pondType: 'POND',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: '30ce12',
                    description: 'низкая',
                    level: 7,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        regionType: 'CITY',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Москва',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Коммунарка',
                        regionType: 'CITY',
                        geoId: 29393,
                        rgid: '63487',
                        valueForAddress: 'посёлок Коммунарка',
                        queryParams: {
                            rgid: '63487',
                            address: 'Россия, Москва, посёлок Коммунарка',
                        },
                    },
                    {
                        value: 'улица Александры Монаховой',
                        regionType: 'STREET',
                        geoId: 29393,
                        rgid: '63487',
                        valueForAddress: 'улица Александры Монаховой',
                        queryParams: {
                            rgid: '63487',
                            address: 'Россия, Москва, посёлок Коммунарка, улица Александры Монаховой',
                            streetId: 140790,
                        },
                    },
                    {
                        value: '90к2',
                        regionType: 'HOUSE',
                        geoId: 29393,
                        rgid: '63487',
                        valueForAddress: '90к2',
                        queryParams: {
                            rgid: '63487',
                            address: 'Россия, Москва, посёлок Коммунарка, улица Александры Монаховой, 90к2',
                            streetId: 140790,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['8dbece'],
                    metroGeoId: 20520,
                    rgbColor: '8dbece',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Бунинская Аллея',
                    metroCityRgid: 587795,
                    timeToMetro: 17,
                },
                {
                    lineColors: ['8dbece'],
                    metroGeoId: 20519,
                    rgbColor: '8dbece',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Улица Горчакова',
                    metroCityRgid: 587795,
                    timeToMetro: 21,
                },
                {
                    lineColors: ['8dbece'],
                    metroGeoId: 20518,
                    rgbColor: '8dbece',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Бульвар Адмирала Ушакова',
                    metroCityRgid: 587795,
                    timeToMetro: 24,
                },
                {
                    lineColors: ['e4402d'],
                    metroGeoId: 218464,
                    rgbColor: 'e4402d',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Коммунарка',
                    metroCityRgid: 587795,
                    timeToMetro: 26,
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.610943,
                        longitude: 37.491734,
                        defined: true,
                    },
                    distance: 9200,
                },
            ],
            insideMKAD: false,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2497,
                    distance: 31152,
                    latitude: 55.74816,
                    longitude: 37.613552,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 41,
                            distance: 31152,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            point: {
                latitude: 55.54344,
                precision: 'EXACT',
                longitude: 37.48227,
            },
            parks: [],
            distanceFromRingRoad: 9200,
            geoId: 29393,
            populatedRgid: 741964,
            address: 'Москва, мкр. Бунинский, ул. Александры Монаховой',
            rgid: 63487,
            subjectFederationName: 'Москва и МО',
            subjectFederationRgid: 741964,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            airports: [
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 2211,
                    distanceOnCar: 23670,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 36,
                            distance: 23670,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 2428,
                    distanceOnCar: 42262,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 40,
                            distance: 42262,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 3430,
                    distanceOnCar: 60678,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 57,
                            distance: 60678,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3697,
                    distanceOnCar: 57057,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 61,
                            distance: 57057,
                        },
                    ],
                },
            ],
            schools: [],
            settlementGeoId: 29393,
            metro: {
                lineColors: ['8dbece'],
                metroGeoId: 20520,
                rgbColor: '8dbece',
                metroTransport: 'ON_TRANSPORT',
                name: 'Бунинская Аллея',
                metroCityRgid: 587795,
                timeToMetro: 17,
            },
            settlementRgid: 63487,
            expectedMetroList: [],
        },
        viewTypes: [
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'ENTRANCE',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'ENTRANCE',
            'COURTYARD',
            'GENERAL',
            'GENERAL',
            'ENTRANCE',
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'ENTRANCE',
            'GENERAL',
            'GENERAL',
            'ENTRANCE',
            'HALL',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'GENPLAN',
        ],
        images: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_main',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_main',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527d8c6ed0cd10f4850b1c043ef/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017f08047c4024f3cd23b9dd9df5dd/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f08047cdddadf7257af99bcc841/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017f0804780eae7f8458c4f471bb78/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1535139/2a0000017f0804315b4c4bdc7697fb927c87/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527e439f08f71399d4c7bfddb75/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017527e2fc6f7be8bbf921787d5d6c/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c92400b95efca13a8cfe3bfa0a8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923fea5e0755f4d61dcfd7702a/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c923fce348c95d778ffb106e2b8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923fa5f41152ef7d1344db67bb/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c923f8664533313b3ce7a11a99e/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c923ef577f6944a55a431ca9eaf/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c5fb10c5f252d258b60bc007e7b/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c5fb0f4ab0c96044edd0055a7b4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0e1b9b05f8d756387486438/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c5fb0cb09006bea81dc25b0eaf2/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c5fb0b4c657ded332aa7f948be3/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017527e1d50fe102635b4e68efdadc/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527e1ac19cc5d3809de62f95ebb/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527dab8e458af0bcbe16934774b/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527dae791d2011cad1cd0371b64/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017527da4aa070a75c9245ed41bbb2/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017527da0477755a1ad320a92844ab/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017527d9e62cded20ccbf7009a1a18/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017527d9d0c3ecb8109fc7c5d75c5a/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017527dbb5849438b27e528af72b16/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017f080305d381a165084c26dd888f/realty_minicard',
        ],
        // siteSpecialProposals: [
        //     {
        //         proposalType: 'MORTGAGE',
        //         description: 'Ипотека 4.99% на весь период',
        //         endDate: '2022-12-31T00:00:00.000+03:00',
        //         mainProposal: false,
        //         specialProposalType: 'mortgage',
        //         shortDescription: 'Ипотека 4.99% на весь период',
        //     } as ISiteSpecialProposal,
        // ],
        buildingClass: 'COMFORT',
        state: 'UNFINISHED',
        finishedApartments: true,
        price: {
            from: 9433060,
            to: 9433060,
            currency: 'RUR',
            minPricePerMeter: 184600,
            maxPricePerMeter: 184600,
            averagePricePerMeter: 184600,
            rooms: {
                '1': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                '2': {
                    soldout: false,
                    from: 9433060,
                    to: 9433060,
                    currency: 'RUR',
                    areas: {
                        from: '51.1',
                        to: '51.1',
                    },
                    hasOffers: true,
                    offersCount: 1,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '3': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                PLUS_4: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
            },
            totalOffers: 1,
            priceRatioToMarket: 0,
            minArea: 51.1,
            maxArea: 51.1,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '9473615',
                        meanPricePerM2: '228885',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '9348618',
                        meanPricePerM2: '226178',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '9397644',
                        meanPricePerM2: '230851',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '9426858',
                        meanPricePerM2: '232456',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 52308,
                name: 'ПИК',
                legalName: 'ПАО «ПИК СЗ»',
                legalNames: ['ПАО «ПИК СЗ»'],
                url: 'http://www.pik.ru',
                logo: '//avatars.mdst.yandex.net/get-realty/2935/company.52308.2202722811127988665/builder_logo_info',
                objects: {
                    all: 148,
                    salesOpened: 94,
                    finished: 77,
                    unfinished: 71,
                    suspended: 0,
                },
                address: 'Москва, Баррикадная ул., 19, стр.1',
                born: '1993-12-31T21:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjANxMPzUR4',
                    },
                ],
            },
        ],
        description:
            'Бунинские луга расположен в новой Москве, в 1 км от метро «Бунинская аллея». Его окружает сразу несколько обустроенных парков и прудов. Строительство района подразумевает поэтапное возведение современных жилых домов и всей инфраструктуры. Первая очередь строительства уже завершена. Открыли свои двери детские сад и школа. В рамках проекта будет построено школы, детские сады, поликлиники, физкультурно-оздоровительный центр.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+79214428598',
                    redirectId: '+79214428598',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+79214428598',
                    redirectId: '+79214428598',
                },
                {
                    tag: 'serp',
                    phone: '+79214428598',
                    redirectId: '+79214428598',
                },
                {
                    tag: 'personalDefault',
                    phone: '+79214428598',
                    redirectId: '+79214428598',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+79214428598',
                    redirectId: '+79214428598',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '00:00',
                            close: '23:59',
                        },
                    ],
                },
            ],
            name: 'ПИК',
            logo: '//avatars.mdst.yandex.net/get-realty/2899/company.918322.8813397388247573526/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 918322,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvbIDAYku6+sDSXr4yYwC8CwCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15DJ3XVHxHiqqpGEHcc1hg+h0Ix1uY7CPQIlz5xm/GWYWQt0A6dwwo2zhlm8zXvoeSLwmaRehiIapXTI+wYhykDlS7N1i/47bf3uKHT+0ILklPwkuCLzzhNasN+t9avREIzoX1Nk3v6Xkr/8qESkPWMt8aMbarO/5hHo87ojhlLp1k7AnaNA6I+qV+4Y7HsZ5xCmxpZBEA2wXMeDb/hfATwpn02CFSg/YgKInsdWAutseciILCSb+gPE36U9F6aMaHn8F+866oZM6E9nx5/Vc6poKw9b7rSDDtSufLUaGlXmQygLHwJKiXgDzn1UnQNtnWjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDS1+bSuUqIGxZ42UBIc6OW0q5fSrxGuLxrfh5gukcwNUl9mluBgR8P58+NKMpzGp4VUMkQG085qSXmSPJJTUeX4Jh/TXkaw7ebVXmWQ00CUEjeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vroksFr2xXVINXWi5xAyn1EKUFWgTQt0RtPqq1ZAYTvgEhkCJtESfOsZ6144lXOFAVWV0HFQVHVT3RHGXzlVCv3jmkKaROAwkLn8iSnUHMaB4lgyHnwJmUUlwYF7qDvgjRhWUx77nFopxeGh6RCHXxHeLMs8DKZ8M76eH6JzZ/Y0TTk4tDs97nWF5470QCGyqD18fSonQDk+slYSfOGWnRQPbT+o+89nV/KkypsWhUz4iZTxcS3myG/a5vYZLhBZoTuDJ3XVHxHiqqpGEHcc1hg+hNdJIhMAZAF9Pn2wdcPqCWJTaW6yEfUNML/YIINjLNsGwyRSH0Z5+kK/GK6M9YebV24Fy8r6g0IH9wT69mIQKrwxuNrQ6OH61ZAlGmqGCiGO5b3h9K5hB4s/ev1OAHwi0c0ZZ7Umnbt5zb5WueV97GD/v19ShjZ9xnzi1F3By5SZhA0yI1XnPOWhhs7WnEv128Rtk/sPsDrghwpkd47xfUKbGlkEQDbBcx4Nv+F8BPClceSCBy6+g+QDM4OuzaXKXzpfCC32LwwHN56IaAwJ18ulE648HIXb4bJsYjSugMEdSn7+uWiOZ2j9y8Zwr4VbypicwUW+JvvpMjaUIyqaDAE1FqLi8bqSL8iCI1B2coYIZm3oDi+SPkHqdteGdFoALMs8DKZ8M76eH6JzZ/Y0TQ8yctQOaC5LdlU2nswl3iIAC+0Ohm/h9oYC+LQwLkxeYupeyPuU1D/fqlzJbp3N/jed8+EpgAsA7wIujmgBfG/zXC9pbMUmqjAAVgR3sC7wFqqByJX4B5Bcjk5znwYC1DxcNr5wV2h78nBwd2Nfy93lAn+D+Q/TkED4l+x7yM3xfbgdCa+J4yRl8b/0l+RPfq64Xqjo1igQyVOu4BooDa2CmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lylX3mZYeZlHs3IaRLbplIn3F6ZD2DrApsNq99nvTb8vhgC0PaRox6cRdMzML62eiccHuh7BVSJ+/QRjRtqYqtIPebi9Q2/hwFGU+bQ12s/VhbArqG7RlRhRGf8O+Jm2744Ka5pw7kFGbdb9dCFGCVqfjsC9myTlvEp7ndvSy8iRRGGqx3b1nQFVbiIm4UC1eFDezmfSvuVhMsXIGVieqWkSj/o63hxCpf6aQS/QFrawEvFw2vnBXaHvycHB3Y1/L3d068nz5GL5ZxvuteoHzZObxn7KEmfEZPlsiMQ5UkkzsdqCGUHeeJ6DiUFx9ZPNHL3hlgeONyRfyYMJF6Kb093HyjrNc48OEquqhQUY9EM/720WwCRKP8BEW+PJxeMxLAhRf32zJo9QM6nG45T6dHAcbPV/lrPn7xVO2H/0ropF7jMEv6+TkHno25qXFx4NNRqpmx1cQ38vOqnr+i5brZVaB+r07hMG2IZv7wJPkOAzH3WlhU/0PAxOGpTrd+51Lo8+ANXlXJdhxw1YTSfyftzt9RltBgYHB1G7bteM9VZ0RfhLSYfVcdZFgHl3wX2ZulhsFyEbKHQs4UBSUe8F/aQkoOdt3M0wamayAOUmNEg6txbWfjD9aECVr6l+k3LlbHQjnSk2nYqptzHwHp/bk2iUQ5ABUN5z+8fLSRslNQT3MZeADDGLdo8tXlnfefY1aizf0+6pxrAbO8gUiFD/d3wiq8SWTT20wyygTnksV7iZyBLjrej42qj2E5OoXG3qsqkshaA8WqdoDdfpCO7bs/KJ3nfPhKYALAO8CLo5oAXxv6e4fEbo3ltKDxmm4qvXagJ2/E9wLYGDdWxiRCuvJRvCYz3hGRlUjR1Pc8feEuI5hcQez8nFpqWDivRoru30F9TwjakItGi2xpwtjiTc5H187gfzpYQ1Zj4Sa+0cWSYUTPJLwIeUORx4fasn/V97bP+U2r0dHCYGt7HM7jsnrcoDWvllJjm0tcObto/IjScnn2wXIRsodCzhQFJR7wX9pCR01iErK46+WaOAqAljw3oMvROmZZeliDkY29friz5J87HChTNfVqM45NvG0LlUoE2CizuDN82oRhpNZ3DuSYuCV5Az8fZfj+oVnh5tv1l+Og==',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'VQyst94uy0hb9UXXHCETpUXMARPHtF/9CS0boWXsUdn6za2slzm3Iu/dTY4uNnxvDcmgKvZfn/JaBbOB8Cbrq6DAm2InkpEg+gOLN6HD66Mv4PlCaeL26bSZTYqQk9aOPINteSiX8bhen0O5pFY5FC7zbM+/Niz087r5EQDTydmRTrv0wqrNs/UJIZTlKIcDUwdUnG/sd5T/UcRtH/9fPwTMek+sWBGd6p/QUdSiIEhMYEiVRZFRM0ZT9mEFW6Sj7QjBGChJ3FhFXUDH4w7yrU3PC8kChjfGiV7RjJ2gp7u7xqVviB9b3IGTSDVzccAmGyO6EgKs25kBp8aHS/g1tDScMI5qbX0shlIjRkV/WDKl+x9pYFu7z6JHPlMTD6Zx/DQrKBOxu0742DIlO0aP4/ja1vHcFae9ktWRhESH2oCAZLWVyoKgFLyyQOH2fwoEt/20aONOzVzZ0u1rVx2vahlOPhJhHXga8VMflXGrI3dLQbZ/tlmuNC8Nsh3wlKQxdPdbbxRhMYlAsaK9yDo5qlnEJr1OyUThu6xTCmaDMYfrUz0mS8rcFtJYtm42tch2jXM7r1B8T2Ub3L35z411iMwFRyGNL7MneuGHbQiKFXZtnJ+9rasgJCSFJuTkCa4VFr29gdtCGoi8A0nocfRiC5xFt+r10+p0+fUVdrHf1C7rZY3ClbgRtfGLHkqJE3G7PQAxn//JLvgbuHOYhdVj3fHKvf+a4dY5wi2Hlmu1HXOJKJYf7sZSoqdx7fsxc1RseT9rhVD4jxfJGmTrUhBy/jaTxeYOEVxVXPW869Cyx/jLlxekJ1Q5YOBav5JmyvjhFfyOZpJ0MjTwGFGPigXXh+f+Ml6emwytlXdXbdKo6ENAPq7+ANNEB9KHKNEFuL8L7TILbGm/x8jxXYs/NNE1SdQANdo8O46TSM9A6H+KhiGJ/W2n',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF5MHjEJ0NLDIN4NPTkR4',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=166185',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
    {
        id: 872687,
        name: 'Capital Towers',
        fullName: 'МФК Capital Towers',
        locativeFullName: 'в МФК Capital Towers',
        location: {
            ponds: [
                {
                    pondId: '137667589',
                    name: 'река Москва',
                    timeOnFoot: 180,
                    distanceOnFoot: 253,
                    latitude: 55.749245,
                    longitude: 37.54954,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 253,
                        },
                    ],
                    pondType: 'RIVER',
                },
                {
                    pondId: '164173396',
                    name: 'Краснопресненские пруды',
                    timeOnFoot: 180,
                    distanceOnFoot: 286,
                    latitude: 55.753666,
                    longitude: 37.550816,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 3,
                            distance: 286,
                        },
                    ],
                    pondType: 'POND',
                },
                {
                    pondId: '1703440097',
                    name: 'Нижний Красногвардейский пруд',
                    timeOnFoot: 300,
                    distanceOnFoot: 469,
                    latitude: 55.755585,
                    longitude: 37.547,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 5,
                            distance: 469,
                        },
                    ],
                    pondType: 'POND',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ffe424',
                    description: 'достаточно развитая',
                    level: 4,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: 'ee4613',
                    description: 'очень высокая',
                    level: 2,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: 'ee4613',
                    description: 'очень высокая',
                    level: 2,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '30ce12',
                    description: 'высокая',
                    level: 7,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
                {
                    name: 'carsharing',
                    rgbColor: '30ce12',
                    description: 'высокая',
                    level: 6,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        regionType: 'CITY',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Москва',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Краснопресненская набережная',
                        regionType: 'STREET',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Краснопресненская набережная',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Краснопресненская набережная',
                            streetId: 185280,
                        },
                    },
                    {
                        value: 'вл14с1кБ',
                        regionType: 'HOUSE',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'вл14с1кБ',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Краснопресненская набережная, вл14с1кБ',
                            streetId: 185280,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['099dd4'],
                    metroGeoId: 98559,
                    rgbColor: '099dd4',
                    metroTransport: 'ON_FOOT',
                    name: 'Выставочная',
                    metroCityRgid: 587795,
                    timeToMetro: 11,
                },
                {
                    lineColors: ['6fc1ba'],
                    metroGeoId: 115085,
                    rgbColor: '6fc1ba',
                    metroTransport: 'ON_FOOT',
                    name: 'Деловой Центр',
                    metroCityRgid: 587795,
                    timeToMetro: 14,
                },
                {
                    lineColors: ['099dd4'],
                    metroGeoId: 98560,
                    rgbColor: '099dd4',
                    metroTransport: 'ON_FOOT',
                    name: 'Международная',
                    metroCityRgid: 587795,
                    timeToMetro: 15,
                },
                {
                    lineColors: ['ffa8af'],
                    metroGeoId: 152947,
                    rgbColor: 'ffa8af',
                    metroTransport: 'ON_FOOT',
                    name: 'Деловой Центр',
                    metroCityRgid: 587795,
                    timeToMetro: 19,
                },
                {
                    lineColors: ['ed9f2d'],
                    metroGeoId: 218566,
                    rgbColor: 'ed9f2d',
                    metroTransport: 'ON_FOOT',
                    name: 'Тестовская',
                    metroCityRgid: 587795,
                    timeToMetro: 20,
                },
            ],
            address: 'Москва, наб. Краснопресненская, вл. 14',
            rgid: 193368,
            routeDistances: [],
            subjectFederationName: 'Москва и МО',
            insideMKAD: true,
            subjectFederationRgid: 741964,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 756,
                    distance: 6726,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 12,
                            distance: 6726,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'ffe424',
                    description: 'достаточно развитая',
                    level: 4,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            point: {
                latitude: 55.751457,
                precision: 'EXACT',
                longitude: 37.548546,
            },
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 1875,
                    distanceOnCar: 30312,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 31,
                            distance: 30312,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 2696,
                    distanceOnCar: 30254,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 44,
                            distance: 30254,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3233,
                    distanceOnCar: 49502,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 53,
                            distance: 49502,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3816,
                    distanceOnCar: 54385,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 63,
                            distance: 54385,
                        },
                    ],
                },
            ],
            parks: [
                {
                    parkId: '121405908',
                    name: 'парк Красная Пресня',
                    timeOnFoot: 967,
                    distanceOnFoot: 1343,
                    latitude: 55.752327,
                    longitude: 37.54937,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 16,
                            distance: 1343,
                        },
                    ],
                    parkType: 'PARK',
                },
            ],
            schools: [],
            settlementGeoId: 213,
            metro: {
                lineColors: ['099dd4'],
                metroGeoId: 98559,
                rgbColor: '099dd4',
                metroTransport: 'ON_FOOT',
                name: 'Выставочная',
                metroCityRgid: 587795,
                timeToMetro: 11,
            },
            geoId: 213,
            populatedRgid: 587795,
            settlementRgid: 587795,
            expectedMetroList: [],
        },
        viewTypes: [
            'GENERAL',
            'GENPLAN',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'HALL',
            'HALL',
            'HALL',
            'HALL',
            'LIFT',
            'LIFT',
        ],
        images: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bd5e634dbcb035cbae5c13f8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017f1c86546ec80299e9707dc5feda/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d27bdc4263e2c28bc8afc76864e/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27bf889992dbdfb3fac1001c1f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beaa0db974fe03cbd2f6c75f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d27beb8a948f7b7909827558be4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017d243a450f7a16b8741de40e859f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a3355109f3d4c5c2ccf8ed4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d243a2e06ec9423d62835b55f5a/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017d243a24634124706d3020e1f745/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d243a205cf03d5fd186810e70d8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017d2438d87824f0e5a1f90490d423/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438d46937da08dcc3b5ffc4eb/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438b24e6cdc3d04f23dd5cd8d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017d2438bf2bf7f9c9e2b1e8b942da/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017d2438cb0a26e22902fbd155eee4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017d2438b956795e63cf7987a4aa31/realty_minicard',
        ],
        buildingClass: 'BUSINESS',
        state: 'UNFINISHED',
        finishedApartments: false,
        price: {
            currency: 'RUR',
            rooms: {
                '1': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '49',
                        to: '62',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '2': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '67.5',
                        to: '115.9',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '3': {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '82.5',
                        to: '128.2',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: true,
                    currency: 'RUR',
                    areas: {
                        from: '28',
                        to: '30.7',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'NOT_ON_SALE',
                },
                PLUS_4: {
                    soldout: false,
                    currency: 'RUR',
                    areas: {
                        from: '126.9',
                        to: '197.2',
                    },
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
            },
            totalOffers: 0,
            priceRatioToMarket: 0,
            minArea: 49.02,
            maxArea: 197.21,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '80533850',
                        meanPricePerM2: '967560',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '81536334',
                        meanPricePerM2: '928355',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '81536334',
                        meanPricePerM2: '928355',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '81536334',
                        meanPricePerM2: '928355',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 268706,
                name: 'Capital Group',
                legalName: 'ООО «МЕГАПОЛИС ГРУП»',
                legalNames: ['ООО «МЕГАПОЛИС ГРУП»'],
                url: 'http://www.capitalgroup.ru/',
                logo: '//avatars.mdst.yandex.net/get-realty/3375/company.268706.1595744232291160933/builder_logo_info',
                objects: {
                    all: 33,
                    salesOpened: 17,
                    finished: 28,
                    unfinished: 5,
                    suspended: 0,
                },
                address: 'Москва, Пресненская набережная, 8с1',
                born: '1992-12-31T21:00:00Z',
                hasChat: true,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJwMLjEN2NPjQRw',
                    },
                ],
            },
        ],
        description:
            'Три элегантных небоскреба высотой 270 м., объединенных общим основанием, расположены на набережные реки Москвы между «Москва-Сити» и Центром международной торговли. Все квартиры видовые.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+74996495079',
                    redirectId: '+74996495079',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+74996495079',
                    redirectId: '+74996495079',
                },
                {
                    tag: 'serp',
                    phone: '+74996495079',
                    redirectId: '+74996495079',
                },
                {
                    tag: 'personalDefault',
                    phone: '+74996495079',
                    redirectId: '+74996495079',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+74996495079',
                    redirectId: '+74996495079',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            name: 'Capital Group',
            logo: '//avatars.mdst.yandex.net/get-realty/3274/company.295494.267970144362838907/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 295494,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvaYiIe1aSjzUoYyIfijuEu9CmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15/EqL5H8driaioalaLwgT1lzVLJ5onqr1BJWH0hWmCdLSMx4TjIEE2IvM6Fbvstqc/mvJXeMOkn7cSlcvF1YwWFS7N1i/47bf3uKHT+0ILklPwkuCLzzhNasN+t9avREIzoX1Nk3v6Xkr/8qESkPWMt8aMbarO/5hHo87ojhlLp1k7AnaNA6I+qV+4Y7HsZ5xCmxpZBEA2wXMeDb/hfATwpn02CFSg/YgKInsdWAutseciILCSb+gPE36U9F6aMaHn8F+866oZM6E9nx5/Vc6poKw9b7rSDDtSufLUaGlXmQygLHwJKiXgDzn1UnQNtnWjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDS1+bSuUqIGxZ42UBIc6OW0R+nDt0V+LorRYZMiTjV8aF9mluBgR8P58+NKMpzGp4VEY1/Acd2wPWiIQNqumkhzDkW8JKDC/y67ARzqYIGcZjeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vro2gXzyEZIGAsSS69G++32J0FWgTQt0RtPqq1ZAYTvgEi5BQxLm4s9WVILgl1adGCxGGZyKfLpGZr6wq3exHicb7qtWD2JWfnAzO4+Bb81LWPhF9CyHodZBrcC7wa0v0GCYBBw8VyV9EeqZdsWKXd3K8kzKDyIwUDqIUhbjF4qUoyJTaW6yEfUNML/YIINjLNs4B/vc9BuyAE3u65/OlwZe4zuJaR5kgBPPzrIlWw8e/G7aK100sRCWYtWbIduiyis7KW+KZElY7IOTAEUwfAS2ubiXeHtNhTFuurdKxIC4wpjPeEZGVSNHU9zx94S4jmFg1vrQOStl9NMAnnYvykTHd0NSultCSTqmwK0keKU7FJji8Nxz35i01WyXu9AAy4nELRMjZowbYuQwwfA2K4NSloL8c9Rs47De7Ay35GYngo/wDyDnAAibIzmfUSwBeFW3naiM2FbCN895zCa+b07DKbcR4SSgLRlyt5/JW611JlCigBsL2FL4yso4unovXKBrvtTeXR8JEsZSGaCxDIvbzQRVXekIA6qZlWET4nFkJ8Z39A2uOdnY+XPfTHXWmsf1fXB14E5Lm/t6WDSjDvN8mOU5bX3cHzjBDxXpslTebX1L8dQaDnjKtvjNu5fxxYco/lUqLnjWYRe+c8BeZ9tU8P5oN/oPP3qs5SAadUdlprPYqe3uf0tg+tlFpyp/20nP+zhAHSqp20agM4QCaFZ/W6WIxdvb4x/iwU8pW+W1UkuLosIsUnBUWdCKkWQaBEObsV12x/alyBWOJ7UN46UptYjogN3d7sp4scIqPLKgbSesXxF3PCyBRzy6DTB8jUPiXNmlAEI3giqQ4Thli10QEAIV7X0Iw957UxRQRsfdhyFfoUZFZaz0KrakNI2NkbOQooAbC9hS+MrKOLp6L1yga77U3l0fCRLGUhmgsQyL29lc94QF+hq5HLArfI2RdWOx00IU409GdZXXDDGirFJj6tk/SroywWyGztW+WE4qErhMqXrXWaaMZTaNYZv7CL/iI2V/mrPmSoodEF7VbkO86nk8SFRqohE1qUBVf0n/ISBVtTxROLCbLgRFJYsJXTR/WV0rwTioso9UeZaJyAsqcQLYqNL6aXI4zgWWLQjDj+JaysyhOhcAiFYmGutP4gHWLfsMDFezdqoVHsh7wCh4N1l2Su+tvdxXBgEsqXQG/VNMwf+EIxCa/YAVtlGHrOCxdpbdldcEnAG59MwdgF/jDBG16x8WrlmxrALjIOi8yHlAEiG7jU9igeJh/NaainW23faQ9umW5IusuEshz9Ko1u1qonUxvK5NZXgxtpCbLCvr+aE715L89WjuVXI0+xkue5TWjca3kJdWcz2tTb8CUN8slwlJuFphgmaw9n18KKsrbQGr9YGDMJR2m1cwL6ZHQjHW5jsI9AiXPnGb8ZZhRimPCqSM5g94fAr5vzA5E5c1SyeaJ6q9QSVh9IVpgnSRiKTb1auwrIVl46VmyHe2RVr+y7hNfOLIEaAvDGCWey4iVypoDanzzlwp4IcxkOxyjWqx7p2S1IBV7uIS1vsV1F/fbMmj1AzqcbjlPp0cBx1zxxls4a7sGIrrE5ioz2sGq3WHC6iFqScWkzXT8pKROj8vdRdU6BxdHg8FCYK8gYyHtb3KC/M0AiPlfhQYrs9uxuBxpZY87VPR6NrAZ0DhB0H91ROM96yMHyt89nRdaZuliMXb2+Mf4sFPKVvltVJ4DB9t7uh7dTibHZuN9i5F7sR0S3taIBq2i2nh0l1/pQKbGlkEQDbBcx4Nv+F8BPC7kDzjVTXjNn5llvYDbh1TWvv8wUPV3scCFAzEJjuDbddW8bUMur7J62ayszSfO6EXnFeDNq9LX2AQHxC+T4fxRpWd3JJ12+1Nb8LGa0EzzhC/tiqJRVTZZZf3U7e0ie4SUo+GnJKZhIBM0QHs6ZXsvjoCXNhXWvnf5JNIQ5jgPW1bdy1SSC+KJMntlZcGtcHlOXLHuxG5E4/PZLde7jc+mTsCdo0Doj6pX7hjsexnnEsMPgcIBoL4ID23KtRYkUjwBl1Q9fV0yd/XYiTYJp4gA==',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'm0CnBF0mlb8cBHOL4erAAbb3GtNq4UPN5YmeqOaAMIg/sAHAYUffttafe62/g2UiYLf6LkVtkFQzCnd54ctOiNHcADmzYA4qWbj6yintioHIprj9/NUcQp7ngTlB9SDj2a9zxiudys7c2wD0nVSDOiATPHlJldrelB79iuZna2YuMfyA3uIE5OXIAwfjEQMJSlYQRZeN9LMQ9ANaSD3gR1bIsQ9Atv592HiO6p3noGb2/OoSI9WR4w11IgC1J/7Mrn1rqfA4pOrRK/7Xa4db2aYPbycA6c3nWdVwUk8SHTN/yjgvpsHySKxQ91OWhdYUEaSZFQz0o/o4Wq0ACI7ZIbaSrL9DokxpbZzh3dXp/pmNVviZNAQ0GNfKRJicQTqLHJtc5c5VffMbHh1TVysfIXAPhsJDwkGUsRVIB3waWnDvkrkcVKOfnseQpPVMTJSLGU8cTQFF7YI2i8OucVHk5eZev2dqEhP+p/C9BkyPS3rGSmCh7+b8+0e8ISPZD+I7agI2AWsU0ohLmQLVBRx/x5Yyf33qI/3QsfMe8OTCsw4Uz+IAFqK96gCkezla+Fwft73BbtfNSy0C98D5p1JNo7iiys+z8EBcXoO6fox7zlyjaGPkv0r/BigMvqdbZOvFf1Umul06QjpRpzQpuVUdH7qC7tU9Se2Z4BlDBcKDeNtuFWuYG7wby7LG8fE1M3duTcr79speiMbSBOAdi1/qJgLb3b3SGtIWQlJLd4K70QT7FAHwU2Ojq/g9B/L9q0I/68gTOMusQphNU466H9sN/9HHA7YveyaWLKrC8dL+hOIY0TyoZXKjY85PRK2xADp293E+Gx0ud6Ac/o4OQ1nqOZx3mfkF49i75nbZWtleYwYj2QZR6J79zJ0LVXqzRxewZG08tn1QYaUAT1g=',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF0OHTkJ2NLDkN1MPDcR5',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=872687',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
    {
        id: 305036,
        name: 'Ильинские луга',
        fullName: 'Ильинские луга',
        locativeFullName: 'в  «Ильинские луга»',
        location: {
            ponds: [],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: '30ce12',
                    description: 'низкая',
                    level: 7,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Московская',
                        regionType: 'SUBJECT_FEDERATION',
                        geoId: 1,
                        rgid: '587654',
                        valueForAddress: 'Московская область',
                        queryParams: {
                            rgid: '587654',
                            address: 'Россия, Московская область',
                        },
                    },
                    {
                        value: 'округ Красногорск',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        geoId: 98590,
                        rgid: '587683',
                        valueForAddress: 'Красногорск (городской округ)',
                        queryParams: {
                            rgid: '587683',
                            address: 'Россия, Московская область, Красногорск (городской округ)',
                        },
                    },
                    {
                        value: 'Ильинское-Усово',
                        regionType: 'CITY',
                        geoId: 118175,
                        rgid: '62613',
                        valueForAddress: 'посёлок Ильинское-Усово',
                        queryParams: {
                            rgid: '62613',
                            address:
                                'Россия, Московская область, Красногорск (городской округ), посёлок Ильинское-Усово',
                        },
                    },
                    {
                        value: 'Заповедная улица',
                        regionType: 'STREET',
                        geoId: 118175,
                        rgid: '62613',
                        valueForAddress: 'Заповедная улица',
                        queryParams: {
                            rgid: '62613',
                            address:
                                'Россия, Московская область, Красногорск (городской округ), посёлок Ильинское-Усово, Заповедная улица',
                            streetId: 17329,
                        },
                    },
                    {
                        value: '3',
                        regionType: 'HOUSE',
                        geoId: 118175,
                        rgid: '62613',
                        valueForAddress: '3',
                        queryParams: {
                            rgid: '62613',
                            address:
                                'Россия, Московская область, Красногорск (городской округ), посёлок Ильинское-Усово, Заповедная улица, 3',
                            streetId: 17329,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['0042a5'],
                    metroGeoId: 20568,
                    rgbColor: '0042a5',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Строгино',
                    metroCityRgid: 587795,
                    timeToMetro: 26,
                },
                {
                    lineColors: ['df477c'],
                    metroGeoId: 218570,
                    rgbColor: 'df477c',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Павшино',
                    metroCityRgid: 587795,
                    timeToMetro: 29,
                },
                {
                    lineColors: ['0042a5'],
                    metroGeoId: 109160,
                    rgbColor: '0042a5',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Мякинино',
                    metroCityRgid: 587795,
                    timeToMetro: 32,
                },
                {
                    lineColors: ['df477c'],
                    metroGeoId: 218564,
                    rgbColor: 'df477c',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Красногорская',
                    metroCityRgid: 587795,
                    timeToMetro: 34,
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.789684,
                        longitude: 37.37222,
                        defined: true,
                    },
                    distance: 10411,
                    highway: {
                        id: '20',
                        name: 'Новорижское шоссе',
                    },
                },
            ],
            insideMKAD: false,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 2100,
                    distance: 35543,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 35,
                            distance: 35543,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            point: {
                latitude: 55.775276,
                precision: 'EXACT',
                longitude: 37.23992,
            },
            parks: [],
            distanceFromRingRoad: 10411,
            geoId: 118175,
            populatedRgid: 741964,
            address: 'Московская обл., Красногорск, пос. Ильинское-Усово, ул. Заповедная',
            rgid: 62613,
            subjectFederationName: 'Москва и МО',
            subjectFederationRgid: 741964,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 2256,
                    distanceOnCar: 34821,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 37,
                            distance: 34821,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 2882,
                    distanceOnCar: 28556,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 48,
                            distance: 28556,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3710,
                    distanceOnCar: 72049,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 61,
                            distance: 72049,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 5050,
                    distanceOnCar: 75870,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 84,
                            distance: 75870,
                        },
                    ],
                },
            ],
            schools: [],
            settlementGeoId: 118175,
            metro: {
                lineColors: ['0042a5'],
                metroGeoId: 20568,
                rgbColor: '0042a5',
                metroTransport: 'ON_TRANSPORT',
                name: 'Строгино',
                metroCityRgid: 587795,
                timeToMetro: 26,
            },
            settlementRgid: 62613,
            expectedMetroList: [],
        },
        viewTypes: [
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'COURTYARD',
            'ENTRANCE',
            'ENTRANCE',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'ENTRANCE',
            'ENTRANCE',
            'COURTYARD',
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'ENTRANCE',
            'COURTYARD',
            'GENERAL',
            'ENTRANCE',
            'COURTYARD',
            'ENTRANCE',
            'ENTRANCE',
            'COURTYARD',
            'ENTRANCE',
            'COURTYARD',
            'ENTRANCE',
            'GENPLAN',
        ],
        images: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_main',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_main',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_main',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/1030388/2a000001752b78ab833242b9699c0cd58cac/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e71033430ffc51fcf2d73acad7d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e71030e1abccffefd1652287481/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017e7102e4a2129b664d2d6defbb1a/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e7101c4353634a15c1e1149e64d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1604130/2a0000017e71019ee5bdaeb8f6664ed827be/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017e710173df94265520e71579d554/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017e70fea8e6c7c3942d4829f6bef5/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd850568ac34bd2f0c7aea55/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017e70fd5cff4215587525b89a4502/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a0000017c319dacb83c305e9c33fccd2af1/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c319c27925af81fbad640f64e78/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb2463b9dbc13c6de1f63cc89b/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017ccb2444b0a2647cc8648e69fdd8/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017ccb2425fb4c4b57d34fe8a7ce93/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017ccb240cafbf8cfffc1997aee4bc/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c3be964cc9c17601bab95904f8d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1030388/2a0000017c3be94a5b54442c79d2622ccd91/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be932fb60d3e4a48eaca911ad/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be90ec300f2013a41c079f706/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a0000017c3be8e8bd1829cffd423b2a659d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c3be8c835bce43f52922c98ce2c/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be8a53b34a63fe053f6e36bdd/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000017c3be80f4d2d5631551830cafa5e/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c319bf9c904976b3a985906036b/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319b06ee855a5c3cb33a63bfd7/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a0000017c319ae6e0ca70bd882ad90d5746/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b7779a852b4bfbaccf2b9e9cc/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/3587101/2a0000017c0cc87c66f568343d315f4d06a4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a0000017c0cc89d9885d31733f8ece057a4/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001752b75d8b9eb99208e222e38bbf0/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001808eb43dc5bb0ec31bf700c20686/realty_minicard',
        ],
        // siteSpecialProposals: [
        //     {
        //         proposalType: 'MORTGAGE',
        //         description: 'Ипотека 4.99% на весь период',
        //         endDate: '2022-12-31T00:00:00.000+03:00',
        //         mainProposal: false,
        //         specialProposalType: 'mortgage',
        //         shortDescription: 'Ипотека 4.99% на весь период',
        //     } as ISiteSpecialProposal,
        // ],
        buildingClass: 'COMFORT',
        state: 'UNFINISHED',
        finishedApartments: true,
        price: {
            from: 5836960,
            to: 5836960,
            currency: 'RUR',
            minPricePerMeter: 152799,
            maxPricePerMeter: 152799,
            averagePricePerMeter: 152799,
            rooms: {
                '1': {
                    soldout: false,
                    from: 5836960,
                    to: 5836960,
                    currency: 'RUR',
                    areas: {
                        from: '38.2',
                        to: '38.2',
                    },
                    hasOffers: true,
                    offersCount: 1,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '2': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                '3': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                PLUS_4: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
            },
            totalOffers: 1,
            priceRatioToMarket: 0,
            minArea: 38.2,
            maxArea: 38.2,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '6906078',
                        meanPricePerM2: '166916',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '6834335',
                        meanPricePerM2: '164533',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '6888950',
                        meanPricePerM2: '168132',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '6976724',
                        meanPricePerM2: '169999',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 52308,
                name: 'ПИК',
                legalName: 'ООО «ГрадОлимп»',
                legalNames: ['ООО «ГрадОлимп»'],
                url: 'http://www.pik.ru',
                logo: '//avatars.mdst.yandex.net/get-realty/2935/company.52308.2202722811127988665/builder_logo_info',
                objects: {
                    all: 148,
                    salesOpened: 94,
                    finished: 77,
                    unfinished: 71,
                    suspended: 0,
                },
                address: 'Москва, Баррикадная ул., 19, стр.1',
                born: '1993-12-31T21:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJxMLjANxMPzUR4',
                    },
                ],
            },
        ],
        description:
            'Проект среднеэтажной застройки с собственной инфраструктурой раскинется на участке площадью 215 Га в Красногорском районе Московской области, в 10 км от столицы по Новорижскому шоссе.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+74951532126',
                    redirectId: '+74951532126',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+74951532126',
                    redirectId: '+74951532126',
                },
                {
                    tag: 'serp',
                    phone: '+74951532126',
                    redirectId: '+74951532126',
                },
                {
                    tag: 'personalDefault',
                    phone: '+74951532126',
                    redirectId: '+74951532126',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+74951532126',
                    redirectId: '+74951532126',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '00:00',
                            close: '23:59',
                        },
                    ],
                },
            ],
            name: 'ПИК',
            logo: '//avatars.mdst.yandex.net/get-realty/2899/company.918322.8813397388247573526/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 918322,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvbIDAYku6+sDSXr4yYwC8CwCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15DJ3XVHxHiqqpGEHcc1hg+h0Ix1uY7CPQIlz5xm/GWYWQt0A6dwwo2zhlm8zXvoeSLwmaRehiIapXTI+wYhykDlS7N1i/47bf3uKHT+0ILklPwkuCLzzhNasN+t9avREIzoX1Nk3v6Xkr/8qESkPWMt8aMbarO/5hHo87ojhlLp1k7AnaNA6I+qV+4Y7HsZ5xCmxpZBEA2wXMeDb/hfATwpn02CFSg/YgKInsdWAutseciILCSb+gPE36U9F6aMaHn8F+866oZM6E9nx5/Vc6poKw9b7rSDDtSufLUaGlXmQygLHwJKiXgDzn1UnQNtnWjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDS1+bSuUqIGxZ42UBIc6OW0q5fSrxGuLxrfh5gukcwNUl9mluBgR8P58+NKMpzGp4WSVwzTCmxYvUTSYP8r/VOvzHLWKA0x53dQDK2G+pFJADeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vro14xRg6VDzr1CfUEBAf9+l0FWgTQt0RtPqq1ZAYTvgEi8EzygJILcPY/hMz4ZFMUAEwW7T483RCCh7RlGW2Zl1GkKaROAwkLn8iSnUHMaB4lgyHnwJmUUlwYF7qDvgjRhWUx77nFopxeGh6RCHXxHeLMs8DKZ8M76eH6JzZ/Y0TTk4tDs97nWF5470QCGyqD18fSonQDk+slYSfOGWnRQPbT+o+89nV/KkypsWhUz4iZTxcS3myG/a5vYZLhBZoTuDJ3XVHxHiqqpGEHcc1hg+hNdJIhMAZAF9Pn2wdcPqCWJTaW6yEfUNML/YIINjLNsGwyRSH0Z5+kK/GK6M9YebV24Fy8r6g0IH9wT69mIQKrwxuNrQ6OH61ZAlGmqGCiGO5b3h9K5hB4s/ev1OAHwi0c0ZZ7Umnbt5zb5WueV97G1OGPuavsZd88+oOWOdm5/ZhA0yI1XnPOWhhs7WnEv128Rtk/sPsDrghwpkd47xfUKbGlkEQDbBcx4Nv+F8BPClceSCBy6+g+QDM4OuzaXKXzpfCC32LwwHN56IaAwJ19IuAVzYleREfCezD9mkfRXdSn7+uWiOZ2j9y8Zwr4VbypicwUW+JvvpMjaUIyqaDAE1FqLi8bqSL8iCI1B2coYIZm3oDi+SPkHqdteGdFoALMs8DKZ8M76eH6JzZ/Y0TQ8yctQOaC5LdlU2nswl3iIAC+0Ohm/h9oYC+LQwLkxeYupeyPuU1D/fqlzJbp3N/jed8+EpgAsA7wIujmgBfG/zXC9pbMUmqjAAVgR3sC7wFqqByJX4B5Bcjk5znwYC1DxcNr5wV2h78nBwd2Nfy93V+Ou8SAzODeve89c1rC5g0klEHtYjxaY/E697GBMl1VpWdxbEshnhfH+wFBMeE+ACmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lylX3mZYeZlHs3IaRLbplIn30hiMlAykZCZ0VGyOrfKT6wC0PaRox6cRdMzML62eiccHuh7BVSJ+/QRjRtqYqtIPebi9Q2/hwFGU+bQ12s/VhbArqG7RlRhRGf8O+Jm2744Ka5pw7kFGbdb9dCFGCVqfjsC9myTlvEp7ndvSy8iRRGGqx3b1nQFVbiIm4UC1eFDezmfSvuVhMsXIGVieqWkSj/o63hxCpf6aQS/QFrawEvFw2vnBXaHvycHB3Y1/L3d068nz5GL5ZxvuteoHzZObxn7KEmfEZPlsiMQ5UkkzsdqCGUHeeJ6DiUFx9ZPNHL0P2bNuudPZD3E4cByIxZmRlLux99hmrfXXKxIvzvjm8jzGnjFNWt24qz3fGufaG1ex+IXv6ibU4eCyvn+n76oVvnW/48DlijAuslNPUSvdGN6GFUYh9HCT1YrPH5pTOxuSeytlAqOgVKCDcUduu2Kt5oczK11OFYJFk0vpYWnjEO4t3nZKO6R1/61K22yytZ9Xg4ZKCtabBavrAOux5NnvgvU/XpSqAuMG6khv+irOZEQX2LNEPL4MIh6OJjgzg0ndZdkrvrb3cVwYBLKl0Bv1TTMH/hCMQmv2AFbZRh6zgqsKIpTcNuh38T0mMzTcgw5U5mfEBtVeqTCI43fj2kKe+bYAeJl78yxkfiOKfbn4hywz9xVJuhA1/Qec7I3dWXHAVV4XRt2uPhNd3hdqQtAaqxpGY2jjG2NLlovulgYACZC08LSJHCzFFGctzWay5O/HTQhTjT0Z1ldcMMaKsUmPKXF5eqEG7hGuuc+545yp7T7448wsQcS9UTZNYUlphptL0hfwjU301nai3fPRNzGBHQjHW5jsI9AiXPnGb8ZZhbtm/oOc86VXPn1yEAe6WNGm0/xyWL6URKaQr3W+zP/3j89Hd2YTn/R8d2/sIYOahknYQ1iTnE5LFEqrChg6tw5i83G1YzJcOZ2a674YVRXmo/lUqLnjWYRe+c8BeZ9tU3a8Tr1/+hqmW5dpzMISDe22O4xYmitkO2gsCgYdmq8DFWv7LuE184sgRoC8MYJZ7OIis2lV0I63UxK6hkSDu1gD1vrXL7M2DPrSd9LGJUfZnzSnffJCInHQBUsVao8arg==',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'oWSnPtTRWybIdBEpl6VC3SafCatkjDJivk95eNLjol9W6a89e7WIP/BNVTluBkJhd0pmtUzNDfNwJnORd9tKtjOEyV1pvRMl6SGuKtYcDBTS4MZV6khnZao8JBar39hUcDeLKFn7iKA5OvMT6vYethFmvR14FSVXm86xn9UeoKXon4Lr8Hr6/nWL1SvsQQHC5ZuYca73kEhc602B/ZVrVKjnghY1Ro06wOfJ2czoVmfnzj/GpOfkRtk4BjqDl9cuGGW1TqwciE+wxpDhwZ1+9P+jMPYkZrljmk31b6CenZfsKpIpcvX9OXjVfDaUH3l3dwkBkSifP1HUTc2v/WVMyW3+WYrpRR43YPJIPg/3prQOAN83RHxxn1KSbzCGvNlxI1hlMLPoRx1WRDjShcSt5L1Ebi9Ryca92BRag/GGwwRA8dQlXE++Wl5IQ+QF9bZpHifk2HKNRSiEjVnCWEG+yb1K25Tr60PjJxhjackNmREsCffaIWfawPgmeqPXTUPhtk5J7ZIHuRbesuHmuIauFwyvb18JCqpumaZyO0deQzE1hX3QFTTVmcXf25vEVr9pmjuY0EsD1FwQ3SiFSyrlNP2Nkw5ZODJJdsLYRtyC7QdxnsEI5tufwKttxVBklmzR/i2RkwvYrfuWAQDTaehHNUBqWaZ0C6WhDUYF9XUXyBZ/JS7iRTU7abwLECTaaKhHvt+lnhZ2EJazgdf02wFUITcB0tJZbEfDqbo2QLd9MNv8VZqgkvqlnbOk4TzkpndGHUw8yzKSUI+BOiVIBGho0y1Xy8wKQsfm4s2NgLbk5Hi8eKiRC23+6Dh7mLF/1LwDnnnPdVByhX2pLrjQs4zEcVNtg5K9OQXtDIna9/852GqEPTT2KGwhpIXdreHWn0Q64YQn4bHq/4z1/NsYA/gKCXw2nmEVQzlRokPHbmsbTuOhXg==',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF0OHTUJxNLTMNyMPTIR2',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=305036',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
    {
        id: 79355,
        name: 'на Беговой',
        fullName: 'дом на Беговой',
        locativeFullName: 'в доме на Беговой',
        location: {
            ponds: [],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '30ce12',
                    description: 'хорошо развитая',
                    level: 6,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: 'ffe424',
                    description: 'выше средней',
                    level: 5,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: 'ffe424',
                    description: 'выше средней',
                    level: 5,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '20a70a',
                    description: 'очень высокая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
                {
                    name: 'carsharing',
                    rgbColor: 'ffe424',
                    description: 'выше средней',
                    level: 4,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Москва',
                        regionType: 'CITY',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Москва',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва',
                        },
                    },
                    {
                        value: 'Хорошёвское шоссе',
                        regionType: 'STREET',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: 'Хорошёвское шоссе',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Хорошёвское шоссе',
                            streetId: 181019,
                        },
                    },
                    {
                        value: '12к1',
                        regionType: 'HOUSE',
                        geoId: 213,
                        rgid: '587795',
                        valueForAddress: '12к1',
                        queryParams: {
                            rgid: '587795',
                            address: 'Россия, Москва, Хорошёвское шоссе, 12к1',
                            streetId: 181019,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['ed9f2d', 'b1179a'],
                    metroGeoId: 20375,
                    rgbColor: 'ed9f2d',
                    metroTransport: 'ON_FOOT',
                    name: 'Беговая',
                    metroCityRgid: 587795,
                    timeToMetro: 2,
                },
                {
                    lineColors: ['b1179a'],
                    metroGeoId: 20374,
                    rgbColor: 'b1179a',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Полежаевская',
                    metroCityRgid: 587795,
                    timeToMetro: 8,
                },
                {
                    lineColors: ['ffe400', '6fc1ba'],
                    metroGeoId: 189452,
                    rgbColor: 'ffe400',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Хорошёвская',
                    metroCityRgid: 587795,
                    timeToMetro: 8,
                },
                {
                    lineColors: ['b1179a'],
                    metroGeoId: 20499,
                    rgbColor: 'b1179a',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Улица 1905 Года',
                    metroCityRgid: 587795,
                    timeToMetro: 9,
                },
                {
                    lineColors: ['4f8242'],
                    metroGeoId: 20558,
                    rgbColor: '4f8242',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Динамо',
                    metroCityRgid: 587795,
                    timeToMetro: 14,
                },
            ],
            address: 'Москва, Хорошевское ш.',
            rgid: 12441,
            routeDistances: [],
            subjectFederationName: 'Москва и МО',
            insideMKAD: true,
            subjectFederationRgid: 741964,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 938,
                    distance: 8679,
                    latitude: 55.749058,
                    longitude: 37.612267,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 15,
                            distance: 8679,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: '30ce12',
                    description: 'хорошо развитая',
                    level: 6,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '9ada1b',
                    description: 'высокая доступность',
                    level: 6,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            point: {
                latitude: 55.774994,
                precision: 'EXACT',
                longitude: 37.550243,
            },
            airports: [
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 1822,
                    distanceOnCar: 28423,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 30,
                            distance: 28423,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 2896,
                    distanceOnCar: 33121,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 48,
                            distance: 33121,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3419,
                    distanceOnCar: 51809,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 56,
                            distance: 51809,
                        },
                    ],
                },
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 4002,
                    distanceOnCar: 56693,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 66,
                            distance: 56693,
                        },
                    ],
                },
            ],
            parks: [],
            schools: [],
            settlementGeoId: 213,
            metro: {
                lineColors: ['ed9f2d', 'b1179a'],
                metroGeoId: 20375,
                rgbColor: 'ed9f2d',
                metroTransport: 'ON_FOOT',
                name: 'Беговая',
                metroCityRgid: 587795,
                timeToMetro: 2,
            },
            geoId: 213,
            populatedRgid: 587795,
            settlementRgid: 587795,
            expectedMetroList: [],
        },
        viewTypes: ['GENERAL', 'GENERAL', 'HALL', 'ENTRANCE', 'HALL', 'LIFT', 'GENERAL', 'GENERAL'],
        images: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_main',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/937147/2a0000016094103d62a056e12e69e97ffd03/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001609cae8ce6ec8e8445742270588d/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a0be0b149a1d96825cce89079f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a04b3a682d7d332e029e865e06/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609d97051785e904d0196c069ab6/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a00000160a3294dcbccf37feb3def88eb5c/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a000001609a3164efb47541cced06b69ac1/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/787013/2a0000016091c8d5232b9c9e3fb0bfc19c0a/realty_minicard',
        ],
        buildingClass: 'BUSINESS',
        state: 'HAND_OVER',
        finishedApartments: true,
        price: {
            currency: 'RUR',
            rooms: {
                '1': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                '2': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                '3': {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                PLUS_4: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
            },
            totalOffers: 0,
            priceRatioToMarket: 0,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '12328387',
                        meanPricePerM2: '268008',
                        timestamp: '2021-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '12376470',
                        meanPricePerM2: '269053',
                        timestamp: '2021-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '16035714',
                        meanPricePerM2: '291558',
                        timestamp: '2021-06-30T00:00:00Z',
                    },
                    {
                        meanPrice: '16500000',
                        meanPricePerM2: '300000',
                        timestamp: '2021-07-31T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 21331,
                name: 'ДОНСТРОЙ',
                legalNames: [],
                url: 'http://www.donstroy.com/',
                logo: '//avatars.mdst.yandex.net/get-realty/2935/company.21331.1274241827130720067/builder_logo_info',
                objects: {
                    all: 35,
                    salesOpened: 22,
                    finished: 29,
                    unfinished: 6,
                    suspended: 0,
                },
                address: 'Москва, ул. Мосфильмовская, 70',
                born: '1993-12-31T21:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJyNLjYN4MPjER2',
                    },
                ],
            },
        ],
        description:
            'ЖК Дом на Беговой  создан по индивидуальному архитектурному проекту. Такой подход позволяет обеспечить уникальность здания, его узнаваемость, а также сделать планировки квартир свободными, что в свою очередь, помогает владельцам нового жилья обустроить его в соответствии со своими вкусами и предпочтениями. Дом на Беговой не исключение. Здесь каждый желающий сможет подобрать квартиру, которая сразу станет родным семейным гнездышком для ее жильцов.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+78120000000',
                    redirectId: '+78120000000',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+78120000000',
                    redirectId: '+78120000000',
                },
                {
                    tag: 'serp',
                    phone: '+78120000000',
                    redirectId: '+78120000000',
                },
                {
                    tag: 'personalDefault',
                    phone: '+78120000000',
                    redirectId: '+78120000000',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+78120000000',
                    redirectId: '+78120000000',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            name: 'Savills',
            logo: '//avatars.mdst.yandex.net/get-realty/3274/company.344969.6139949194493917564/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 344969,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvbAmQovBw5hmnWx0xb4bwgyCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15CVouD25E50NYpuTs/iUPfh0Ix1uY7CPQIlz5xm/GWYWJJmcsn6Dkmdynv0NU0ndHFffO+qwKTStY+EBaAqYohkp3DG03cyWZbjaiC3/zll/m4l3h7TYUxbrq3SsSAuMKhTPz3Tpo1VEZru8sXPCS25sRUYITuxx1Bfp6tsf5yDBPwkuCLzzhNasN+t9avREIzoX1Nk3v6Xkr/8qESkPWMoSCHANLk0VlF/bTY98MffkTziLejREfmHEwRjcL35fa3nfPhKYALAO8CLo5oAXxvxHDvNvzlLTXliImYiMzNLEhEMtxtlwJqfuyka3eOLrBVLs3WL/jtt/e4odP7QguST0fKQS3eqv4t6DfaSldXkRwU92a5YSUBRsuxSkl0qOZSHV/+J5ZY/SasSZqQbwNexEB39BSv4PbTcwYSEn9rE/EUP3lDdI9zO3zaH8xs8EcNSRENFS7KkbQblTAsjR9XGUSiisYnoqbOB9uZN2zudptXuRs3DdDsFKYNNCbxKbpRFw1He8pd/JXRq2I/ppB7/2LSoPp9ZShtAoZB+ObPs/N9XfFTMSHyK1FfkXYggJA45vzJeXQ0F22VJ0E9anqse/vj0sOxZxdZtwmBsdAHpHm4l3h7TYUxbrq3SsSAuMKYz3hGRlUjR1Pc8feEuI5hUfB8B93xC46RQhyfoJZKKQ3wjOJKovoDH9B6psMfsvY0kKdqFVxh6olv2GMhNgfhwZNapC9gGEgXQe8sEJLIDtpTeF+X2Du4WgFB599jD7UjJtm0oRAS0wlIbRgnKSsrQ1SKBwv/+OJirgtA9arlYUQ4duPK3JsawqcitL4CZ8Ks192mZYJ4/Jbra8jF79ghUShD65zZk1SlEym0/cRWrTlAEiG7jU9igeJh/NaainWNEjgSGEOT8VEl2lU/O2vblF/fbMmj1AzqcbjlPp0cBxm1ERs/EFLEwME9TLwm+k8TTMldzhSxAZUyATuC/mvsfW8gxj5DgdubcCHlAQynPIKbGlkEQDbBcx4Nv+F8BPCEWMX+6fmO9soi9goItcFErEOmHZopMcCqcClobQWUcp3yhRDQciNu76XEbTTG+rmWFIQhFjzZOTkCRFKha4EU7E8ZSr10QiUNqD4OMcFaN3I7uqcGNdMrGnYHlpIN73wtZNZnWvyQvFQw7KsCWaCye8msrluV4m3fqAR0SEdu04mo8/tnUgxEh4eBQyzqhXZ30BrbfX7l9bhUMQhK1bzQmQ3GyBww4gUOJprRT+iSQL9O32rcfl99Tp2Mw4gK6DhWXA1PRDi7WEqOebi6/L9upZJQVyfhNXzvJ1Am+2F75QOTB+sfnMirIMfoqC5uLQyZtREbPxBSxMDBPUy8JvpPE0zJXc4UsQGVMgE7gv5r7H9I8JMd6c+0NM+Q0rqYn4tl4AMMYt2jy1eWd959jVqLN/T7qnGsBs7yBSIUP93fCJipFoMIy64j9fBoKPAhxMyS9xW8sjZ0+irUEQGRCv3yx0Ix1uY7CPQIlz5xm/GWYVRo5qiKxpxgZ3euSCI9C0HTyogtFNtVUjBxf1UbUoN/GwXIRsodCzhQFJR7wX9pCSg523czTBqZrIA5SY0SDq3nwdSg8VyKTQv+gb5czgz2LY83EnvT9dDuyPpNWxvJSazLPAymfDO+nh+ic2f2NE0nRJoGcM0wAZs3S+p5Rl27bY7jFiaK2Q7aCwKBh2arwP2Q3FeAPlJ/ps//Ytbc12QY3U2pePGYpe+n5hAPpSU3lHtaYdObLkO21tgSdDNDmko183gN5DPxhuOcsuZSlseXNUsnmieqvUElYfSFaYJ0rTcg0lrWoAkFT9ubSnUEYNjPeEZGVSNHU9zx94S4jmFZdotB9XovJqi/ZdVxDB8Q1S843d9gIa7fCHIvsW6fuoGP2DKYT8XoJMifMQAAOJz8noSuNs2xCv/T1w78UdjV4lNpbrIR9Q0wv9ggg2Ms2xI5g9yhKsP4ZJAHxEmeQ51CVouD25E50NYpuTs/iUPfh70jt0pXf8Z2XrI8bvPZq6W5N8r1j/c7D3eWuTysFYJQz3qcUJv0qzebcSpjonh5NYjogN3d7sp4scIqPLKgbTo/L3UXVOgcXR4PBQmCvIGDhgGQnOI+jRiN+39huCiEyng71M3FbrWYC6GBFWZLFDlAEiG7jU9igeJh/NaainWKms78dP8p/ZCKpqdlR/lbHR66Z/REozU7q+nfRdiZzaUIqVLmsFXjQawN+1fNX8PtiNF2iJ6jvH1/vKa7GXHlY7XNCrmrEJQWxVePG/0JvR6EwXhnZk7T/K9TvIXQtK924UvDCbwPCS4866zvRKj2EvJJ4tqiTxEOjIY4+ymvMPTeuYiZ4gGrO7Fa+Va8GO1YWv+9yA1dFD2Ls2DvWUB7kL+2KolFVNlll/dTt7SJ7hJSj4ackpmEgEzRAezpley+OgJc2Fda+d/kk0hDmOA9bVt3LVJIL4okye2Vlwa1weU5cse7EbkTj89kt17uNz6ZOwJ2jQOiPqlfuGOx7GecUWoN8fyaW91OrqMeyt45b5JGq9/dYVEWygnKPj23Fed',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'z5aB1gRsK6m8GI49tuxh9E3KDmK88vO1+ngDDQYYFYFUyg6wjHgIplYVZnM4pu5VwRAJsmBnUlpnGbH9C+ONcoN5qT4hn1VDpgsGk+FpjPZsPTELZpALeMoalaHKHE50+w5CrHZcDi+KzMQyaOJvTfIUesmqNiQEorDzMubKzytZZv/Ut0h1R2gtJZA8W+j9+24zmY2Trz3gxKCYRjHDXwgIPb/UMz79KVui9IT4JaqLI5gsYf2+OJlbkeINm7RdOwj1n6ZXfKlUygf29C+akMLRCmcuFpyQ2xM8gUJZhIHw9rt3ojnJJ+4hNgZfFl61HyD7he/eQC85uDvCbyEJvlo1buxw0/J1lkNbri1MA/uYFkUyjj65KK1VGww4dX5Ow2v3UyB/JQuJhp8UzN7LdGZiObTzT4LeLg5Zt/RJdHM7ptKtAMOC/c3tEF3xCn6S3Q1IBw8VJb0MNPaS/z9rt4m+6nc5lZWgJ+NWYGhC6um2RsAl/IU5XGlQyI3Y0qEUN4RAuY+sZPIM2hBJl682r62Q1ieXCz8IexZ25MJzYp2lYpnVUDMxPNxG4TZCzoIeZX4hXVDcRC0jOgKhEiRhZ4ZPtGuJYL3AZJygn6KN3W41vwuQTSYuWJ4h/0tTVdIM9GdOs84TNEGaVs5GVZ5fW/d0IaI/gC9zFANobf3mRC0Xy7CtCHdW9qTHe8i1hfoULbmlivGleX8LFz5z3cPg2VDeEZbQDvf0wJwohfVYbsILKtjg1o0WHwnqsVSNUXZJMiXlJ1E0SKMDQPBMH/u6Q4SP6ONMSZ44mphJygLW5Kjkpwd3uiczcn3ZYfFM2W++OuV6f4scvsMI7KFlqdQRXEbz1lVOEnK3dZ2pOLP0URLAZk487Iv1ZPl5TzupN+3cywL2YUIvIQux2APvNb7iFwz77yDvaf79X9LTeG8t/o/dNhTzGil4BMPQKcroE41mqvZXarmf',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF4MHTIJwMLDANwMPDARw',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=79355',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
    {
        id: 1839196,
        name: 'Люберцы',
        fullName: 'ЖК «Люберцы»',
        locativeFullName: 'в ЖК «Люберцы»',
        location: {
            ponds: [
                {
                    pondId: '164142041',
                    name: 'озеро Чёрное',
                    timeOnFoot: 60,
                    distanceOnFoot: 100,
                    latitude: 55.699017,
                    longitude: 37.953793,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 1,
                            distance: 100,
                        },
                    ],
                    pondType: 'LAKE',
                },
                {
                    pondId: '137677691',
                    name: 'река Пехорка',
                    timeOnFoot: 665,
                    distanceOnFoot: 923,
                    latitude: 55.694115,
                    longitude: 37.96121,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 11,
                            distance: 923,
                        },
                    ],
                    pondType: 'RIVER',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
                {
                    name: 'carsharing',
                    rgbColor: 'ffe424',
                    description: 'выше средней',
                    level: 4,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Московская',
                        regionType: 'SUBJECT_FEDERATION',
                        geoId: 1,
                        rgid: '587654',
                        valueForAddress: 'Московская область',
                        queryParams: {
                            rgid: '587654',
                            address: 'Россия, Московская область',
                        },
                    },
                    {
                        value: 'округ Люберцы',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        geoId: 98594,
                        rgid: '587697',
                        valueForAddress: 'Люберцы (городской округ)',
                        queryParams: {
                            rgid: '587697',
                            address: 'Россия, Московская область, Люберцы (городской округ)',
                        },
                    },
                    {
                        value: 'Люберцы',
                        regionType: 'CITY',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: 'Люберцы',
                        queryParams: {
                            rgid: '596066',
                            address: 'Россия, Московская область, Люберцы (городской округ), Люберцы',
                        },
                    },
                    {
                        value: 'улица Камова',
                        regionType: 'STREET',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: 'улица Камова',
                        queryParams: {
                            rgid: '596066',
                            address: 'Россия, Московская область, Люберцы (городской округ), Люберцы, улица Камова',
                            streetId: 91646,
                        },
                    },
                    {
                        value: '5к2',
                        regionType: 'HOUSE',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: '5к2',
                        queryParams: {
                            rgid: '596066',
                            address:
                                'Россия, Московская область, Люберцы (городской округ), Люберцы, улица Камова, 5к2',
                            streetId: 91646,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218432,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Некрасовка',
                    metroCityRgid: 587795,
                    timeToMetro: 19,
                },
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218431,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Лухмановская',
                    metroCityRgid: 587795,
                    timeToMetro: 25,
                },
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218430,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'ул. Дмитриевского',
                    metroCityRgid: 587795,
                    timeToMetro: 27,
                },
                {
                    lineColors: ['b1179a'],
                    metroGeoId: 115040,
                    rgbColor: 'b1179a',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Лермонтовский проспект',
                    metroCityRgid: 587795,
                    timeToMetro: 33,
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.71197,
                        longitude: 37.83749,
                        defined: true,
                    },
                    distance: 9629,
                },
            ],
            insideMKAD: false,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 3137,
                    distance: 29625,
                    latitude: 55.749893,
                    longitude: 37.623425,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 29625,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            point: {
                latitude: 55.6975,
                precision: 'EXACT',
                longitude: 37.954872,
            },
            parks: [],
            distanceFromRingRoad: 9629,
            geoId: 10738,
            populatedRgid: 596066,
            address: 'Люберцы, ул. Камова',
            rgid: 596066,
            subjectFederationName: 'Москва и МО',
            subjectFederationRgid: 741964,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            airports: [
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3175,
                    distanceOnCar: 35927,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 35927,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3434,
                    distanceOnCar: 50124,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 57,
                            distance: 50124,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 3727,
                    distanceOnCar: 58359,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 62,
                            distance: 58359,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 5385,
                    distanceOnCar: 58964,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 89,
                            distance: 58964,
                        },
                    ],
                },
            ],
            schools: [],
            settlementGeoId: 10738,
            metro: {
                lineColors: ['ff66e8'],
                metroGeoId: 218432,
                rgbColor: 'ff66e8',
                metroTransport: 'ON_TRANSPORT',
                name: 'Некрасовка',
                metroCityRgid: 587795,
                timeToMetro: 19,
            },
            settlementRgid: 596066,
            expectedMetroList: [],
        },
        viewTypes: [
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'GENERAL',
            'COURTYARD',
            'GENERAL',
            'GENPLAN',
        ],
        images: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_minicard',
        ],
        // siteSpecialProposals: [
        //     {
        //         proposalType: 'MORTGAGE',
        //         description: 'Ипотека 2.99%',
        //         mainProposal: true,
        //         specialProposalType: 'mortgage',
        //         shortDescription: 'Ипотека 2.99%',
        //     } as ISiteSpecialProposal,
        //     {
        //         proposalType: 'SALE',
        //         description: 'Трейд-ин',
        //         mainProposal: false,
        //         specialProposalType: 'sale',
        //         shortDescription: 'Трейд-ин',
        //     } as ISiteSpecialProposal,
        // ],
        buildingClass: 'COMFORT',
        state: 'UNFINISHED',
        finishedApartments: true,
        price: {
            from: 4100000,
            to: 13262255,
            currency: 'RUR',
            minPricePerMeter: 115588,
            maxPricePerMeter: 282970,
            averagePricePerMeter: 193450,
            rooms: {
                '1': {
                    soldout: false,
                    from: 4806602,
                    to: 8705124,
                    currency: 'RUR',
                    areas: {
                        from: '21.6',
                        to: '42.1',
                    },
                    hasOffers: true,
                    offersCount: 324,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '2': {
                    soldout: false,
                    from: 5552982,
                    to: 10567805,
                    currency: 'RUR',
                    areas: {
                        from: '31.3',
                        to: '56',
                    },
                    hasOffers: true,
                    offersCount: 335,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '3': {
                    soldout: false,
                    from: 6562568,
                    to: 10569362,
                    currency: 'RUR',
                    areas: {
                        from: '45.9',
                        to: '67.5',
                    },
                    hasOffers: true,
                    offersCount: 127,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    from: 4100000,
                    to: 6966760,
                    currency: 'RUR',
                    areas: {
                        from: '19.4',
                        to: '32.2',
                    },
                    hasOffers: true,
                    offersCount: 211,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                PLUS_4: {
                    soldout: false,
                    from: 11547403,
                    to: 13262255,
                    currency: 'RUR',
                    areas: {
                        from: '69.7',
                        to: '81.6',
                    },
                    hasOffers: true,
                    offersCount: 35,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
            },
            totalOffers: 1032,
            priceRatioToMarket: 0,
            minArea: 19.4,
            maxArea: 81.64,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '7158193',
                        meanPricePerM2: '183985',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '7204752',
                        meanPricePerM2: '185390',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '7214042',
                        meanPricePerM2: '186014',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '7268619',
                        meanPricePerM2: '187780',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 102320,
                name: 'Группа «Самолет»',
                legalName: 'ООО «СЗ «САМОЛЕТ ДЕВЕЛОПМЕНТ»',
                legalNames: ['ООО «СЗ «САМОЛЕТ ДЕВЕЛОПМЕНТ»'],
                url: 'http://samoletgroup.ru/',
                logo: '//avatars.mdst.yandex.net/get-realty/3022/company.102320.6062064445878226625/builder_logo_info',
                objects: {
                    all: 38,
                    salesOpened: 31,
                    finished: 14,
                    unfinished: 24,
                    suspended: 0,
                },
                address: 'Москва, улица Ивана Франко, 8',
                born: '2011-12-31T20:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJxNLTMN4MPzAR5',
                    },
                ],
            },
        ],
        description:
            'Проект «Люберцы» — настоящий город в городе: комплекс площадью 96.3 га строится в 7.5 км от МКАД на границе Москвы. Он включает 66 домов, Технологический лицей, детские сады, магазины, МФЦ. Вокруг комплекса — крупные зеленые зоны: берега реки Пехорки и озера Черного, Салтыковский лесопарк. Рядом — Зенинское шоссе с выездом на МКАД.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'serp',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'personalDefault',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            name: 'Абсолют Недвижимость',
            logo: '//avatars.mdst.yandex.net/get-realty/2899/company.1890036.7567954620118333462/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 1893290,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvbMElh3ciVsEIHsFjio+pewCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15eQZAA4R5Q9OzqNaykFVatB0Ix1uY7CPQIlz5xm/GWYWMiAgXkymXlt8yXLJQ4jKeo9XyUy5+8bHT727+/17fvOiDx4rdZOyK68OP/+zyFMZ01iErK46+WaOAqAljw3oMjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDSyAFxOpEs3kf221hiFpmqiaU3hfl9g7uFoBQeffYw+1EPN/SuZzesa2Tzer2yWyuJEIOVTzT/12aCfwh9qknlY39PuqcawGzvIFIhQ/3d8ItKnZK5YqAfQXdordb7xBwElyB60gf8pY5XPE5701rPZhTPz3Tpo1VEZru8sXPCS25sRUYITuxx1Bfp6tsf5yDBUcG9emswn0TlCo7siXnRpjzuNSsb4G9enoIjb9I0ZvF9mluBgR8P58+NKMpzGp4VWYeRMvJF5pE4HRC+u+qrqc5qw0O+eWUdx3EL3OJbuyTeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vrodWyOVPcFfcO4I81JagE8nEFWgTQt0RtPqq1ZAYTvgEg7jp2pt+QAs0/o+INiXYgr04qrlSPMBBIP4JhWuJjmqlS7N1i/47bf3uKHT+0ILkmjU7w13OSqeH3HmZQsyYyXjCtbwIlE8PoTXUHvDxUJyB0Ix1uY7CPQIlz5xm/GWYV01iErK46+WaOAqAljw3oMlLux99hmrfXXKxIvzvjm8k9Y/x7Ty8ohK934m2H0PGgvYU89Itgk+oKJ73PGZRwDx00IU409GdZXXDDGirFJj+wxDADuJexjwYNJBdxm65cKbGlkEQDbBcx4Nv+F8BPCVE584fNjxdqAFINHCV8+/8vMSg/v7KMfFWrFv5NdNpffmTXlHxC09Kg9lwiz6NBA4h42JH3WPg9UlTYguMeDFWJX7lYyDpwcjwekBTUq20azLPAymfDO+nh+ic2f2NE0ZnCjTLG7uE3BCC7qxYNi+YlNpbrIR9Q0wv9ggg2Ms2xrYiolbHgNi1G+NkE7Pml3eriYpMctO9BHEYz1znzuTanUum8PhuwqdwneUFs1+tnlAEiG7jU9igeJh/NaainWFLPi7gm1D7GU1m/vLFTftzPX7AJ5TTrdBTpO4qh2c0CDERtXKuCPbWltPIdZa0/dW7WqidTG8rk1leDG2kJssB0Ix1uY7CPQIlz5xm/GWYUv7JKuZnpzt+OF74DJXKp6giZHajOLpaK2w9bLKjxEvH2TGw0nVAd+dp1rhwKrmu13L5XZDqo1QJNL+oQZdU+x5LHMz4craOllegXylF0yKB3tsgZ4kWzLiSOkKMoKc0+bCVQ/DKV+dhB06o9EZcyA9wCzQFpZYUbL59Ej/ki47FtCAeF9gK36k1bKF1kbFkLCXN3g+uSD76HmQGqyyGhwa2IqJWx4DYtRvjZBOz5pd3q4mKTHLTvQRxGM9c587k11PNq2vfsJ8wSnYRnW8k4/d7YsTv861uhbCoZS3dE8J5/BfvOuqGTOhPZ8ef1XOqaVytzi6mja2DBj17VRJ3BSbhCsl/NpawM7qXQc/dwswlzVLJ5onqr1BJWH0hWmCdJd4hqGEChqNszsGgKqVkEPAo/n9dB2m/YuWS9ke0GZ+pS7sffYZq311ysSL8745vJMdxYBNpERAD1+VSYyz+mHJs08MmoXt1DzghWIp2fbbFS843d9gIa7fCHIvsW6fuoGP2DKYT8XoJMifMQAAOJzUeyf90dAUQb2zJQIvJEBDaOWObYfHnI+wr2Xq7bPSg0sErlbU1yBELUqg49YDe6MlZYNcSLL9M8EnWy05GRCdnTWISsrjr5Zo4CoCWPDegw370U51tcWYT7Jq8QzU+K1FuZcWhJrnbGfoJs+UgUHj2VI0nS9hV+/bvRlrJ2EuH4ytMvhPpb6hvdgCHjnPnGqlLux99hmrfXXKxIvzvjm8qOMWszfRPyETbfRJzTuQMfKOs1zjw4Sq6qFBRj0Qz/vk6rMm6LOW5S9srqvGU57UklKPhpySmYSATNEB7OmV7JhQCieiEq6SapeW0EgE0orrRkXwmN+0XVf6/c3GbwVJFu1qonUxvK5NZXgxtpCbLBT7K6Q72H4drO6BIk6m9pO0NWWVDIS0KTE58KOAdyMNB70jt0pXf8Z2XrI8bvPZq6W5N8r1j/c7D3eWuTysFYJQz3qcUJv0qzebcSpjonh5NYjogN3d7sp4scIqPLKgbTo/L3UXVOgcXR4PBQmCvIGDhgGQnOI+jRiN+39huCiEyng71M3FbrWYC6GBFWZLFDlAEiG7jU9igeJh/NaainWKms78dP8p/ZCKpqdlR/lbHR66Z/REozU7q+nfRdiZzaUIqVLmsFXjQawN+1fNX8Ptspb/wC1NCqeiVu5ClK3/XDhH9G5csfgK0GLl0t23njZcbgGIOsGCOOv+2B0ebz0U+H5tCwYwDPScXR7z8GPztPQayOGkY8wyf34aKxbx2iAhkJA9+56FCmssiHIF/37T8JLgi884TWrDfrfWr0RCElKPhpySmYSATNEB7OmV7JcbYW695CQr5AC3QcSEKjoDvjdBgmWe5H82H2f1jz6wtBydEgQ2B4hY6tvXgYl5Ug=',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'yVfgfJWp0anlAnCtc1gEBiF6IcMo6Z+EPoZtPjo7oEcoQuwqaL+0czf3NvHjTaX/duYmNAqULlR48fcZ6RWIe7vRb4Tq9YeRKcz+CgQB2xyYuL1sApx6M6c4ogLB+FzJD4vQEwi0xGWr2wKKd+lwZRyvdq1qcEN/QRQwCPSVozrUzdTXv3f9jMYd7clvwX4K93xFRfbZSmsKfyhzwLPwFvJlCYwlVdIsDORpvB4Q9uBGVi/pZNTR+67Zl4qaNjHFf6sOxSN85YVA9HC6TeUqUTL1j/yuzrSey8A6IzHTx0ARcy9qZaUiEPNpjR6epQCTUrykNiWv8BxsZb+hdchMKuqenWPMw+ZFmOVZg8TytWuA8WHITJWzPKO54wAzBZuTohh+73zEz+7GUUq0+jt83w3sjH0gFZAy+1qO4DUDgOGQ2vpE5hIUj1t2oSr+SN7E2023focyboBJkuzh9qiXpK2gKz3bFaNyjBASr1sryYRDQB5omUjQ7eEBPw9+QImRQlVA9D61HTRLpjP6K9PfDp3K1Wj62HYAWPfXdhILlqVDv3Uf1b+kqKnJ5H7Xxfm2H5Th3/wVA5xPpci2kOtez41DPsyfvokY0Vx1RQ+hlJvKrugXoiphRwg+BpfYI/Dc3rjoN0IhQRbcxropmc8BfZ9wAqo6jMy1GAvpXxXr5+5m2dg73vDyG9HUlPCMKVaopJvIVA5PKlmKCdXAWw3Q2gO9hgVoIXACziIbU7pbMfctK172sPB0eQI4/ppbwuFhPLHLRHEAv9o4KOJVP8CWTgkddssgYSuPV6SiW/ikaDY6El68nXKu7XJHReWuqtiSs3zcn06M5yIq8yqVbKnBfvGqXPuiyrLmj+u7rtbkJYpE0G7wIqHQkoyYj5S/+rClkbAcez3J9sxAU/iVarCVw9wRDYOkLcW/NPo=',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=1839196',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
];

export const oneNewbuilding = [
    {
        id: 1839196,
        name: 'Люберцы',
        fullName: 'ЖК «Люберцы»',
        locativeFullName: 'в ЖК «Люберцы»',
        location: {
            ponds: [
                {
                    pondId: '164142041',
                    name: 'озеро Чёрное',
                    timeOnFoot: 60,
                    distanceOnFoot: 100,
                    latitude: 55.699017,
                    longitude: 37.953793,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 1,
                            distance: 100,
                        },
                    ],
                    pondType: 'LAKE',
                },
                {
                    pondId: '137677691',
                    name: 'река Пехорка',
                    timeOnFoot: 665,
                    distanceOnFoot: 923,
                    latitude: 55.694115,
                    longitude: 37.96121,
                    timeDistanceList: [
                        {
                            transport: 'ON_FOOT',
                            time: 11,
                            distance: 923,
                        },
                    ],
                    pondType: 'RIVER',
                },
            ],
            allHeatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'price-rent',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена аренды',
                },
                {
                    name: 'price-sell',
                    rgbColor: '20a70a',
                    description: 'очень низкая',
                    level: 8,
                    maxLevel: 9,
                    title: 'Цена продажи',
                },
                {
                    name: 'profitability',
                    rgbColor: '128000',
                    description: 'очень высокая',
                    level: 9,
                    maxLevel: 9,
                    title: 'Прогноз окупаемости',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
                {
                    name: 'carsharing',
                    rgbColor: 'ffe424',
                    description: 'выше средней',
                    level: 4,
                    maxLevel: 8,
                    title: 'Доступность Яндекс.Драйва',
                },
            ],
            structuredAddress: {
                component: [
                    {
                        value: 'Россия',
                        regionType: 'COUNTRY',
                        geoId: 225,
                        rgid: '143',
                        valueForAddress: 'Россия',
                        queryParams: {
                            rgid: '143',
                            address: 'Россия',
                        },
                    },
                    {
                        value: 'Московская',
                        regionType: 'SUBJECT_FEDERATION',
                        geoId: 1,
                        rgid: '587654',
                        valueForAddress: 'Московская область',
                        queryParams: {
                            rgid: '587654',
                            address: 'Россия, Московская область',
                        },
                    },
                    {
                        value: 'округ Люберцы',
                        regionType: 'SUBJECT_FEDERATION_DISTRICT',
                        geoId: 98594,
                        rgid: '587697',
                        valueForAddress: 'Люберцы (городской округ)',
                        queryParams: {
                            rgid: '587697',
                            address: 'Россия, Московская область, Люберцы (городской округ)',
                        },
                    },
                    {
                        value: 'Люберцы',
                        regionType: 'CITY',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: 'Люберцы',
                        queryParams: {
                            rgid: '596066',
                            address: 'Россия, Московская область, Люберцы (городской округ), Люберцы',
                        },
                    },
                    {
                        value: 'улица Камова',
                        regionType: 'STREET',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: 'улица Камова',
                        queryParams: {
                            rgid: '596066',
                            address: 'Россия, Московская область, Люберцы (городской округ), Люберцы, улица Камова',
                            streetId: 91646,
                        },
                    },
                    {
                        value: '5к2',
                        regionType: 'HOUSE',
                        geoId: 10738,
                        rgid: '596066',
                        valueForAddress: '5к2',
                        queryParams: {
                            rgid: '596066',
                            address:
                                'Россия, Московская область, Люберцы (городской округ), Люберцы, улица Камова, 5к2',
                            streetId: 91646,
                        },
                    },
                ],
                unifiedOneline: '',
            },
            metroList: [
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218432,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Некрасовка',
                    metroCityRgid: 587795,
                    timeToMetro: 19,
                },
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218431,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Лухмановская',
                    metroCityRgid: 587795,
                    timeToMetro: 25,
                },
                {
                    lineColors: ['ff66e8'],
                    metroGeoId: 218430,
                    rgbColor: 'ff66e8',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'ул. Дмитриевского',
                    metroCityRgid: 587795,
                    timeToMetro: 27,
                },
                {
                    lineColors: ['b1179a'],
                    metroGeoId: 115040,
                    rgbColor: 'b1179a',
                    metroTransport: 'ON_TRANSPORT',
                    name: 'Лермонтовский проспект',
                    metroCityRgid: 587795,
                    timeToMetro: 33,
                },
            ],
            routeDistances: [
                {
                    geoPoint: {
                        latitude: 55.71197,
                        longitude: 37.83749,
                        defined: true,
                    },
                    distance: 9629,
                },
            ],
            insideMKAD: false,
            cityCenter: [
                {
                    transport: 'ON_CAR',
                    time: 3137,
                    distance: 29625,
                    latitude: 55.749893,
                    longitude: 37.623425,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 29625,
                        },
                    ],
                },
            ],
            subjectFederationId: 1,
            point: {
                latitude: 55.6975,
                precision: 'EXACT',
                longitude: 37.954872,
            },
            parks: [],
            distanceFromRingRoad: 9629,
            geoId: 10738,
            populatedRgid: 596066,
            address: 'Люберцы, ул. Камова',
            rgid: 596066,
            subjectFederationName: 'Москва и МО',
            subjectFederationRgid: 741964,
            heatmaps: [
                {
                    name: 'infrastructure',
                    rgbColor: 'f87c19',
                    description: 'минимальная',
                    level: 2,
                    maxLevel: 8,
                    title: 'Инфраструктура',
                },
                {
                    name: 'transport',
                    rgbColor: '620801',
                    description: 'низкая доступность',
                    level: 1,
                    maxLevel: 9,
                    title: 'Транспорт',
                },
            ],
            airports: [
                {
                    id: '878109',
                    name: 'Жуковский (Раменское)',
                    timeOnCar: 3175,
                    distanceOnCar: 35927,
                    latitude: 55.568665,
                    longitude: 38.143654,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 52,
                            distance: 35927,
                        },
                    ],
                },
                {
                    id: '858742',
                    name: 'Домодедово',
                    timeOnCar: 3434,
                    distanceOnCar: 50124,
                    latitude: 55.41435,
                    longitude: 37.90048,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 57,
                            distance: 50124,
                        },
                    ],
                },
                {
                    id: '878042',
                    name: 'Шереметьево',
                    timeOnCar: 3727,
                    distanceOnCar: 58359,
                    latitude: 55.963852,
                    longitude: 37.4169,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 62,
                            distance: 58359,
                        },
                    ],
                },
                {
                    id: '878065',
                    name: 'Внуково',
                    timeOnCar: 5385,
                    distanceOnCar: 58964,
                    latitude: 55.604942,
                    longitude: 37.282578,
                    timeDistanceList: [
                        {
                            transport: 'ON_CAR',
                            time: 89,
                            distance: 58964,
                        },
                    ],
                },
            ],
            schools: [],
            settlementGeoId: 10738,
            metro: {
                lineColors: ['ff66e8'],
                metroGeoId: 218432,
                rgbColor: 'ff66e8',
                metroTransport: 'ON_TRANSPORT',
                name: 'Некрасовка',
                metroCityRgid: 587795,
                timeToMetro: 19,
            },
            settlementRgid: 596066,
            expectedMetroList: [],
        },
        viewTypes: [
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'GENERAL',
            'COURTYARD',
            'GENERAL',
            'COURTYARD',
            'GENERAL',
            'GENPLAN',
        ],
        images: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_main',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_main',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_main',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_main',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_main',
        ],
        appLargeImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_large',
        ],
        appMiddleImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_middle',
        ],
        appMiddleSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_snippet_middle',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_snippet_middle',
        ],
        appLargeSnippetImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_app_snippet_large',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_app_snippet_large',
        ],
        minicardImages: [
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee200076cc33d90796bb192b5/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/216201/2a000001806ee3fe33282d334913ac051ec0/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee45b79fd5b93d69b490c4a40/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/1540742/2a000001806ee482e5905613be46b69ef8b3/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee49830a199cf96eec50aa8c5/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4971f94df649545d821128f/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4a0078efdb7e65b170be8a2/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/937147/2a000001806ee4ac311460e7ca008c2ea609/realty_minicard',
            '//avatars.mds.yandex.net/get-verba/997355/2a000001808eac8ad7a3fafe3b3f8ec60410/realty_minicard',
        ],
        // siteSpecialProposals: [
        //     {
        //         proposalType: 'MORTGAGE',
        //         description: 'Ипотека 2.99%',
        //         mainProposal: true,
        //         specialProposalType: 'mortgage',
        //         shortDescription: 'Ипотека 2.99%',
        //     } as ISiteSpecialProposal,
        //     {
        //         proposalType: 'SALE',
        //         description: 'Трейд-ин',
        //         mainProposal: false,
        //         specialProposalType: 'sale',
        //         shortDescription: 'Трейд-ин',
        //     } as ISiteSpecialProposal,
        // ],
        buildingClass: 'COMFORT',
        state: 'UNFINISHED',
        finishedApartments: true,
        price: {
            from: 4100000,
            to: 13262255,
            currency: 'RUR',
            minPricePerMeter: 115588,
            maxPricePerMeter: 282970,
            averagePricePerMeter: 193450,
            rooms: {
                '1': {
                    soldout: false,
                    from: 4806602,
                    to: 8705124,
                    currency: 'RUR',
                    areas: {
                        from: '21.6',
                        to: '42.1',
                    },
                    hasOffers: true,
                    offersCount: 324,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '2': {
                    soldout: false,
                    from: 5552982,
                    to: 10567805,
                    currency: 'RUR',
                    areas: {
                        from: '31.3',
                        to: '56',
                    },
                    hasOffers: true,
                    offersCount: 335,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                '3': {
                    soldout: false,
                    from: 6562568,
                    to: 10569362,
                    currency: 'RUR',
                    areas: {
                        from: '45.9',
                        to: '67.5',
                    },
                    hasOffers: true,
                    offersCount: 127,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                OPEN_PLAN: {
                    soldout: false,
                    currency: 'RUR',
                    hasOffers: false,
                    offersCount: 0,
                    priceRatioToMarket: 0,
                },
                STUDIO: {
                    soldout: false,
                    from: 4100000,
                    to: 6966760,
                    currency: 'RUR',
                    areas: {
                        from: '19.4',
                        to: '32.2',
                    },
                    hasOffers: true,
                    offersCount: 211,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
                PLUS_4: {
                    soldout: false,
                    from: 11547403,
                    to: 13262255,
                    currency: 'RUR',
                    areas: {
                        from: '69.7',
                        to: '81.6',
                    },
                    hasOffers: true,
                    offersCount: 35,
                    priceRatioToMarket: 0,
                    status: 'ON_SALE',
                },
            },
            totalOffers: 1032,
            priceRatioToMarket: 0,
            minArea: 19.4,
            maxArea: 81.64,
            newbuildingPriceStatisticsSeriesState: {
                state: 'is_present',
                series: [
                    {
                        meanPrice: '7158193',
                        meanPricePerM2: '183985',
                        timestamp: '2022-03-31T00:00:00Z',
                    },
                    {
                        meanPrice: '7204752',
                        meanPricePerM2: '185390',
                        timestamp: '2022-04-30T00:00:00Z',
                    },
                    {
                        meanPrice: '7214042',
                        meanPricePerM2: '186014',
                        timestamp: '2022-05-31T00:00:00Z',
                    },
                    {
                        meanPrice: '7268619',
                        meanPricePerM2: '187780',
                        timestamp: '2022-06-30T00:00:00Z',
                    },
                ],
            },
        },
        flatStatus: flatStatus.NOT_ON_SALE as FlatStatus,
        developers: [
            {
                id: 102320,
                name: 'Группа «Самолет»',
                legalName: 'ООО «СЗ «САМОЛЕТ ДЕВЕЛОПМЕНТ»',
                legalNames: ['ООО «СЗ «САМОЛЕТ ДЕВЕЛОПМЕНТ»'],
                url: 'http://samoletgroup.ru/',
                logo: '//avatars.mdst.yandex.net/get-realty/3022/company.102320.6062064445878226625/builder_logo_info',
                objects: {
                    all: 38,
                    salesOpened: 31,
                    finished: 14,
                    unfinished: 24,
                    suspended: 0,
                },
                address: 'Москва, улица Ивана Франко, 8',
                born: '2011-12-31T20:00:00Z',
                hasChat: false,
                encryptedPhones: [
                    {
                        phoneWithMask: '+7 ××× ××× ×× ××',
                        phoneHash: 'KzcF0OHTUJxNLTMN4MPzAR5',
                    },
                ],
            },
        ],
        description:
            'Проект «Люберцы» — настоящий город в городе: комплекс площадью 96.3 га строится в 7.5 км от МКАД на границе Москвы. Он включает 66 домов, Технологический лицей, детские сады, магазины, МФЦ. Вокруг комплекса — крупные зеленые зоны: берега реки Пехорки и озера Черного, Салтыковский лесопарк. Рядом — Зенинское шоссе с выездом на МКАД.',
        salesDepartment: {
            isRedirectPhones: true,
            phonesWithTag: [
                {
                    tag: 'default',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'mapsMobile',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'serp',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'personalDefault',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
                {
                    tag: 'HoneyPot',
                    phone: '+74951814542',
                    redirectId: '+74951814542',
                },
            ],
            weekTimetable: [
                {
                    dayFrom: 1,
                    dayTo: 7,
                    timePattern: [
                        {
                            open: '09:00',
                            close: '21:00',
                        },
                    ],
                },
            ],
            name: 'Абсолют Недвижимость',
            logo: '//avatars.mdst.yandex.net/get-realty/2899/company.1890036.7567954620118333462/builder_logo_info',
            timetableZoneMinutes: 180,
            id: 1893290,
            statParams:
                'lBe5lMmwQWWe63CMAjVmPLo/EOM+g236K7RFoSluCvbMElh3ciVsEIHsFjio+pewCmxpZBEA2wXMeDb/hfATwpXHkggcuvoPkAzODrs2lymljDgNA4byvlQ5CtqcGF4xRM4ZKFB/ED8FQO7C8BN2yh0Ix1uY7CPQIlz5xm/GWYXy0YPn+VRwmcflymgkTF15eQZAA4R5Q9OzqNaykFVatB0Ix1uY7CPQIlz5xm/GWYWMiAgXkymXlt8yXLJQ4jKeo9XyUy5+8bHT727+/17fvOiDx4rdZOyK68OP/+zyFMZ01iErK46+WaOAqAljw3oMjeUQONV5L8unFga5IvP8DxesLjrS2sWi5MDnOjUfTDSyAFxOpEs3kf221hiFpmqiaU3hfl9g7uFoBQeffYw+1EPN/SuZzesa2Tzer2yWyuJEIOVTzT/12aCfwh9qknlY39PuqcawGzvIFIhQ/3d8ItKnZK5YqAfQXdordb7xBwElyB60gf8pY5XPE5701rPZhTPz3Tpo1VEZru8sXPCS25sRUYITuxx1Bfp6tsf5yDBUcG9emswn0TlCo7siXnRpjzuNSsb4G9enoIjb9I0ZvF9mluBgR8P58+NKMpzGp4VWYeRMvJF5pE4HRC+u+qrqc5qw0O+eWUdx3EL3OJbuyTeomXQUtOsVPo0qfQT+mzv94PDGpxPQCZB+XCr8/vrodWyOVPcFfcO4I81JagE8nEFWgTQt0RtPqq1ZAYTvgEg7jp2pt+QAs0/o+INiXYgr04qrlSPMBBIP4JhWuJjmqlS7N1i/47bf3uKHT+0ILkmjU7w13OSqeH3HmZQsyYyXjCtbwIlE8PoTXUHvDxUJyB0Ix1uY7CPQIlz5xm/GWYV01iErK46+WaOAqAljw3oMlLux99hmrfXXKxIvzvjm8k9Y/x7Ty8ohK934m2H0PGgvYU89Itgk+oKJ73PGZRwDx00IU409GdZXXDDGirFJj+wxDADuJexjwYNJBdxm65cKbGlkEQDbBcx4Nv+F8BPCVE584fNjxdqAFINHCV8+/8vMSg/v7KMfFWrFv5NdNpffmTXlHxC09Kg9lwiz6NBA4h42JH3WPg9UlTYguMeDFWJX7lYyDpwcjwekBTUq20azLPAymfDO+nh+ic2f2NE0ZnCjTLG7uE3BCC7qxYNi+YlNpbrIR9Q0wv9ggg2Ms2xrYiolbHgNi1G+NkE7Pml3eriYpMctO9BHEYz1znzuTanUum8PhuwqdwneUFs1+tnlAEiG7jU9igeJh/NaainWFLPi7gm1D7GU1m/vLFTftzPX7AJ5TTrdBTpO4qh2c0CDERtXKuCPbWltPIdZa0/dW7WqidTG8rk1leDG2kJssB0Ix1uY7CPQIlz5xm/GWYUv7JKuZnpzt+OF74DJXKp6giZHajOLpaK2w9bLKjxEvH2TGw0nVAd+dp1rhwKrmu13L5XZDqo1QJNL+oQZdU+x5LHMz4craOllegXylF0yKB3tsgZ4kWzLiSOkKMoKc0+bCVQ/DKV+dhB06o9EZcyA9wCzQFpZYUbL59Ej/ki47FtCAeF9gK36k1bKF1kbFkLCXN3g+uSD76HmQGqyyGhwa2IqJWx4DYtRvjZBOz5pd3q4mKTHLTvQRxGM9c587k11PNq2vfsJ8wSnYRnW8k4/d7YsTv861uhbCoZS3dE8J5/BfvOuqGTOhPZ8ef1XOqaVytzi6mja2DBj17VRJ3BSbhCsl/NpawM7qXQc/dwswlzVLJ5onqr1BJWH0hWmCdJd4hqGEChqNszsGgKqVkEPAo/n9dB2m/YuWS9ke0GZ+pS7sffYZq311ysSL8745vJMdxYBNpERAD1+VSYyz+mHJs08MmoXt1DzghWIp2fbbFS843d9gIa7fCHIvsW6fuoGP2DKYT8XoJMifMQAAOJzUeyf90dAUQb2zJQIvJEBDaOWObYfHnI+wr2Xq7bPSg0sErlbU1yBELUqg49YDe6MlZYNcSLL9M8EnWy05GRCdnTWISsrjr5Zo4CoCWPDegw370U51tcWYT7Jq8QzU+K1FuZcWhJrnbGfoJs+UgUHj2VI0nS9hV+/bvRlrJ2EuH4ytMvhPpb6hvdgCHjnPnGqlLux99hmrfXXKxIvzvjm8qOMWszfRPyETbfRJzTuQMfKOs1zjw4Sq6qFBRj0Qz/vk6rMm6LOW5S9srqvGU57UklKPhpySmYSATNEB7OmV7JhQCieiEq6SapeW0EgE0orrRkXwmN+0XVf6/c3GbwVJFu1qonUxvK5NZXgxtpCbLBT7K6Q72H4drO6BIk6m9pO0NWWVDIS0KTE58KOAdyMNB70jt0pXf8Z2XrI8bvPZq6W5N8r1j/c7D3eWuTysFYJQz3qcUJv0qzebcSpjonh5NYjogN3d7sp4scIqPLKgbTo/L3UXVOgcXR4PBQmCvIGDhgGQnOI+jRiN+39huCiEyng71M3FbrWYC6GBFWZLFDlAEiG7jU9igeJh/NaainWKms78dP8p/ZCKpqdlR/lbHR66Z/REozU7q+nfRdiZzaUIqVLmsFXjQawN+1fNX8Ptspb/wC1NCqeiVu5ClK3/XDhH9G5csfgK0GLl0t23njZcbgGIOsGCOOv+2B0ebz0U+H5tCwYwDPScXR7z8GPztPQayOGkY8wyf34aKxbx2iAhkJA9+56FCmssiHIF/37T8JLgi884TWrDfrfWr0RCElKPhpySmYSATNEB7OmV7JcbYW695CQr5AC3QcSEKjoDvjdBgmWe5H82H2f1jz6wtBydEgQ2B4hY6tvXgYl5Ug=',
            encryptedPhones: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                },
            ],
            encryptedPhonesWithTag: [
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'default',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'mapsMobile',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'serp',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'personalDefault',
                },
                {
                    phoneWithMask: '+7 ××× ××× ×× ××',
                    phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
                    tag: 'HoneyPot',
                },
            ],
            encryptedDump:
                'yVfgfJWp0anlAnCtc1gEBiF6IcMo6Z+EPoZtPjo7oEcoQuwqaL+0czf3NvHjTaX/duYmNAqULlR48fcZ6RWIe7vRb4Tq9YeRKcz+CgQB2xyYuL1sApx6M6c4ogLB+FzJD4vQEwi0xGWr2wKKd+lwZRyvdq1qcEN/QRQwCPSVozrUzdTXv3f9jMYd7clvwX4K93xFRfbZSmsKfyhzwLPwFvJlCYwlVdIsDORpvB4Q9uBGVi/pZNTR+67Zl4qaNjHFf6sOxSN85YVA9HC6TeUqUTL1j/yuzrSey8A6IzHTx0ARcy9qZaUiEPNpjR6epQCTUrykNiWv8BxsZb+hdchMKuqenWPMw+ZFmOVZg8TytWuA8WHITJWzPKO54wAzBZuTohh+73zEz+7GUUq0+jt83w3sjH0gFZAy+1qO4DUDgOGQ2vpE5hIUj1t2oSr+SN7E2023focyboBJkuzh9qiXpK2gKz3bFaNyjBASr1sryYRDQB5omUjQ7eEBPw9+QImRQlVA9D61HTRLpjP6K9PfDp3K1Wj62HYAWPfXdhILlqVDv3Uf1b+kqKnJ5H7Xxfm2H5Th3/wVA5xPpci2kOtez41DPsyfvokY0Vx1RQ+hlJvKrugXoiphRwg+BpfYI/Dc3rjoN0IhQRbcxropmc8BfZ9wAqo6jMy1GAvpXxXr5+5m2dg73vDyG9HUlPCMKVaopJvIVA5PKlmKCdXAWw3Q2gO9hgVoIXACziIbU7pbMfctK172sPB0eQI4/ppbwuFhPLHLRHEAv9o4KOJVP8CWTgkddssgYSuPV6SiW/ikaDY6El68nXKu7XJHReWuqtiSs3zcn06M5yIq8yqVbKnBfvGqXPuiyrLmj+u7rtbkJYpE0G7wIqHQkoyYj5S/+rClkbAcez3J9sxAU/iVarCVw9wRDYOkLcW/NPo=',
        },
        phone: {
            phoneWithMask: '+7 ××× ××× ×× ××',
            phoneHash: 'KzcF0OHTUJxOLDEN0NPTQRy',
        },
        backCallTrafficInfo: {
            campaign: 'siteid=1839196',
        },
        withBilling: true,
        awards: {},
        limitedCard: false,
    },
];

export const links = {
    SELL_APARTMENT_NEW_FLAT_3_ROOM: {
        count: 809,
        params: {
            roomsTotal: ['3'],
            newFlat: ['YES'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_APARTMENT_NEW_FLAT: {
        count: 809,
        params: {
            newFlat: ['YES'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SITES_DECORATION_TURNKEY: {
        count: 245,
        params: {
            decoration: ['TURNKEY'],
            rgid: ['741964'],
        },
    },
    SITE_WITH_PARKING: {
        count: 529,
        params: {
            rgid: ['741964'],
            parkingType: ['OPEN'],
        },
    },
    SELL_APARTMENT_NEW_FLAT_1_ROOM: {
        count: 809,
        params: {
            roomsTotal: ['1'],
            newFlat: ['YES'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_APARTMENT_NEW_FLAT_2_ROOM: {
        count: 809,
        params: {
            roomsTotal: ['2'],
            newFlat: ['YES'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_LOT: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['LOT'],
        },
    },
    SITES: {
        count: 809,
        params: {
            rgid: ['741964'],
        },
    },
    SELL_APARTMENT_3: {
        count: 809,
        params: {
            roomsTotal: ['3'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_ROOM: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['ROOMS'],
        },
    },
    SELL_GARAGE: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['GARAGE'],
        },
    },
    SITE_MATKAPITAL: {
        count: 254,
        params: {
            rgid: ['741964'],
            hasSiteMaternityFunds: ['YES'],
        },
    },
    SELL_APARTMENT_NEW_FLAT_PLUS_4_ROOM: {
        count: 809,
        params: {
            roomsTotal: ['PLUS_4'],
            newFlat: ['YES'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SITE_ECONOMY: {
        count: 115,
        params: {
            rgid: ['741964'],
            buildingClass: ['ECONOM'],
        },
    },
    SITES_DECORATION_CLEAN: {
        count: 205,
        params: {
            decoration: ['CLEAN'],
            rgid: ['741964'],
        },
    },
    SITE_PREMIUM: {
        count: 168,
        params: {
            rgid: ['741964'],
            buildingClass: ['ELITE'],
        },
    },
    SITE_APARTMENTS: {
        count: 146,
        params: {
            apartments: ['YES'],
            rgid: ['741964'],
        },
    },
    SITE_MORTGAGE: {
        count: 1,
        params: {
            rgid: ['741964'],
            hasSiteMortgage: ['YES'],
        },
    },
    SITE_214_FZ: {
        count: 809,
        params: {
            rgid: ['741964'],
            dealType: ['FZ_214'],
        },
    },
    SELL_COMMERCIAL_OFFICE: {
        count: 809,
        params: {
            commercialType: ['OFFICE'],
            rgid: ['741964'],
            category: ['COMMERCIAL'],
            type: ['SELL'],
        },
    },
    SITES_NEAR_METRO: {
        count: 228,
        params: {
            metroTransport: ['ON_FOOT'],
            rgid: ['741964'],
            timeToMetro: ['10'],
        },
    },
    SITES_COMMISSION_THIS_QUARTER: {
        count: 591,
        params: {
            rgid: ['741964'],
            deliveryDate: ['3_2022'],
        },
    },
    SELL_APARTMENT_2: {
        count: 809,
        params: {
            roomsTotal: ['2'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SITE_BUSINESS: {
        count: 221,
        params: {
            rgid: ['741964'],
            buildingClass: ['BUSINESS'],
        },
    },
    SITES_DISCOUNT: {
        count: 127,
        params: {
            hasSpecialProposal: ['YES'],
            rgid: ['741964'],
        },
    },
    SELL_APARTMENT_OWNER: {
        count: 809,
        params: {
            agents: ['NO'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_APARTMENT: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['APARTMENT'],
        },
    },
    SELL_APARTMENT_STUDIO: {
        count: 809,
        params: {
            roomsTotal: ['STUDIO'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SITE_MILITARY_MORTGAGE: {
        count: 1,
        params: {
            hasMilitarySiteMortgage: ['YES'],
            rgid: ['741964'],
        },
    },
    SELL_COMMERCIAL: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['COMMERCIAL'],
        },
    },
    SELL_COMMERCIAL_FREE_PURPOSE: {
        count: 809,
        params: {
            commercialType: ['FREE_PURPOSE'],
            rgid: ['741964'],
            category: ['COMMERCIAL'],
            type: ['SELL'],
        },
    },
    SITE_INSTALLMENT: {
        count: 109,
        params: {
            rgid: ['741964'],
            hasInstallment: ['YES'],
        },
    },
    SELL_APARTMENT_1: {
        count: 809,
        params: {
            roomsTotal: ['1'],
            rgid: ['741964'],
            category: ['APARTMENT'],
            type: ['SELL'],
        },
    },
    SELL_HOUSE: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['HOUSE'],
        },
    },
};

export const fewLinks = {
    SELL_GARAGE: {
        count: 809,
        params: {
            type: ['SELL'],
            rgid: ['741964'],
            category: ['GARAGE'],
        },
    },
};

export const context = {
    region: {
        rgid: '741964',
        name: 'Москва и МО',
        locativeName: 'в Москве и МО',
    },
    offerType: 'SELL',
};

export const totalCount = 3236;
export const seoTagsCategories = ['apartment', 'commercial', 'garage', 'sites', 'room'];

export const initialState = {
    geo,
    searchCategories: {
        newbuildings,
        links,
        context,
        totalCount,
        seoTagsCategories,
    },
};

export const initialStateWithoutLinks = {
    geo,
    searchCategories: {
        newbuildings,
        links: {},
        context,
        totalCount,
        seoTagsCategories,
    },
};

export const initialStateWithoutNewbuildings = {
    geo,
    searchCategories: {
        newbuildings: [],
        links,
        context,
        totalCount,
        seoTagsCategories,
    },
};

export const initialStateWithoutNewbuildingsAndWithFewLinks = {
    geo,
    searchCategories: {
        links: fewLinks,
        newbuildings: [],
        context,
        totalCount,
        seoTagsCategories,
    },
};

export const initialStateWithOneNewbuilding = {
    geo,
    searchCategories: {
        links,
        newbuildings: oneNewbuilding,
        context,
        totalCount,
        seoTagsCategories,
    },
};
