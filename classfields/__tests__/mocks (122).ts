import { newbuildingOffer, houseOffer, rentOffer } from '../../__tests__/stubs/offer';

export { newbuildingOffer, houseOffer };

export const state = {
    geo: {
        rgid: 42,
    },
};

export const rentOfferWithAllFeatures = {
    ...rentOffer,
    rentConditionsMap: {
        PETS: true,
        CHILDREN: true,
    },
};
