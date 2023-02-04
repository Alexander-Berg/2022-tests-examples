import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг каталога',
    specs: [
        {
            name: 'Общий',
            url: '/213/moscow/catalog/',
            canonical: 'https://yandex.ru/maps/213/moscow/catalog/',
            h1: '720 224 организации Москвы',
            title: 'Каталог организаций Москвы с отзывами, адресами и графиками работы',
            description:
                '720 224 организаций Москвы на карте. Посмотрите отзывы посетителей, график работы, адреса, телефоны, фото. Яндекс Карты помогут добраться до места пешком, на общественном транспорте или на машине.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'}
            ],
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/catalog/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/catalog/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'С группой',
            url: '/213/moscow/catalog/7/',
            canonical: 'https://yandex.ru/maps/213/moscow/catalog/7/',
            h1: 'Продукты в Москве',
            title: 'Продукты в Москве: каталог мест на карте',
            description:
                'Продукты — список мест в Москве. Посмотрите отзывы посетителей, графики работы, адреса, телефоны, фото. Яндекс Карты помогут добраться до места пешком, на общественном транспорте или на машине.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Продукты', url: 'https://yandex.ru/maps/213/moscow/catalog/7/'}
            ]
        }
    ]
};

export default contents;
