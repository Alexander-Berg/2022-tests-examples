import { IOfferCard } from 'realty-core/types/offerCard';

import { newbuildingOffer, rentOffer, secondaryOffer } from '../../__tests__/stubs/offer';
import { alfaBankMortgageLoaded, alfaBankMortgageInitial } from '../../__tests__/stubs/alfabank';

export { newbuildingOffer, rentOffer, secondaryOffer };
export { alfaBankMortgageLoaded, alfaBankMortgageInitial };

export const disabledNewBuildingOffer = {
    ...newbuildingOffer,
    active: false,
};

export const apartmentsOffer = {
    ...newbuildingOffer,
    house: {
        ...newbuildingOffer.house,
        apartments: true,
    },
};

export const goodPriceNewbuildingOffer = ({
    ...newbuildingOffer,
    predictions: {
        predictedPriceAdvice: {
            summary: 'LOW',
        },
    },
} as unknown) as IOfferCard;

export const severalMainBadgesNewbuildingOffer = ({
    ...newbuildingOffer,
    active: false,
    predictions: {
        predictedPriceAdvice: {
            summary: 'LOW',
        },
    },
} as unknown) as IOfferCard;

export const disabledSecondaryOffer = {
    ...secondaryOffer,
    active: false,
};

export const goodPriceSecondaryOffer = ({
    ...secondaryOffer,
    predictions: {
        predictedPriceAdvice: {
            summary: 'LOW',
        },
    },
} as unknown) as IOfferCard;

export const severalMainBadgesSecondaryOffer = ({
    ...secondaryOffer,
    active: false,
    predictions: {
        predictedPriceAdvice: {
            summary: 'LOW',
        },
    },
} as unknown) as IOfferCard;

export const yaRentOffer = {
    ...rentOffer,
    yandexRent: true,
};

export const disabledRentOffer = {
    ...rentOffer,
    active: false,
};

export const severalMainBadgesRentOffer = ({
    ...rentOffer,
    active: false,
    yandexRent: true,
    utilitiesIncluded: true,
} as unknown) as IOfferCard;
