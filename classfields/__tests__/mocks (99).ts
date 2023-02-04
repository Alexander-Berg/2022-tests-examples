import { ApartmentType } from 'realty-core/types/siteCard';
import { SitePlansSortTypes } from 'realty-core/types/sitePlans';

import { ISitePlansV2SerpProps } from '../';

export const getSiteCard = (apartmentType: ApartmentType) => ({
    buildingFeatures: {
        apartmentType,
    },
});

const getPlans = () => [
    {
        roomType: 'STUDIO',
        roomCount: 0,
        wholeArea: {
            value: 19.7,
            unit: 'SQ_M',
        },
        livingArea: {
            value: 9.4,
            unit: 'SQ_M',
        },
        images: {},
        clusterId: '375274-1200-8F0B651CDE02929A',
        floors: [2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16],
        commissioningDate: [
            {
                year: 2023,
                quarter: 2,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 3,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
        ],
        pricePerOffer: {
            currency: 'RUB',
            from: '5811500',
            to: '6422200',
        },
        pricePerMeter: {
            currency: 'RUB',
            from: '294999',
            to: '325999',
        },
        offersCount: 1,
        offerId: '6258444696742167171',
    },
    {
        roomType: 'STUDIO',
        roomCount: 0,
        wholeArea: {
            value: 21.5,
            unit: 'SQ_M',
        },
        livingArea: {
            value: 10.3,
            unit: 'SQ_M',
        },
        images: {},
        clusterId: '375274-1200-E6D72ED0C12B3EFC',
        floors: [3, 5, 7, 9, 11, 13, 15, 17],
        commissioningDate: [
            {
                year: 2023,
                quarter: 1,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 2,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 3,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
        ],
        pricePerOffer: {
            currency: 'RUB',
            from: '5873800',
            to: '7030500',
        },
        pricePerMeter: {
            currency: 'RUB',
            from: '273200',
            to: '327000',
        },
        offersCount: 2,
        offerId: '5444798587497977345',
    },
    {
        roomType: 'STUDIO',
        roomCount: 0,
        wholeArea: {
            value: 22.1,
            unit: 'SQ_M',
        },
        livingArea: {
            value: 10.3,
            unit: 'SQ_M',
        },
        images: {},
        clusterId: '375274-1200-BA5E93B656127842',
        floors: [2, 4, 6, 8, 10, 12, 14, 16],
        commissioningDate: [
            {
                year: 2023,
                quarter: 1,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 2,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
            {
                year: 2023,
                quarter: 3,
                constructionState: 'CONSTRUCTION_STATE_UNFINISHED',
            },
        ],
        pricePerOffer: {
            currency: 'RUB',
            from: '6110650',
            to: '7226700',
        },
        pricePerMeter: {
            currency: 'RUB',
            from: '276500',
            to: '327000',
        },
        offersCount: 10,
        offerId: '5444614363033890089',
    },
];

export const getProps = (apartmentType: ApartmentType) => {
    const plans = getPlans();

    return ({
        card: getSiteCard(apartmentType),
        plans,
        totalPlans: plans.length,
        sort: SitePlansSortTypes.AREA_DESC,
    } as unknown) as ISitePlansV2SerpProps;
};
