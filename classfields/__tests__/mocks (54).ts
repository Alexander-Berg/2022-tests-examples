import { IGeoStore } from 'realty-core/view/react/common/reducers/geo';
import { PageName } from 'realty-core/types/router';

export const props = {
    pageType: 'search',
    geo: { rgid: 741964 } as IGeoStore,
    params: { rgid: 741964 },
    router: {},
    cookies: {},
} as const;

export const mapProps = {
    ...props,
    pageType: 'offers-search-map' as PageName,
};

export const initialState = {
    filters: {
        offers: {
            type: 'offers',
            decl: {
                'geo-refinement-multi': {
                    control: 'geo-refinement-multi',
                },
                'geo-region': {
                    control: 'suggest',
                },
                type: {},
                priceType: {},
                metroTransport: {},
            },
            data: {
                'geo-refinement-multi': [],
                type: 'SELL',
                ctype: 'SELL',
                category: 'APARTMENT',
                priceType: 'PER_OFFER',
                metroTransport: 'ON_FOOT',
                pricingPeriod: 'PER_MONTH',
                commissionMax: null,
                'geo-region': {
                    label: 'Москва и МО',
                    rgid: 741964,
                },
                sort: 'RELEVANCE',
            },
        },
    },
} as const;

export const initialStateWithoutSelectedFilters = {
    filters: {
        offers: {
            type: 'offers',
            decl: {},
            data: {
                'geo-refinement-multi': [],
                type: 'SELL',
                ctype: 'SELL',
                category: 'APARTMENT',
                priceType: 'PER_OFFER',
                metroTransport: 'ON_FOOT',
                pricingPeriod: 'PER_MONTH',
                commissionMax: null,
                'geo-region': {
                    label: 'Москва и МО',
                    rgid: 741964,
                },
                sort: 'RELEVANCE',
            },
        },
    },
} as const;
