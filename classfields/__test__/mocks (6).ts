import { TrafficSourceInfo } from '@vertis/schema-registry/ts-types/realty/event/model';

import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

import { ISiteSnippetType } from 'realty-core/types/siteSnippet';
import { ISiteSpecialProposal } from 'realty-core/types/common';

const imgArray = Array(7).fill(generateImageUrl({ width: 800, height: 600 }));

export const siteSnippetMock = ({
    appLargeImages: imgArray,
    buildingClass: 'COMFORT',
    backCallTrafficInfo: {} as TrafficSourceInfo,
    developers: [
        {
            id: 2247,
            name: 'Самолёт',
        },
    ],
    finishedApartments: true,
    flatStatus: 'ON_SALE',
    fullName: 'ЖК «Солнечный город»',
    id: 123456,
    images: imgArray,
    appMiddleImages: imgArray,
    limitedCard: false,
    location: {
        rgid: 193455,
        settlementRgid: 417899,
        address: 'Санкт-Петербург, ул. Лётчика Лихолетова / ул. Тимофея Фёдорова',
        metro: {
            metroCityRgid: 417899,
            lineColors: ['f03d2f'],
            metroGeoId: 20300,
            metroTransport: 'ON_TRANSPORT',
            name: 'Проспект Ветеранов',
            rgbColor: 'f03d2f',
            timeToMetro: 35,
        },
        subjectFederationRgid: 741965,
        populatedRgid: 2,
    },
    locativeFullName: 'в ЖК «Тестовый город»',
    name: '«Тестовый город',
    phone: { phoneHash: 'KzcF4MHTIJzMLzUN1NPTUR1', phoneWithMask: '+7 812 335 ×× ××' },
    price: {
        currency: 'RUR',
        from: 3350161,
        rooms: {
            1: {
                areas: { from: '31', to: '43.4' },
                currency: 'RUR',
                from: 4500244,
                hasOffers: false,
                priceRatioToMarket: 0,
                soldout: false,
                status: 'ON_SALE',
            },
        },
    },
    salesDepartment: {
        name: '«Тестовая Недвижимость',
    },
    state: 'UNFINISHED',
    viewTypes: Array(7).fill('GENERAL'),
    withBilling: true,
    siteSpecialProposals: [
        {
            description: 'Скидки до 15%',
            mainProposal: true,
            specialProposalType: 'discount',
        } as ISiteSpecialProposal,
    ],
    awards: {},
} as unknown) as ISiteSnippetType;
