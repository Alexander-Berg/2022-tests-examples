import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой парковок',
    specs: [
        {
            name: 'Без геолокации',
            url: '/parking/',
            description:
                'Платные и бесплатные парковки на карте города. Поиск ближайшей парковки. Маршруты проезда на автомобиле и общественном транспорте',
            canonical: 'https://yandex.ru/maps/parking/',
            title: 'Парковки на карте — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Парковки', url: 'https://yandex.ru/maps/parking/'}
            ],
            alternates: [
                {
                    href: 'https://yandex.ru/maps/parking/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/parking/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'В Москве',
            url: '/213/moscow/parking/',
            description:
                'Платные и бесплатные парковки на карте Москвы. Поиск ближайшей парковки. Маршруты проезда на автомобиле и общественном транспорте',
            canonical: 'https://yandex.ru/maps/213/moscow/parking/',
            title: 'Парковки в Москве — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Парковки', url: 'https://yandex.ru/maps/213/moscow/parking/'}
            ]
        }
    ]
};

export default contents;
