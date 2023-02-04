import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг морды',
    specs: [
        {
            name: '.ru',
            url: '/',
            canonical: 'https://yandex.ru/maps/',
            title: 'Яндекс Карты — транспорт, навигация, поиск мест',
            description:
                'Карты помогут найти нужное место даже без точного адреса и построят до него маршрут на общественном транспорте, автомобиле или пешком.',
            breadcrumbList: [{name: 'Карты', url: 'https://yandex.ru/maps/'}]
        },
        {
            name: '.com.tr',
            url: '/',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/',
            title: 'Yandex Maps: Ulaşım, navigasyon, yer arama',
            description:
                'Yandex Haritalar tam adresi bilmediğinizde bile istediğiniz yeri bulmanıza yardımcı olur ve oraya toplu taşıma, araç veya yürüyüş rotası oluşturur.',
            breadcrumbList: [{name: 'Haritalar', url: 'https://yandex.com.tr/harita/'}],
            alternates: [
                {
                    href: 'https://yandex.ru/harita/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/harita/',
                    hreflang: 'en'
                },
                {
                    href: 'https://yandex.com.tr/harita/',
                    hreflang: 'tr-TR'
                }
            ]
        },
        {
            name: '.com',
            url: '/',
            tld: 'com',
            canonical: 'https://yandex.com/maps/',
            title: 'Yandex Maps: search for places, transport, and routes',
            description:
                "Yandex Maps will help you find your destination even if you don't have the exact address — get a route for taking public transport, driving, or walking.",
            breadcrumbList: [{name: 'Maps', url: 'https://yandex.com/maps/'}]
        }
    ]
};

export default contents;
