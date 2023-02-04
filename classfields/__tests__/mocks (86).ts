import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISamoletSitePlan } from 'realty-core/types/samolet/plan';

export const plan1 = ({
    roomType: '3',
    roomCount: 3,
    wholeArea: { value: 89.2, unit: 'SQ_M' },
    livingArea: { value: 47.5, unit: 'SQ_M' },
    kitchenArea: { value: 21.5, unit: 'SQ_M' },
    images: {
        large: generateImageUrl({ width: 768, height: 768 }),
    },
    clusterId: '299766-0A020803-40F1989E932E9CDA',
    floors: [5, 12, 15, 17, 24],
    commissioningDate: [
        { year: 2021, quarter: 4, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
        { year: 2023, quarter: 1, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
    ],
    pricePerOffer: { currency: 'RUB', from: '9410600', to: '12728840' },
    pricePerMeter: { currency: 'RUB', from: '105500', to: '142700' },
    offersCount: 5,
    offerId: '2650051711357903040',
    site: {
        name: 'Томилино Парк',
    },
} as unknown) as ISamoletSitePlan;

export const plan2 = ({
    roomType: 'STUDIO',
    roomCount: 0,
    wholeArea: { value: 19.5, unit: 'SQ_M' },
    livingArea: { value: 9.4, unit: 'SQ_M' },
    images: {},
    clusterId: '299766-1200-92C1D37ACEC2D3F5',
    floors: [4],
    commissioningDate: [{ year: 2020, quarter: 4, constructionState: 'CONSTRUCTION_STATE_FINISHED' }],
    pricePerOffer: { currency: 'RUB', from: '6064500', to: '6064500' },
    pricePerMeter: { currency: 'RUB', from: '311000', to: '311000' },
    offersCount: 1,
    offerId: '2750765349514123525',
    site: {
        name: 'Томилино Парк',
    },
} as unknown) as ISamoletSitePlan;

export const planApart = ({
    roomType: '3',
    roomCount: 3,
    wholeArea: { value: 89.2, unit: 'SQ_M' },
    livingArea: { value: 47.5, unit: 'SQ_M' },
    kitchenArea: { value: 21.5, unit: 'SQ_M' },
    images: {
        large: generateImageUrl({ width: 768, height: 768 }),
    },
    clusterId: '299766-0A020803-40F1989E932E9CDA',
    floors: [5, 12, 15, 17, 24],
    commissioningDate: [
        { year: 2021, quarter: 4, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
        { year: 2023, quarter: 1, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
    ],
    pricePerOffer: { currency: 'RUB', from: '9410600', to: '12728840' },
    pricePerMeter: { currency: 'RUB', from: '105500', to: '142700' },
    offersCount: 5,
    offerId: '2650051711357903040',
    site: {
        name: 'Томилино Парк',
        buildingFeatures: {
            isApartment: true,
        },
    },
} as unknown) as ISamoletSitePlan;

export const planMix = ({
    roomType: '3',
    roomCount: 3,
    wholeArea: { value: 89.2, unit: 'SQ_M' },
    livingArea: { value: 47.5, unit: 'SQ_M' },
    kitchenArea: { value: 21.5, unit: 'SQ_M' },
    images: {
        large: generateImageUrl({ width: 768, height: 768 }),
    },
    clusterId: '299766-0A020803-40F1989E932E9CDA',
    floors: [5, 12, 15, 17, 24],
    commissioningDate: [
        { year: 2021, quarter: 4, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
        { year: 2023, quarter: 1, constructionState: 'CONSTRUCTION_STATE_UNFINISHED' },
    ],
    pricePerOffer: { currency: 'RUB', from: '9410600', to: '12728840' },
    pricePerMeter: { currency: 'RUB', from: '105500', to: '142700' },
    offersCount: 5,
    offerId: '2650051711357903040',
    site: {
        name: 'Томилино Парк',
        buildingFeatures: {
            apartmentType: 'APARTMENTS_AND_FLATS',
        },
    },
} as unknown) as ISamoletSitePlan;
