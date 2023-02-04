import { newbuildingOffer } from '../../__tests__/stubs/offer';

export const CURRENT_DATE_TIME = '2021-03-17T10:00:00.111Z';

export { newbuildingOffer as offer };

const nowDateTime = new Date(CURRENT_DATE_TIME).getTime();

export const hourBeforeEditedOffer = {
    ...newbuildingOffer,
    updateDate: new Date(nowDateTime - 60 * 60 * 1000).toISOString(),
};

export const yesterdayEditedOffer = {
    ...newbuildingOffer,
    updateDate: new Date(nowDateTime - 24 * 60 * 60 * 1000).toISOString(),
};

export const hourBeforeCreatedOffer = {
    ...newbuildingOffer,
    creationDate: new Date(nowDateTime - 60 * 60 * 1000).toISOString(),
    updateDate: undefined,
};

export const yesterdayCreatedOffer = {
    ...newbuildingOffer,
    creationDate: new Date(nowDateTime - 24 * 60 * 60 * 1000).toISOString(),
    updateDate: undefined,
};

export const offerWithoutViews = {
    ...newbuildingOffer,
    views: undefined,
};

export const baseState = {
    user: {
        favoritesMap: {},
    },
};

export const stateWithOfferInFavorites = {
    user: {
        favoritesMap: { [newbuildingOffer.offerId]: true },
    },
};

export const onActionsClick = () => null;
