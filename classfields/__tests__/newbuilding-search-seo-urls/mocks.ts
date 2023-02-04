import router from 'realty-router';

import ServiceUrl from 'realty-core/app/lib/url';

import type { Tmock } from './types';

const getUrlHelper = (viewType: string, url: string) =>
    new ServiceUrl({
        url,
        routers: router,
        viewType,
    });

export const getMockData = (viewType: string, totalItems: number, isAmpPage: boolean): Tmock => ({
    searchParams: {
        type: 'SELL',
    },
    pageParams: {
        type: 'search',
    },
    search: {
        offers: {
            pager: {
                totalItems,
            },
        },
    },
    seo: {
        ampUrl: '',
        canonicalUrl: '',
    },
    seoParams: [],
    req: {
        urlHelper: getUrlHelper(viewType, 'https://realty.yandex.ru/moskva/kupit/novostrojka/'),
    },
    getParams: () => ({
        rgid: 587795,
        type: 'SELL',
        streetId: '55034',
        streetName: 'ulica-cyurupy',
    }),
    isAmpPage,
    user: {
        regionInfo: {},
    },
});
