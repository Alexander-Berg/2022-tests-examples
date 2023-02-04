import {SeoFile} from '../../types';

const contents: SeoFile = {
    name: 'Слой пробок',
    specs: [
        {
            name: 'Без геолокации',
            url: '/probki/',
            canonical: 'https://yandex.ru/maps/probki/',
            title: 'Пробки на дорогах — Яндекс Карты',
            description:
                'Яндекс Пробки: пробки на дорогах городов и областей России в режиме онлайн. Построение маршрутов и онлайн-навигация с учетом пробок на автомобиле и общественном транспорте на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Пробки', url: 'https://yandex.ru/maps/probki/'}
            ],
            alternates: [
                {
                    href: 'https://yandex.ru/maps/probki/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/traffic/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'На домене .com.tr',
            url: '/trafik/',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/trafik/',
            title: 'Canlı Yol Durumu — Yandex Haritalar',
            description:
                "Yandex Trafik: Türkiye yollarında çevrimiçi trafik durumu. Yandex Haritalar'ın web servisinde veya mobil uygulamasında, trafik durumuna göre toplu taşımayla veya özel araçla rota oluşturma ve navigasyon özelliği. Yandex Haritalar sokak, bina ve kurum bulmaya, uydu haritasını ve cadde panoramalarını görüntülemeye yardım eder.",
            breadcrumbList: [
                {name: 'Haritalar', url: 'https://yandex.com.tr/harita/'},
                {name: 'Trafik', url: 'https://yandex.com.tr/harita/trafik/'}
            ]
        },
        {
            name: 'На домене .com',
            url: '/traffic/',
            tld: 'com',
            canonical: 'https://yandex.com/maps/traffic/',
            title: 'Traffic conditions — Yandex Maps',
            description:
                'Yandex Traffic: real-time traffic for cities and regions in Russia. Driving and public transport directions, real-time navigation that accounts for traffic. Driving and public transport directions that account for traffic, on the web and in the Yandex Maps mobile app. Find the right street, building, or organization, view satellite maps and street panoramas.',
            breadcrumbList: [
                {name: 'Maps', url: 'https://yandex.com/maps/'},
                {name: 'Traffic conditions', url: 'https://yandex.com/maps/traffic/'}
            ]
        },
        {
            name: 'Слой в Москве',
            url: '/213/moscow/probki/',
            canonical: 'https://yandex.ru/maps/213/moscow/probki/',
            title: 'Пробки в Москве — Яндекс Карты',
            description:
                'Пробки в Москве в режиме онлайн на Яндекс Картах. Построение маршрутов и онлайн-навигация в Москве с учетом пробок на автомобиле и общественном транспорте на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Пробки', url: 'https://yandex.ru/maps/213/moscow/probki/'}
            ],
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map,trf,skl,trfe&z=10&ll=37.622504,55.753215&lg=0&cr=0&signature=DHi9_Ic7p3Uf0DlXBNXaTeQ_OH1dot7Bx6PnJ67S7fE='
            }
        },
        {
            name: 'Старый шорткат',
            url: '/traffic/',
            canonical: 'https://yandex.ru/maps/probki/',
            title: 'Пробки на дорогах — Яндекс Карты',
            description:
                'Яндекс Пробки: пробки на дорогах городов и областей России в режиме онлайн. Построение маршрутов и онлайн-навигация с учетом пробок на автомобиле и общественном транспорте на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Пробки', url: 'https://yandex.ru/maps/probki/'}
            ],
            redirectUrl: '/probki/'
        },
        {
            name: 'Старый шорткат на домене .com.tr',
            url: '/traffic/',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/trafik/',
            title: 'Canlı Yol Durumu — Yandex Haritalar',
            description:
                "Yandex Trafik: Türkiye yollarında çevrimiçi trafik durumu. Yandex Haritalar'ın web servisinde veya mobil uygulamasında, trafik durumuna göre toplu taşımayla veya özel araçla rota oluşturma ve navigasyon özelliği. Yandex Haritalar sokak, bina ve kurum bulmaya, uydu haritasını ve cadde panoramalarını görüntülemeye yardım eder.",
            breadcrumbList: [
                {name: 'Haritalar', url: 'https://yandex.com.tr/harita/'},
                {name: 'Trafik', url: 'https://yandex.com.tr/harita/trafik/'}
            ],
            redirectUrl: '/trafik/'
        },
        {
            name: 'Старый шорткат в Москве',
            url: '/moscow_traffic/',
            canonical: 'https://yandex.ru/maps/213/moscow/probki/',
            title: 'Пробки в Москве — Яндекс Карты',
            description:
                'Пробки в Москве в режиме онлайн на Яндекс Картах. Построение маршрутов и онлайн-навигация в Москве с учетом пробок на автомобиле и общественном транспорте на сайте и в мобильном приложении. Яндекс Карты помогут найти улицу, дом, организацию, посмотреть спутниковую карту и панорамы улиц городов.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Пробки', url: 'https://yandex.ru/maps/213/moscow/probki/'}
            ],
            redirectUrl: '/213/moscow/probki/'
        },

        // MAPSUI-17336: [SEO] Поддержать редиректы с /probki/ на /traffic/ на нац доменах
        {
            name: '/traffic/ на домене .com.tr',
            url: '/traffic/',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/trafik/',
            title: 'Canlı Yol Durumu — Yandex Haritalar',
            redirectUrl: '/trafik/'
        },
        {
            name: '/probki/ на домене .com.tr',
            url: '/probki/',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/trafik/',
            title: 'Canlı Yol Durumu — Yandex Haritalar',
            redirectUrl: '/trafik/'
        },
        {
            name: '/trafik/ на домене .com',
            url: '/trafik/',
            tld: 'com',
            canonical: 'https://yandex.com/maps/traffic/',
            title: 'Traffic conditions — Yandex Maps',
            redirectUrl: '/traffic/'
        },
        {
            name: '/probki/ на домене .com',
            url: '/probki/',
            tld: 'com',
            canonical: 'https://yandex.com/maps/traffic/',
            title: 'Traffic conditions — Yandex Maps',
            redirectUrl: '/traffic/'
        },
        {
            name: '/trafik/ на домене .ru',
            url: '/trafik/',
            canonical: 'https://yandex.ru/maps/probki/',
            title: 'Пробки на дорогах — Яндекс Карты',
            redirectUrl: '/probki/'
        },
        {
            name: '/traffic/ на домене .ru',
            url: '/traffic/',
            canonical: 'https://yandex.ru/maps/probki/',
            title: 'Пробки на дорогах — Яндекс Карты',
            redirectUrl: '/probki/'
        }
    ]
};

export default contents;
