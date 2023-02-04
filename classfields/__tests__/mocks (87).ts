import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';

export const store = {
    user: {
        favoritesMap: {},
    },
};

export const snippet = {
    id: 223071,
    name: 'Новое Колпино',
    fullName: 'квартал «Новое Колпино»',
    locativeFullName: 'в квартале «Новое Колпино»',
    location: {
        geoId: 26081,
        rgid: 417903,
        settlementRgid: 417903,
        settlementGeoId: 26081,
        populatedRgid: 417903,
        address: 'Колпино, ул. Загородная',
        subjectFederationId: 10174,
        subjectFederationRgid: 741965,
        subjectFederationName: 'Санкт-Петербург и ЛО',
        point: {
            latitude: 59.774136,
            longitude: 30.600004,
            precision: 'EXACT',
        },
        expectedMetroList: [],
        schools: [],
        parks: [],
        airports: [
            {
                id: '858726',
                name: 'Пулково',
                timeOnCar: 3306,
                distanceOnCar: 31441,
                latitude: 59.79992,
                longitude: 30.271744,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 55,
                        distance: 31441,
                    },
                ],
            },
        ],
        cityCenter: [
            {
                transport: 'ON_CAR',
                time: 3331,
                distance: 33023,
                latitude: 59.933926,
                longitude: 30.307991,
                timeDistanceList: [
                    {
                        transport: 'ON_CAR',
                        time: 55,
                        distance: 33023,
                    },
                ],
            },
        ],
        metro: {
            metroCityRgid: 417899,
            lineColors: ['23a12c'],
            metroGeoId: 20311,
            rgbColor: '23a12c',
            metroTransport: 'ON_TRANSPORT',
            name: 'Рыбацкое',
            timeToMetro: 28,
        },
        metroList: [
            {
                metroCityRgid: 417899,
                lineColors: ['23a12c'],
                metroGeoId: 20311,
                rgbColor: '23a12c',
                metroTransport: 'ON_TRANSPORT',
                name: 'Рыбацкое',
                timeToMetro: 28,
            },
            {
                metroCityRgid: 417899,
                lineColors: ['23a12c'],
                metroGeoId: 20312,
                rgbColor: '23a12c',
                metroTransport: 'ON_TRANSPORT',
                name: 'Обухово',
                timeToMetro: 36,
            },
            {
                metroCityRgid: 417899,
                lineColors: ['16bdf0'],
                metroGeoId: 20305,
                rgbColor: '16bdf0',
                metroTransport: 'ON_TRANSPORT',
                name: 'Купчино',
                timeToMetro: 41,
            },
            {
                metroCityRgid: 417899,
                lineColors: ['23a12c'],
                metroGeoId: 20313,
                rgbColor: '23a12c',
                metroTransport: 'ON_TRANSPORT',
                name: 'Пролетарская',
                timeToMetro: 42,
            },
        ],
    },
    viewTypes: ['GENERAL', 'GENERAL', 'GENERAL', 'COURTYARD', 'GENERAL', 'GENERAL', 'COURTYARD'],
    images: Array(22).fill(generateImageUrl({ width: 543, height: 332 })),
    appMiddleImages: Array(22).fill(generateImageUrl({ width: 883, height: 560 })),
    appLargeImages: Array(22).fill(generateImageUrl({ width: 883, height: 560 })),
    siteSpecialProposals: [
        {
            proposalType: 'MORTGAGE',
            description: 'Ставка по ипотеке 2.99%',
            mainProposal: true,
            specialProposalType: 'mortgage',
            shortDescription: 'Ставка по ипотеке 2.99%',
        },
        {
            proposalType: 'SALE',
            description: 'Трейд-ин',
            mainProposal: false,
            specialProposalType: 'sale',
            shortDescription: 'Трейд-ин',
        },
        {
            description: 'Беспроцентная рассрочка',
            mainProposal: false,
            specialProposalType: 'installment',
        },
    ],
    buildingClass: 'ECONOM',
    state: 'UNFINISHED',
    finishedApartments: true,
    price: {
        from: 2504082,
        to: 7860939,
        currency: 'RUR',
        minPricePerMeter: 108330,
        maxPricePerMeter: 136281,
        rooms: {
            '1': {
                soldout: false,
                from: 3454856,
                to: 4321841,
                currency: 'RUR',
                areas: {
                    from: '28.5',
                    to: '34.5',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            '2': {
                soldout: false,
                from: 4950396,
                to: 7860939,
                currency: 'RUR',
                areas: {
                    from: '44.2',
                    to: '65.7',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            '3': {
                soldout: false,
                from: 6586494,
                to: 7776696,
                currency: 'RUR',
                areas: {
                    from: '60.8',
                    to: '73.2',
                },
                hasOffers: false,
                offersCount: 0,
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
                from: 2504082,
                to: 3843143,
                currency: 'RUR',
                areas: {
                    from: '19.8',
                    to: '28.2',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'ON_SALE',
            },
            PLUS_4: {
                soldout: true,
                currency: 'RUR',
                areas: {
                    from: '87.1',
                    to: '87.1',
                },
                hasOffers: false,
                offersCount: 0,
                priceRatioToMarket: 0,
                status: 'NOT_ON_SALE',
            },
        },
        totalOffers: 0,
        priceRatioToMarket: 0,
        minArea: 19.8,
        maxArea: 73.2,
    },
    flatStatus: 'ON_SALE',
    developers: [
        {
            id: 102320,
            name: 'Группа «Самолет»',
        },
    ],
    salesDepartment: {
        id: 403850,
        name: 'СПб Реновация',
        isRedirectPhones: true,
        weekTimetable: [
            {
                dayFrom: 1,
                dayTo: 5,
                timePattern: [
                    {
                        open: '10:00',
                        close: '19:00',
                    },
                ],
            },
            {
                dayFrom: 6,
                dayTo: 7,
                timePattern: [
                    {
                        open: '11:00',
                        close: '18:00',
                    },
                ],
            },
        ],
        logo: '//avatars.mdst.yandex.net/get-realty/3022/company.2159.1918316347314729913/builder_logo_info',
        timetableZoneMinutes: 180,
        statParams:
            // eslint-disable-next-line max-len
            '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',
        encryptedPhones: [
            {
                phoneWithMask: '+7 812 425 ×× ××',
                phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
            },
        ],
        encryptedPhonesWithTag: [
            {
                phoneWithMask: '+7 812 425 ×× ××',
                phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
                tag: '',
            },
            {
                phoneWithMask: '+7 812 425 ×× ××',
                phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
                tag: 'newbuildingOffer',
            },
            {
                phoneWithMask: '+7 812 425 ×× ××',
                phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
                tag: 'campaign',
            },
            {
                phoneWithMask: '+7 812 425 ×× ××',
                phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
                tag: 'mapsMobile',
            },
        ],
        encryptedDump:
            // eslint-disable-next-line max-len
            'rRA9AAvYs++Yxo0ynYYfnq7BETVbxpfn1HWp8P79nA7xtYRx478FeoULwPfYAze5NLC10uheZI286x7yz3JYLIVzR48Yo2fwh3pfMrObh5Dp2TZ17giI9LVWH9bREDRJQvWx3B75u61gXh4RmR3UV2idyVAd0vii0ns/igSN+PGKA7Xn0TCvku0QX9hXUtHr23EER9skFtorCeBq5WsMyGuVpgr8r0FS+sOpohaLXZ08Q/gbHf6Tz41u6hu1mfFWOtTp/tdp5ZBX6n9L5i8osLE5GD3MIXSOLdXduKhfpKI6CE/blXHTbByIzUna1xoYuG1tghk98tRkDgVov6wny++WlOIFoEsSUaf4WMJdy/NdNdjhXnZyIR0E+q03nSF+yc4HBnQKsyR6WD0sUBbw0LOEQzH86BvIY59EAm1Kl/gcz3tGqeRhb0uBtgDHCqzvwy7xiinDyOHcirFPKNCwPiwBP/YKjTUJ3z0Z6b2QJPxOOVbPDuM7HIuTEdhuHlJgn4hjKMrh5v78t16uCoUvn3kJhQhABQv/Qw0+Y9olA8UhWfbj0G0qhNfRXPTCSX0O8U/cY6Qsja0qhtwqp/WXQ8C79mYqzsTIUgrf4nL0TVv68xijYUqbTTfwMGhAjnWvB2+awZ3AyV+lYYoOCE+1uzb94pMvvmkdgQBNN1rVMd7D50fxdzMHjHdXqsHLxuwWNXZO41Wi3VxRdW7y+i9PFLt9njDRdIGsE+TH92y0IbBA31sokOR2JDGFW0Cu9E+dQO61rEo/zoakPtNOTkf8IilGJMNHLlKni9Iyi6fNF8UahraKbmzhjH63rzwOb5z5mlHCyZiQoThnBsj9GqElFhzsdncjpxEGZy6TXYT15L6l2IS6BEFU/+g/XSZkZSBbVEqvejVafVesRRFI01nroeQJC/V/zmAbM3FsbkNYAkoYLfljUT49USDO9VMaNsKBhRqSjor6VZyiQA==',
    },
    phone: {
        phoneWithMask: '+7 812 425 ×× ××',
        phoneHash: 'KzcF4MHTIJ0MLjUNxOPDUR4',
    },
    backCallTrafficInfo: {} as TrafficSourceInfo,
    withBilling: true,
    awards: {},
    limitedCard: false,
} as ISiteSnippetType;
