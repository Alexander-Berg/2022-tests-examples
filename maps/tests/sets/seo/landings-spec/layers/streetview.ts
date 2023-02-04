import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой панорам',
    url: '/213/moscow/panorama/',
    canonical: 'https://yandex.ru/maps/213/moscow/panorama/',
    title: 'Панорамы улиц на карте Москвы — Яндекс Карты',
    description:
        'Панорамные фото Москвы на Яндекс Картах — названия улиц, номера домов, достопримечательности, интересные места. Панорамы позволяют совершить виртуальную прогулку по Москве и другим городам России, Украины, Беларуси, Казахстана и Турции',
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
        {name: 'Панорамы', url: 'https://yandex.ru/maps/213/moscow/panorama/'}
    ],
    alternates: [
        {
            href: 'https://yandex.ru/maps/213/moscow/panorama/',
            hreflang: 'ru'
        },
        {
            href: 'https://yandex.com/maps/213/moscow/streetview/',
            hreflang: 'en'
        }
    ]
};

export default contents;
