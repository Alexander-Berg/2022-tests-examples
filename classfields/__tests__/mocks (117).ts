import { ISiteCard } from 'realty-core/types/siteCard';

import { newbuildingOffer, secondaryOffer } from '../../__tests__/stubs/offer';

export { newbuildingOffer, secondaryOffer };

export const state = {
    geo: {
        rgid: 42,
    },
    offerCard: {},
};

export const site = ({
    buildingFeatures: {
        totalFloors: 24,
        minTotalFloors: 10,
    },
} as unknown) as ISiteCard;

export const siteWithoutFloorsGap = ({
    buildingFeatures: {
        totalFloors: 24,
        minTotalFloors: 24,
    },
} as unknown) as ISiteCard;
