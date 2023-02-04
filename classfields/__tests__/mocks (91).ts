import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { ISeoSiteLinksOwnProps } from '../index';

const links = [
    'SITES',
    'SITES_NEAR_METRO',
    'SITES_DECORATION_CLEAN',
    'SITES_DECORATION_TURNKEY',
    'SITES_COMMISSION_THIS_QUARTER',
    'SITES_DISCOUNT',
].reduce((result, key, index) => {
    result[key] = {
        count: (index || 1) * 100,
        params: {},
    };

    return result;
}, {});

const newbuildings = [
    {
        id: 424783,
        name: 'The Mostman',
        fullName: 'ЖК «The Mostman»',
        price: {
            from: 30225600,
            currency: 'RUR',
        },
        location: {
            rgid: 193318,
            metro: {
                lineColors: ['ffe400'],
                metroGeoId: 20476,
                rgbColor: 'ffe400',
                metroTransport: 'ON_FOOT',
                name: 'Площадь Ильича',
                timeToMetro: 8,
            },
        },
    },
    {
        id: 200202,
        name: 'Символ',
        fullName: 'Квартал «Символ»',
        price: {
            from: 7638480,
            currency: 'RUR',
        },
        location: {
            rgid: 193388,
            metro: {
                lineColors: ['ffe400'],
                metroGeoId: 20476,
                rgbColor: 'ffe400',
                metroTransport: 'ON_TRANSPORT',
                name: 'Площадь Ильича',
                timeToMetro: 17,
            },
        },
    },
    {
        id: 659996,
        name: 'Лефортово парк',
        fullName: 'жилой комплекс «Лефортово парк»',
        price: {
            from: 9638480,
            currency: 'RUR',
        },
        location: {
            rgid: 350822,
            metro: {
                lineColors: ['69cd37'],
                metroGeoId: 20514,
                rgbColor: '69cd37',
                metroTransport: 'ON_TRANSPORT',
                name: 'Римская',
                timeToMetro: 18,
            },
        },
    },
] as ISiteSnippetType[];

export const baseProps: ISeoSiteLinksOwnProps = {
    getUrlParams: () => ({}),
    links,
    placeName: 'Название блока',
    sites: newbuildings,
};

export const defaultTitleProps: ISeoSiteLinksOwnProps = {
    links,
    getUrlParams: () => ({}),
    sites: newbuildings,
    placeName: '',
};
