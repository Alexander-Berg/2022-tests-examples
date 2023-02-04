/* eslint-disable max-len */

import { RequestStatus } from 'realty-core/types/network';

const offers = [
    {
        offerId: '270410021344087525',
        dates: {
            creationDate: '2016-02-19T00:00:00.000+03:00',
            lastDate: '2016-07-06T00:00:00.000+03:00',
            daysExposed: 139,
        },
        prices: {
            firstPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
            lastPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
        },
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        offerInfo: { premoderation: false, partnerId: '1058010352', internal: true, clusterId: '270410021344087525' },
        areaInfo: {
            area: { value: 45, unit: 'SQUARE_METER' },
            totalArea: { value: 45, unit: 'SQUARE_METER' },
            kitchenSpace: { value: 7, unit: 'SQUARE_METER' },
            livingSpace: { value: 28, unit: 'SQUARE_METER' },
        },
        images: {
            totalImages: 1,
            urls: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469'],
            minicardImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/minicard',
            ],
            mainImages: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/main'],
            fullImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/large',
            ],
            alikeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/alike',
            ],
            cosmicImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/cosmic',
            ],
            appMiddleImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_middle',
            ],
            appLargeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_large',
            ],
            appMiniSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_mini',
            ],
            appSmallSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_small',
            ],
            appMiddleSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_middle',
            ],
            appLargeSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_large',
            ],
        },
        location: { localityName: 'Москва', subjectFederationId: '1' },
        building: { buildingId: '7026478805298347531' },
        floorsTotal: 9,
        floorsOffered: 9,
        openPlan: false,
        roomsOffered: 2,
        roomsTotal: 2,
        flatType: 'SECONDARY',
        house: { balconyType: 'UNKNOWN', studio: false },
        apartment: { newFlat: false, renovation: 'UNKNOWN' },
        active: false,
    },
    {
        offerId: '270410021344087525',
        dates: {
            creationDate: '2016-02-19T00:00:00.000+03:00',
            lastDate: '2016-07-06T00:00:00.000+03:00',
            daysExposed: 139,
        },
        prices: {
            firstPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
            lastPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
        },
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        offerInfo: { premoderation: false, partnerId: '1058010352', internal: true, clusterId: '270410021344087525' },
        areaInfo: {
            area: { value: 45, unit: 'SQUARE_METER' },
            totalArea: { value: 45, unit: 'SQUARE_METER' },
            kitchenSpace: { value: 7, unit: 'SQUARE_METER' },
            livingSpace: { value: 28, unit: 'SQUARE_METER' },
        },
        images: {
            totalImages: 1,
            urls: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469'],
            minicardImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/minicard',
            ],
            mainImages: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/main'],
            fullImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/large',
            ],
            alikeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/alike',
            ],
            cosmicImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/cosmic',
            ],
            appMiddleImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_middle',
            ],
            appLargeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_large',
            ],
            appMiniSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_mini',
            ],
            appSmallSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_small',
            ],
            appMiddleSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_middle',
            ],
            appLargeSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_large',
            ],
        },
        location: { localityName: 'Москва', subjectFederationId: '1' },
        building: { buildingId: '7026478805298347531' },
        floorsTotal: 9,
        floorsOffered: 9,
        openPlan: false,
        roomsOffered: 2,
        roomsTotal: 2,
        flatType: 'SECONDARY',
        house: { balconyType: 'UNKNOWN', studio: false },
        apartment: { newFlat: false, renovation: 'UNKNOWN' },
        active: false,
    },
    {
        offerId: '270410021344087525',
        dates: {
            creationDate: '2016-02-19T00:00:00.000+03:00',
            lastDate: '2016-07-06T00:00:00.000+03:00',
            daysExposed: 139,
        },
        prices: {
            firstPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
            lastPrice: { value: 7400000, period: 'WHOLE_LIFE', currency: 'RUR' },
        },
        offerType: 'SELL',
        offerCategory: 'APARTMENT',
        offerInfo: { premoderation: false, partnerId: '1058010352', internal: true, clusterId: '270410021344087525' },
        areaInfo: {
            area: { value: 45, unit: 'SQUARE_METER' },
            totalArea: { value: 45, unit: 'SQUARE_METER' },
            kitchenSpace: { value: 7, unit: 'SQUARE_METER' },
            livingSpace: { value: 28, unit: 'SQUARE_METER' },
        },
        images: {
            totalImages: 1,
            urls: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469'],
            minicardImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/minicard',
            ],
            mainImages: ['//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/main'],
            fullImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/large',
            ],
            alikeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/alike',
            ],
            cosmicImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/cosmic',
            ],
            appMiddleImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_middle',
            ],
            appLargeImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_large',
            ],
            appMiniSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_mini',
            ],
            appSmallSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_small',
            ],
            appMiddleSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_middle',
            ],
            appLargeSnippetImages: [
                '//avatars.mds.yandex.net/get-realty/27080/offer.270410021344087525.1527220312096940469/app_snippet_large',
            ],
        },
        location: { localityName: 'Москва', subjectFederationId: '1' },
        building: { buildingId: '7026478805298347531' },
        floorsTotal: 9,
        floorsOffered: 9,
        openPlan: false,
        roomsOffered: 2,
        roomsTotal: 2,
        flatType: 'SECONDARY',
        house: { balconyType: 'UNKNOWN', studio: false },
        apartment: { newFlat: false, renovation: 'UNKNOWN' },
        active: false,
    },
];

export const getStore = () => ({
    yaDealValuation: {
        network: {
            archiveData: {
                offers,
                page: {
                    totalItems: 10,
                },
            },
            getArchiveOffersStatus: {
                status: RequestStatus.LOADED,
            },
        },
    },
});
