/* eslint-disable max-len */

import { generateImageAliases } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

const imageAliases = generateImageAliases({ width: 200, height: 200 });

export default {
    card: {
        name: 'Пупкино и компания',
        logo: imageAliases,
        foundationDate: '1999-02-27T21:00:00Z',
        address: {
            unifiedAddress:
                'Кудрово, Заневское городское поселение, Всеволожский район, Ленинградская область, Европейский проспект, 8',
            point: {
                latitude: 59.900955,
                longitude: 59.900955,
            },
        },
        workSchedule: [
            {
                day: 'FRIDAY',
                minutesFrom: 11,
                minutesTo: 1439,
            },
            {
                day: 'MONDAY',
                minutesFrom: 11,
                minutesTo: 1439,
            },
            {
                day: 'SUNDAY',
                minutesFrom: 11,
                minutesTo: 1439,
            },
            {
                day: 'WEDNESDAY',
                minutesFrom: 11,
                minutesTo: 1439,
            },
        ],
        description:
            'Но тщательные исследования конкурентов неоднозначны и будут в равной степени предоставлены сами себе. В своём стремлении повысить качество жизни, они забывают, что начало повседневной работы по формированию позиции не оставляет шанса для системы обучения кадров, соответствующей насущным потребностям. Каждый из нас понимает очевидную вещь: высококачественный прототип будущего проекта представляет собой интересный эксперимент проверки распределения внутренних резервов и ресурсов.',
        creationDate: '2020-06-22T20:03:33Z',
        userType: 'AGENCY',
        profileUid: '4045447503',
        offerCounters: {
            totalOffers: 82,
            filteredOffers: 0,
        },
    },
    offers: {
        items: [],
        total: 0,
    },
    searchParams: {
        type: 'SELL',
        category: 'APARTMENT',
        rgid: 741964,
        uid: '4045447503',
        pageSize: 3,
    },
    network: {
        searchOffersStatus: 'LOADED',
    },
};
