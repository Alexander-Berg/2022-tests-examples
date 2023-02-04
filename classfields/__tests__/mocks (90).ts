import { ISeoOfferLinksOwnProps } from '../index';

export const links = [
    'RENT_LONG_APARTMENT',
    'RENT_LONG_APARTMENT_STUDIO',
    'RENT_LONG_APARTMENT_1',
    'RENT_LONG_APARTMENT_2',
    'RENT_LONG_APARTMENT_3',
    'RENT_LONG_APARTMENT_OWNER',
].reduce((result, key, index) => {
    result[key] = {
        count: (index || 1) * 100,
        params: {},
    };

    return result;
}, {});

export const baseProps: ISeoOfferLinksOwnProps = {
    block: 'rentApartment',
    getUrlParams: () => ({}),
    links,
    placeName: 'Название блока',
};

export const commercialBaseProps: ISeoOfferLinksOwnProps = {
    block: 'rentCommercial',
    getUrlParams: () => ({}),
    links: {
        RENT_COMMERCIAL: {
            count: 56,
            params: {},
        },
        RENT_COMMERCIAL_FREE_PURPOSE: {
            count: 56,
            params: {},
        },
    },
    placeName: 'Название блока',
};

export const defaultTitleProps: ISeoOfferLinksOwnProps = {
    block: 'rentApartment',
    placeName: '',
    links,
    getUrlParams: () => ({}),
};
