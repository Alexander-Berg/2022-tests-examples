import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой ОТ',
    url: '/213/moscow/transport/',
    canonical: 'https://yandex.ru/maps/213/moscow/transport/',
    title: 'Общественный транспорт Москвы на карте - Яндекс Карты',
    description:
        'Маршруты, остановки, местоположение автобусов, троллейбусов, трамваев и маршрутных такси в режиме онлайн на карте Москвы. Общественный транспорт Москвы на Яндекс Картах.',
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'}
    ],
    alternates: [
        {
            href: 'https://yandex.ru/maps/213/moscow/transport/',
            hreflang: 'ru'
        },
        {
            href: 'https://yandex.com/maps/213/moscow/transport/',
            hreflang: 'en'
        }
    ]
};

export default contents;
