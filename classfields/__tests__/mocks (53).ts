import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { IVillageSnippetType } from 'realty-core/types/villageSnippet';

const imgMock = generateImageUrl({ width: 900, height: 500 });

const imgObject = {
    appLarge: imgMock,
    appLargeSnippet: imgMock,
    appMiddle: imgMock,
    appMiddleSnippet: imgMock,
    appMiniSnippet: imgMock,
    appSmallSnippet: imgMock,
    cosmic: imgMock,
    full: imgMock,
    large1242: imgMock,
    mini: imgMock,
};

const village: IVillageSnippetType = {
    id: '1777617',
    name: 'Эко озеро',
    fullName: 'Коттеджный посёлок «Эко озеро»',
    locativeFullName: 'в коттеджном посёлке «Эко озеро»',
    location: {
        address: 'Волоколамский район, д. Сляндево',
        geoId: 1,
        geocoderAddress: 'Московская область, Волоколамский район, коттеджный поселок Эко Озеро',
        highways: [
            {
                distanceKm: 105.77,
                name: 'Новорижское шоссе',
                originText: 'до МКАД',
            },
        ],
        insideMKAD: false,
        point: { latitude: 55.892384, longitude: 35.995235 },
        populatedRgid: '741964',
        rgid: '295962',
        stations: [{ distanceKm: 9.905, name: 'Дубосеково' }],
        subjectFederationId: 1,
        subjectFederationName: 'Москва и МО',
        subjectFederationRgid: '741964',
    },
    backCallTrafficInfo: {} as TrafficSourceInfo,
    deliveryDates: [
        {
            phaseName: '1 очередь',
            phaseIndex: 1,
            status: 'HAND_OVER',
            year: 2018,
            quarter: 2,
            finished: true,
        },
    ],
    images: Array(5).fill({
        photoType: 'COMMON',
        image: imgObject,
    }),
    mainPhoto: imgObject,
    offerStats: {
        // eslint-disable-next-line @typescript-eslint/ban-ts-comment
        //@ts-ignore
        entries: [
            {
                landArea: {
                    from: 8.8,
                    to: 18.4,
                    unit: 'SOTKA',
                },
                offerType: 'LAND',
                price: {
                    currency: 'RUB',
                    from: '657090',
                    to: '1256482',
                },
            },
        ],
        primaryPrices: {
            currency: 'RUB',
            from: '657090',
            to: '1256482',
        },
    },
    phone: { phoneWithMask: '+7 495 480 ×× ××', phoneHash: 'SomePhoneHash' },

    salesDepartment: {
        logo: imgMock,
        name: 'Удача',
    },
};

export const villages = Array(5).fill(village);
