import { generateImageUrl } from '@realty-front/jest-utils/puppeteer/tests-helpers/image';

export const Gate = {
    get() {
        return Promise.resolve({
            entries: [
                {
                    type: 'YANDEX',
                    id: 'bKYKdE-NS9yuypv25EydaMzlvS6wQ3sP',
                    text: 'Хороший детский сад и очень хорошие воспитатели',
                    rating: 5,
                    isAnonymous: false,
                    updatedTime: '2020-11-05T14:54:39.585Z',
                    author: {
                        name: 'Vasily Pupkin'
                    },
                    orgId: '70473849576'
                },
                {
                    type: 'YANDEX',
                    id: 'PWI7bSNsLIuLBZbgQHcKnIRwlBrbE_LD',
                    text: 'Замечательный педогогический состав во всех группах',
                    rating: 5,
                    isAnonymous: false,
                    updatedTime: '2020-08-01T08:29:37.759Z',
                    author: {
                        name: 'Vasily Pupkin'
                    },
                    orgId: '70473849576'
                },
                {
                    type: 'YANDEX',
                    id: 'yVgaCnBJrAeE8-E4HTnF9u6YzQrsgP',
                    text: 'Хорошая школа. Переводим сюда детей.',
                    rating: 5,
                    isAnonymous: false,
                    updatedTime: '2020-06-30T08:25:13.616Z',
                    author: {
                        name: 'Vasily Pupkin'
                    },
                    orgId: '70473849576'
                }
            ],
            pager: {
                page: 1,
                totalPages: 2,
                count: 6
            }
        });
    }
};

export const initialState = {
    reviews: {
        isLoading: false,
        entries: [],
        pager: {
            page: 0,
            totalPages: 0,
            count: 0
        }
    }
};

export const item = {
    lat: 55.514687,
    lon: 37.354465,
    properties: {
        objId: '70473849576',
        hintContent: 'Барбарискин',
        clusterCaption: 'Барбарискин',
        balloonContent: '6-я Нововатутинская ул., 3, Россия',
        categories: 'Детский сад',
        name: 'Барбарискин',
        description: '6-я Нововатутинская ул., 3, Россия',
        address: 'Россия, Москва, поселение Десёновское, 6-я Нововатутинская улица, 3',
        openText: 'пн-пт 07:00–19:00',
        shortName: 'Доп Дмитровская 29',
        rating: {
            rates: 32,
            reviews: 888,
            value: 3.5
        },
        phones: [ { info: '', number: '+7 (499) 499-55-69' } ],
        stops: {
            land: { distance: '130 м', name: 'ТЦ Нововатутинский' },
            metro: { color: 10659252, distance: '170м', name: 'Петровско-Разумовская' }
        },
        links: [
            {
                aref: '#facebook',
                link: { href: 'https://www.facebook.com/school1454co/' },
                tag: 'social',
                type: 'SOCIAL'
            },
            {
                aref: '#instagram',
                link: { href: 'https://www.instagram.com/school1454co/' },
                tag: 'social',
                type: 'SOCIAL'
            }
        ],
        subTitle: [
            {
                type: 'working_hours',
                text: 'До 19:00',
                property: [
                    {
                        key: 'is_open',
                        value: '1'
                    },
                    {
                        key: 'short_text',
                        value: 'До 19:00'
                    },
                    {
                        key: 'text',
                        value: 'Открыто до 19:00'
                    }
                ]
            },
            {
                type: 'rating',
                text: '8.6',
                property: [
                    {
                        key: 'value',
                        value: '8.6'
                    },
                    {
                        key: 'value_5',
                        value: '4.3'
                    }
                ]
            }
        ],
        sub: [
            {
                type: 'working_hours',
                text: 'До 19:00',
                property: [
                    { key: 'is_open', value: '1' },
                    { key: 'short_text', value: 'До 19:00' },
                    { key: 'text', value: 'Открыто до 19:00' }
                ]
            },
            {
                type: 'rating',
                text: '8.6',
                property: [
                    { key: 'value', value: '8.6' },
                    { key: 'value_5', value: '4.3' }
                ]
            }
        ],
        photos: {
            count: 1,
            photo: [ { urlTemplate: generateImageUrl({ width: 300, height: 200 }) } ]
        }
    }
};
