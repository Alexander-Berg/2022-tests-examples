import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой маршрутов',
    specs: [
        {
            name: 'Без геолокации',
            url: '/routes/',
            canonical: 'https://yandex.ru/maps/routes/',
            title: 'Навигатор онлайн: построение маршрута на карте — Яндекс Карты',
            description:
                'Онлайн-навигация с учетом пробок на Яндекс Картах. Построение маршрута из одной точки в другую на автомобиле, общественном транспорте или пешком на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Маршруты', url: 'https://yandex.ru/maps/routes/'}
            ],
            alternates: [
                {
                    href: 'https://yandex.ru/maps/routes/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/routes/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'В Москве',
            url: '/213/moscow/routes/',
            canonical: 'https://yandex.ru/maps/213/moscow/routes/',
            title: 'Построение маршрутов на карте Москвы — Яндекс Карты',
            description:
                'Построение оптимального маршрута в Москве с указанием расстояния и времени в пути на автомобиле, общественном транспорте или пешком на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Маршруты', url: 'https://yandex.ru/maps/213/moscow/routes/'}
            ]
        }
    ]
};

export default contents;
