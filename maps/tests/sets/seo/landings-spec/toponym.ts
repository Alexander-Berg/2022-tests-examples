import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг топонима',
    specs: [
        {
            name: 'Страна',
            url: '/geo/53000006/',
            canonical: 'https://yandex.ru/maps/geo/53000006/',
            h1: 'Беларусь',
            title: 'Беларусь',
            og: {
                title: 'Беларусь'
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/geo/53000006/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/geo/53000006/',
                    hreflang: 'en'
                }
            ],
            noIndex: false
        },
        {
            name: 'Область',
            url: '/geo/tverskaya_oblast/53000081/',
            canonical: 'https://yandex.ru/maps/geo/tverskaya_oblast/53000081/',
            h1: 'Тверская область',
            title: 'Тверская область',
            og: {
                title: 'Тверская область'
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/geo/tverskaya_oblast/53000081/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/geo/tverskaya_oblast/53000081/',
                    hreflang: 'en'
                }
            ],
            noIndex: false
        },
        {
            name: 'Город',
            url: '/geo/53063504/',
            canonical: 'https://yandex.ru/maps/geo/reutov/53063504/',
            h1: 'Реутов',
            title: 'Реутов',
            og: {
                title: 'Реутов'
            },
            redirectUrl: '/geo/reutov/53063504/',
            noIndex: false
        },
        {
            name: 'Город без seoname',
            url: '/geo/53177019/',
            canonical: 'https://yandex.ru/maps/geo/53177019/',
            h1: 'Минск',
            title: 'Минск',
            og: {
                title: 'Минск'
            },
            noIndex: false
        },
        {
            name: 'Район',
            url: '/213/moscow/geo/rayon_khamovniki/53211698/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/',
            h1: 'район Хамовники',
            title: 'Район Хамовники в городе Москва — Яндекс Карты',
            description:
                'Район Хамовники: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'}
            ],
            og: {
                title: 'Район Хамовники в городе Москва — Яндекс Карты',
                description:
                    'Район Хамовники: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты'
            },
            noIndex: false
        },
        {
            name: 'Квартал в районе',
            url: '/213/moscow/geo/kvartal_krasnaya_roza/3358968755/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/kvartal_krasnaya_roza/3358968755/',
            h1: 'квартал Красная Роза',
            title: 'Квартал Красная Роза в городе Москва — Яндекс Карты',
            description:
                'Квартал Красная Роза: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Квартал Красная Роза',
                    url: 'https://yandex.ru/maps/213/moscow/geo/kvartal_krasnaya_roza/3358968755/'
                }
            ],
            og: {
                title: 'Квартал Красная Роза в городе Москва — Яндекс Карты',
                description:
                    'Квартал Красная Роза: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты'
            },
            noIndex: false
        },
        {
            name: 'Гидро',
            url: '/213/moscow/geo/bolshoy_novodevichiy_prud/164140908/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/bolshoy_novodevichiy_prud/164140908/',
            h1: 'Большой Новодевичий пруд',
            title: 'Большой Новодевичий пруд в городе Москва — Яндекс Карты',
            description:
                'Большой Новодевичий пруд: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Большой Новодевичий пруд',
                    url: 'https://yandex.ru/maps/213/moscow/geo/bolshoy_novodevichiy_prud/164140908/'
                }
            ],
            og: {
                title: 'Большой Новодевичий пруд в городе Москва — Яндекс Карты',
                description:
                    'Большой Новодевичий пруд: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты'
            },
            noIndex: false
        },
        {
            name: 'Улица',
            url: '/213/moscow/geo/ulitsa_lva_tolstogo/8059948/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/ulitsa_lva_tolstogo/8059948/',
            h1: 'улица Льва Толстого',
            title: 'Улица Льва Толстого в городе Москва — Яндекс Карты',
            description:
                'Улица Льва Толстого: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {name: 'Улица Льва Толстого', url: 'https://yandex.ru/maps/213/moscow/geo/ulitsa_lva_tolstogo/8059948/'}
            ],
            og: {
                title: 'Улица Льва Толстого в городе Москва — Яндекс Карты',
                description:
                    'Улица Льва Толстого: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты'
            },
            noIndex: false
        },
        {
            name: 'Парк',
            url: '/213/moscow/geo/park_novodevichyi_prudy/121373232/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/park_novodevichyi_prudy/121373232/',
            h1: 'парк Новодевичьи Пруды',
            title: 'Парк Новодевичьи Пруды в городе Москва — Яндекс Карты',
            description:
                'Парк Новодевичьи Пруды: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/geo/moskva/53166393/'},
                {name: 'Район Хамовники', url: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/'},
                {
                    name: 'Парк Новодевичьи Пруды',
                    url: 'https://yandex.ru/maps/213/moscow/geo/park_novodevichyi_prudy/121373232/'
                }
            ],
            og: {
                title: 'Парк Новодевичьи Пруды в городе Москва — Яндекс Карты',
                description:
                    'Парк Новодевичьи Пруды: информация и подробная информация об объекте - транспорт, адреса, организации рядом — Яндекс Карты'
            },
            noIndex: false
        }
    ]
};

export default contents;
