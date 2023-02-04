import {SeoFile} from '../../types';

const BASE = {
    canonical: 'https://yandex.ru/maps/213/moscow/sputnik/',
    title: 'Спутниковая карта Москвы — Яндекс Карты',
    description:
        'Спутниковая карта Москвы и других городов, областей, краев, республик, округов на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, построить маршрут на общественном транспорте и автомобиле с учетом пробок, посмотреть панорамы улиц городов.',
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
        {name: 'Спутниковая карта', url: 'https://yandex.ru/maps/213/moscow/sputnik/'}
    ],
    alternates: [
        {
            href: 'https://yandex.ru/maps/213/moscow/sputnik/',
            hreflang: 'ru'
        },
        {
            href: 'https://yandex.com/maps/213/moscow/satellite/',
            hreflang: 'en'
        }
    ]
};

const contents: SeoFile = {
    name: 'Слой спутника',
    specs: [
        {
            name: 'Обычный',
            url: '/213/moscow/sputnik/',
            ...BASE
        },
        {
            name: 'Старый шорткат',
            url: '/moscow_sputnik/',
            ...BASE,
            redirectUrl: '/213/moscow/sputnik/'
        }
    ]
};

export default contents;
