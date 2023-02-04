import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

export const mock = ([
    {
        id: 424783,
        name: 'The Mostman',
        images: ['//avatars.mds.yandex.net/get-verba/1030388/2a0000016f337e8438300168542f5befcd69/realty_main'],
        filteredPrimaryOfferStats: {
            price: {
                from: 30225600,
                to: 67698200,
            },
            primaryOffers: 15,
        },
        location: {
            settlementRgid: 165705,
        },
    },
    {
        id: 123,
        name: 'Александрийское Поселение',
        images: ['//avatars.mds.yandex.net/get-verba/1030388/2a0000016f337e8438300168542f5befcd69/realty_main'],
        filteredPrimaryOfferStats: {
            price: {
                from: 30225600,
                to: 67698200,
            },
            primaryOffers: 1,
        },
        location: {
            settlementRgid: 165705,
        },
    },
    {
        id: 456,
        name: 'The Gulman',
        images: ['//avatars.mds.yandex.net/get-verba/1030388/2a0000016f337e8438300168542f5befcd69/realty_main'],
        filteredPrimaryOfferStats: {
            price: {
                from: 1235600,
                to: 67698200,
            },
            primaryOffers: 142,
        },
        location: {
            settlementRgid: 165705,
        },
    },
    {
        id: 789,
        name: 'Боровой Лес',
        images: ['//avatars.mds.yandex.net/get-verba/1030388/2a0000016f337e8438300168542f5befcd69/realty_main'],
        filteredPrimaryOfferStats: {
            price: {
                from: 5884894,
                to: 67698200,
            },
            primaryOffers: 1995,
        },
        location: {
            settlementRgid: 165705,
        },
    },
    {
        id: 789,
        name: 'Видный после листания ЖК',
        images: ['//avatars.mds.yandex.net/get-verba/1030388/2a0000016f337e8438300168542f5befcd69/realty_main'],
        filteredPrimaryOfferStats: {
            price: {
                from: 123,
                to: 67698200,
            },
            primaryOffers: 144,
        },
        location: {
            settlementRgid: 165705,
        },
    },
] as unknown) as ISiteSnippetType[];
