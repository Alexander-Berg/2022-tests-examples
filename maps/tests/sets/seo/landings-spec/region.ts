import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинги регионов',
    specs: [
        {
            name: 'Город',
            url: '/213/moscow/',
            canonical: 'https://yandex.ru/maps/213/moscow/',
            h1: 'Москва',
            title: 'Карта Москвы с улицами и номерами домов онлайн — Яндекс Карты',
            description:
                'Подробная карта Москвы с улицами и номерами домов на сайте и в мобильном приложении Яндекс Карты. Достопримечательности и организации с рейтингом, отзывами и фото на карте Москвы. Яндекс Карты помогут построить маршрут на общественном транспорте и автомобиле с учетом пробок, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва и Московская область', url: 'https://yandex.ru/maps/1/moscow-and-moscow-oblast/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'}
            ],
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&z=10&ll=37.622504,55.753215&lg=0&cr=0&signature=Jm2czKsD59czUHptLDDLD8qrlULA3KA2yFx4hHhKuH4='
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'Федеральный округ',
            url: '/1/moscow-and-moscow-oblast/',
            canonical: 'https://yandex.ru/maps/1/moscow-and-moscow-oblast/',
            h1: 'Москва и Московская область',
            title: 'Карта Москвы и Московской области с городами — Яндекс Карты',
            description:
                'Подробная карта Москвы и Московской области с городами и другими населенными пунктами на сайте и в мобильном приложении Яндекс Карты. Схема дорог и построение маршрутов на карте Москвы и Московской области. Карты городов и регионов. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва и Московская область', url: 'https://yandex.ru/maps/1/moscow-and-moscow-oblast/'}
            ]
        },
        {
            name: 'Страна',
            url: '/225/russia/',
            canonical: 'https://yandex.ru/maps/225/russia/',
            h1: 'Россия',
            title: 'Карта России с городами и регионами — Яндекс Карты',
            description:
                'Подробная карта России с городами и регионами на сайте и в мобильном приложении Яндекс Карты. Схема дорог и построение маршрутов на карте России. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов. Карты стран, городов и регионов на Яндекс Картах.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Россия', url: 'https://yandex.ru/maps/225/russia/'}
            ]
        },
        {
            name: 'Континент',
            url: '/10001/eurasia/',
            canonical: 'https://yandex.ru/maps/10001/eurasia/',
            h1: 'Евразия',
            title: 'Яндекс Карты — транспорт, навигация, поиск мест',
            description:
                'Карты помогут найти нужное место даже без точного адреса и построят до него маршрут на общественном транспорте, автомобиле или пешком.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Евразия', url: 'https://yandex.ru/maps/10001/eurasia/'}
            ]
        }
    ]
};

export default contents;
