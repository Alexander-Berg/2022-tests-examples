import { IOfferCard } from 'realty-core/types/offerCard';

import { IGetOfferFavoriteStats, IRequiredStore } from '../types';

export const offer = ({
    offerId: '1',
} as unknown) as IOfferCard;

export const getStats: IGetOfferFavoriteStats = () => ({});

export const state = {
    user: {
        favoritesMap: {},
    },
} as IRequiredStore;

export const stateWithOfferInFavorites = ({
    user: {
        favoritesMap: {
            '1': true,
        },
    },
} as unknown) as IRequiredStore;
