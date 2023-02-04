import { newbuildingOffer as offer } from '../../__tests__/stubs/offer';

export { offer };

export const state = {
    user: {
        crc: '123',
        favoritesMap: {},
    },
    offerCard: {
        card: offer,
    },
};
