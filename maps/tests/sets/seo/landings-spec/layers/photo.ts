import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой фото',
    url: '/213/moscow/photo/',
    canonical: 'https://yandex.ru/maps/213/moscow/photo/',
    title: 'Фотографии на карте Москвы — Яндекс Карты',
    description: 'Фотографии улиц, домов, организаций и достопримечательностей Москвы на Яндекс Картах',
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
        {name: 'Фотографии Москвы', url: 'https://yandex.ru/maps/213/moscow/photo/'}
    ],
    alternates: [
        {
            href: 'https://yandex.ru/maps/213/moscow/photo/',
            hreflang: 'ru'
        },
        {
            href: 'https://yandex.com/maps/213/moscow/photo/',
            hreflang: 'en'
        }
    ]
};

export default contents;
