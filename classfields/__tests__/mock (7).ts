import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

const rooms = {
    '1': {
        soldout: false,
        from: 13653000,
        to: 25687160,
        currency: 'RUR',
        areas: {
            from: '35.4',
            to: '48.4',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
    '2': {
        soldout: false,
        from: 16306830,
        to: 37052340,
        currency: 'RUR',
        areas: {
            from: '44',
            to: '88.7',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
    '3': {
        soldout: false,
        from: 30612800,
        to: 56781120,
        currency: 'RUR',
        areas: {
            from: '75.3',
            to: '113.2',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
    OPEN_PLAN: {
        soldout: false,
        from: 9214960,
        to: 41783848,
        currency: 'RUR',
        areas: {
            from: '21.5',
            to: '77',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
    STUDIO: {
        soldout: false,
        from: 9214960,
        to: 41783848,
        currency: 'RUR',
        areas: {
            from: '21.5',
            to: '77',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
    PLUS_4: {
        soldout: false,
        from: 44188400,
        to: 71721792,
        currency: 'RUR',
        areas: {
            from: '119.6',
            to: '138.6',
        },
        hasOffers: false,
        offersCount: 0,
        priceRatioToMarket: 0,
        status: 'ON_SALE',
    },
};

export const siteCardMock = ({
    price: {
        rooms,
    },
} as unknown) as ISiteSnippetType;
