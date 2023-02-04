import { ISeoOfferLinksBlockOwnProps } from '../index';

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

export const baseProps: ISeoOfferLinksBlockOwnProps = {
    block: 'rentApartment',
    getUrlParams: () => ({}),
    links,
    placeName: 'Название блока',
};

export const defaultTitleProps: ISeoOfferLinksBlockOwnProps = {
    block: 'rentApartment',
    placeName: '',
    links,
    getUrlParams: () => ({}),
};
