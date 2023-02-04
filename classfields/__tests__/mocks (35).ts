import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { LINK_BLOCKS } from 'realty-core/view/react/modules/seo-links/constants/offers-link-blocks';

import { ISeoLinkItem } from 'realty-core/types/seo-links';

import { IDistrictPageStore } from 'view/reducers/pages/DistrictPage';

const linkStrings: string[] = [];

// Подцепляем все алиасы из LINK_BLOCKS
Object.values(LINK_BLOCKS).forEach((item) => {
    if (item.titleLink) {
        linkStrings.push(item.titleLink);
    }
    item.subLinks.forEach((link) => linkStrings.push(link));
});

const links: Record<string, ISeoLinkItem> = linkStrings.reduce((result, key, index) => {
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

const seoTagsCategories = ['apartment', 'room', 'commercial', 'garage'];

const districtBaseStore = {
    newbuildings,
    links,
    context: {
        region: {
            rgid: '741964',
            name: 'Москва и МО',
            locativeName: 'в Москве и МО',
        },
        district: {
            id: 193324,
            name: 'Арбат',
            locativeName: 'в Арбате',
        },
    },
    totalCount: 123,
    seoTagsCategories,
} as const;

export const baseInitialState: Partial<IDistrictPageStore> = {
    district: districtBaseStore,
};

export const initialStateWithoutNewbuildings: Partial<IDistrictPageStore> = {
    ...baseInitialState,
    district: {
        ...districtBaseStore,
        newbuildings: [],
    },
};

export const initialStateWithoutLinks: Partial<IDistrictPageStore> = {
    ...baseInitialState,
    district: {
        ...districtBaseStore,
        links: {},
    },
};

export const initialStateWitOneNewbuilding: Partial<IDistrictPageStore> = {
    ...baseInitialState,
    district: {
        ...districtBaseStore,
        newbuildings: districtBaseStore.newbuildings.slice(0, 1),
    },
};

export const initialStateWithoutNewbuildingsAndWithLowLinks: Partial<IDistrictPageStore> = {
    ...baseInitialState,
    district: {
        ...districtBaseStore,
        newbuildings: [],
        links: {
            SELL_HOUSE: links['SELL_HOUSE'],
            SELL_LOT: links['SELL_LOT'],
        },
    },
};
