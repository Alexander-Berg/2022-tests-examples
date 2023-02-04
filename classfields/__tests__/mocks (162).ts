import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

import { LINK_BLOCKS } from 'realty-core/view/react/modules/seo-links/constants/offers-link-blocks';
import { ISeoLinkItem } from 'realty-core/types/seo-links';

import { IStore } from 'view/react/deskpad/reducers/roots/metro-stations';

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

const metroStationBaseStore = {
    newbuildings,
    links,
    context: {
        region: {
            rgid: '741964',
            name: 'Москва и МО',
            locativeName: 'в Москве и МО',
        },
        metro: {
            id: 20476,
            name: 'Площадь Ильича',
            locativeName: 'на Площади Ильича',
        },
    },
    totalCount: 123,
    seoTagsCategories,
} as const;

export const baseInitialState: Partial<IStore> = {
    metroStation: metroStationBaseStore,
};

export const initialStateWithoutNewbuildings: Partial<IStore> = {
    ...baseInitialState,
    metroStation: {
        ...metroStationBaseStore,
        newbuildings: [],
    },
};

export const initialStateWithoutLinks: Partial<IStore> = {
    ...baseInitialState,
    metroStation: {
        ...metroStationBaseStore,
        links: {},
    },
};

export const initialStateWitOneNewbuilding: Partial<IStore> = {
    ...baseInitialState,
    metroStation: {
        ...metroStationBaseStore,
        newbuildings: metroStationBaseStore.newbuildings.slice(0, 1),
    },
};

export const initialStateWithoutNewbuildingsAndWithLowLinks: Partial<IStore> = {
    ...baseInitialState,
    metroStation: {
        ...metroStationBaseStore,
        newbuildings: [],
        links: {
            SELL_HOUSE: links['SELL_HOUSE'],
            SELL_LOT: links['SELL_LOT'],
        },
    },
};
