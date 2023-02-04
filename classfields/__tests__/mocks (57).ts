import { HouseType, IGenPlanBaseHouse } from 'realty-core/view/react/modules/gen-plan/types';

export const houseInfo = {
    finished: true,
    year: 2022,
    phaseName: '1 очередь',
    id: '1859908',
    buildingSiteName: 'к. 1',
    hasPlans: true,
    commissioningDate: {
        year: 2022,
        quarter: 4,
        constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
    },
    maxFloor: 8,
} as IGenPlanBaseHouse;

export const house = {
    loading: false,
    rooms: [
        {
            roomType: '1',
            priceFrom: '7849324',
            priceTo: '7997676',
            areaFrom: 41.27,
            areaTo: 42.05,
            flatPlansCount: 2,
            offersCount: 2,
        },
        {
            roomType: '2',
            priceFrom: '8241136',
            priceTo: '9823960',
            areaFrom: 47.27,
            areaTo: 59.17,
            flatPlansCount: 12,
            offersCount: 20,
        },
        {
            roomType: '3',
            priceFrom: '9316535',
            priceTo: '11009272',
            areaFrom: 61.56,
            areaTo: 64.58,
            flatPlansCount: 5,
            offersCount: 13,
        },
    ],
} as HouseType;
