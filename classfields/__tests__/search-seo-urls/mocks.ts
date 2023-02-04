import router from 'realty-router';

import ServiceUrl from 'realty-core/app/lib/url';

import type { Tmock } from './types';

const getUrlHelper = (viewType: string, url: string) =>
    new ServiceUrl({
        url,
        routers: router,
        viewType,
    });

export const getMockData = (type: string, viewType: string, totalItems: number, isAmpPage: boolean): Tmock => ({
    searchParams: {
        type,
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
        urlHelper: getUrlHelper(viewType, 'https://realty.yandex.ru/moskva/kupit/kvartira/'),
    },
    getParams: () => ({
        rgid: 587795,
        type,
        streetId: '55034',
        streetName: 'ulica-cyurupy',
        category: 'APARTMENT',
    }),
    isAmpPage,
});
