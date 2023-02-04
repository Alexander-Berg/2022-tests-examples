import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой гибрид',
    specs: [
        {
            name: 'Обычный',
            url: '/213/moscow/hybrid/',
            canonical: 'https://yandex.ru/maps/213/moscow/hybrid/',
            title: 'Гибридная карта Москвы — Яндекс Карты',
            description:
                'Гибридная карта Москвы и других городов, областей, краёв, республик, округов — снимки местности, сделанные со спутника. Россия и весь мир на Яндекс Картах',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Гибрид', url: 'https://yandex.ru/maps/213/moscow/hybrid/'}
            ],
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/hybrid/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/hybrid/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'Cтарый шорткат',
            url: '/moscow_hybrid/',
            canonical: 'https://yandex.ru/maps/213/moscow/hybrid/',
            title: 'Гибридная карта Москвы — Яндекс Карты',
            description:
                'Гибридная карта Москвы и других городов, областей, краёв, республик, округов — снимки местности, сделанные со спутника. Россия и весь мир на Яндекс Картах',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Гибрид', url: 'https://yandex.ru/maps/213/moscow/hybrid/'}
            ],
            redirectUrl: '/213/moscow/hybrid'
        }
    ]
};

export default contents;
