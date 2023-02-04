import { IItemAddressProps } from '../index';

export const baseProps = {
    className: 'SitesSerpItem__address',
    item: {
        location: {
            address: 'Москва, мкр. Бунинский, ул. Александры Монаховой',
            distanceFromRingRoad: 9197,
            subjectFederationRgid: 741964,
        },
    },
    preferStreetAddress: false,
} as IItemAddressProps;

export const basePropsWithUnifiedLocation = ({
    ...baseProps,
    item: {
        location: {
            address: 'Россия, Саратов, Шелковичная улица, 186',
            subjectFederationRgid: 574385,
        },
        unifiedLocation: {
            rgid: 574385,
            region: 'Саратовская область',
            district: 'городской округ Саратов',
            localityName: 'Саратов',
            subLocalityName: 'Фрунзенский район',
            shortAddress: 'Шелковичная улица, 186',
            subjectFederationId: 11146,
            street: 'Шелковичная улица',
            houseNumber: 186,
        },
    },
} as unknown) as IItemAddressProps;

export const basePropsWithoutDataAndPlaceholder = {
    className: 'SitesSerpItem__address',
    item: {
        location: {},
        unifiedLocation: {},
    },
    preferStreetAddress: false,
} as IItemAddressProps;

export const basePropsWithExternalPlaceholder = {
    className: 'SitesSerpItem__address',
    item: {
        location: {},
        unifiedLocation: {},
    },
    placeholder: 'Адрес отсутствует',
    preferStreetAddress: false,
} as IItemAddressProps;
