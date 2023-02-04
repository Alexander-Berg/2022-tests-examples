import { ICardPlansOffer } from 'realty-core/types/cardPlansOffers';
import { ISiteSpecialProposal, ISitePrice } from 'realty-core/types/common';
import { ISitePlan } from 'realty-core/types/sitePlans';
import { ICoreStore } from 'realty-core/view/react/common/reducers/types';

export const LOCATION = {
    metro: {
        metroGeoId: 218432,
        metroTransport: 'ON_TRANSPORT',
        timeToMetro: 19,
    },
};

export const QUERY_TEXT = '/lyubertsy/kupit/novostrojka/lyubercy-1839196/';

export const QUERY_ID = 'cce63354dbe464c7baee8b7de82bcfd8';

export const WEB_REFERER = 'https://realty.test.vertis.yandex.ru/lyubertsy/kupit/novostrojka/lyubercy-1839196/';

export const REREFERER = 'https://realty.test.vertis.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/';

export const MAIN_PHOTO_URL = 'https://via.placeholder.com/150';

export const MOCK_DATE = '2022-01-01T12:00:00.100Z';

export const PHONE_NUMBER = '+74951533907';

export const CHAT = { id: 'chat-id' };

export const ANALYTIC_DATA = {
    from: 'trafficFrom',
    utm_campaign: 'utmCampaign',
    utm_content: 'utmContent',
    utm_medium: 'utmMedium',
    utm_source: 'utmSource',
    utm_term: 'utmTerm',
};

export const SITE_PLAN = {
    clusterId: 'cluster-id',
    offersCount: 5,
} as ISitePlan;

export const SITE_PRICE = {
    rooms: {
        1: {
            soldout: false,
            from: 5442150,
            to: 7474960,
            currency: 'RUR',
            areas: {
                from: '33.5',
                to: '44.9',
            },
            hasOffers: true,
            offersCount: 132,
            priceRatioToMarket: 0,
            status: 'ON_SALE',
        },
        2: {
            soldout: false,
            from: 6901000,
            to: 10094400,
            currency: 'RUR',
            areas: {
                from: '47.5',
                to: '72.2',
            },
            hasOffers: true,
            offersCount: 193,
            priceRatioToMarket: 0,
            status: 'ON_SALE',
        },
        3: {
            soldout: true,
            from: 8301150,
            to: 12225770,
            currency: 'RUR',
            areas: {
                from: '64.5',
                to: '94.7',
            },
            hasOffers: true,
            offersCount: 62,
            priceRatioToMarket: 0,
            status: 'ON_SALE',
        },
        OPEN_PLAN: {
            soldout: false,
            currency: 'RUR',
            hasOffers: false,
            offersCount: 0,
            priceRatioToMarket: 0,
        },
        STUDIO: {
            soldout: false,
            from: 4009500,
            to: 5411520,
            currency: 'RUR',
            areas: {
                from: '19.7',
                to: '28.8',
            },
            hasOffers: true,
            offersCount: 57,
            priceRatioToMarket: 0,
            status: 'ON_SALE',
        },
        PLUS_4: {
            soldout: false,
            currency: 'RUR',
            hasOffers: false,
            offersCount: 0,
            priceRatioToMarket: 0,
        },
    },
} as ISitePrice;

export const CORE_STORE = ({
    page: {
        queryId: QUERY_ID,
    },
    config: {
        isTablet: false,
        isMobile: false,
        referer: REREFERER,
        retpath: WEB_REFERER,
    },
    routing: {
        currentRoute: QUERY_TEXT,
    },
} as unknown) as ICoreStore;

export const MOBILE_CORE_STORE = ({
    ...CORE_STORE,
    config: { ...CORE_STORE, isMobile: true, isTablet: false },
} as unknown) as ICoreStore;

export const TABLET_CORE_STORE = ({
    ...CORE_STORE,
    config: { ...CORE_STORE, isMobile: true, isTablet: true },
} as unknown) as ICoreStore;

export const ALL_SPECIAL_PROPOSALS = [
    {
        specialProposalType: '___UNKNOWN_TYPE___',
        proposalType: '___UNKNOWN_TYPE___',
        shortDescription: 'u',
        description: 'Unknown proposal',
        mainProposal: false,
    },
    {
        specialProposalType: 'mortgage',
        proposalType: 'MORTGAGE',
        shortDescription: 'm',
        description: 'Mortgage proposal',
        mainProposal: true,
    },
    {
        specialProposalType: 'discount',
        proposalType: 'DISCOUNT',
        shortDescription: 'd',
        description: 'Discount proposal',
        mainProposal: false,
    },
    {
        specialProposalType: 'gift',
        proposalType: 'GIFT',
        shortDescription: 'g',
        description: 'Gift proposal',
        mainProposal: false,
    },
    {
        specialProposalType: 'installment',
        proposalType: 'INSTALLMENT',
        shortDescription: 'u',
        description: 'Installment proposal',
        mainProposal: false,
    },
    {
        specialProposalType: 'sale',
        proposalType: 'SALE',
        shortDescription: 's',
        description: 'Sale proposal',
        mainProposal: false,
    },
    {
        specialProposalType: 'last',
        proposalType: 'LAST',
        shortDescription: 'l',
        description: 'Last proposal',
        mainProposal: false,
    },
] as ISiteSpecialProposal[];

export const CARD_PLANS_OFFER = ({
    offerId: 'offer-id',
    withExcerpt: true,
    predictions: {
        predictedPriceAdvice: {
            summary: 'LOW',
        },
    },
    location: LOCATION,
    updateDate: '2022-02-04T07:46:33.308Z',
} as unknown) as ICardPlansOffer;
