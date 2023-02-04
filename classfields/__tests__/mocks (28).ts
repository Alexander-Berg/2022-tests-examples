import { OfferWarningsTypes } from 'types/offer-warnings';

export const withoutWarnings = {
    warnings: [],
    editUrl: 'mock',
};

export const withDiscriminationMock = {
    warnings: [{ warning: OfferWarningsTypes.DISCRIMINATION }],
    editUrl: '',
};
