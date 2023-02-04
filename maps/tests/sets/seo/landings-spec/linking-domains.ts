import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Провязывание доменов между собой',
    specs: [
        {
            name: '.com',
            tld: 'com',
            url: '/org/natakhtari/80353362241/',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/org/natakhtari/80353362241/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/org/natakhtari/80353362241/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: '.com и объект из национального домена',
            tld: 'com',
            url: '/org/etno_memorialny_kompleks_atameken/24016800852/',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'en'
                },
                {
                    href: 'https://yandex.kz/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'kk-KZ'
                }
            ]
        },
        {
            name: '.ru',
            tld: 'ru',
            url: '/org/natakhtari/80353362241/',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/org/natakhtari/80353362241/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/org/natakhtari/80353362241/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: '.ru и объект из национального домена',
            url: '/org/etno_memorialny_kompleks_atameken/24016800852/',
            tld: 'ru',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'en'
                },
                {
                    href: 'https://yandex.kz/maps/org/etno_memorialny_kompleks_atameken/24016800852/',
                    hreflang: 'kk-KZ'
                }
            ]
        },
        {
            name: 'Национальный домен',
            tld: 'kz',
            url: '/org/24016800852/',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/org/24016800852/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/org/24016800852/',
                    hreflang: 'en'
                },
                {
                    href: 'https://yandex.kz/maps/org/24016800852/',
                    hreflang: 'kk-KZ'
                }
            ]
        }
    ]
};

export default contents;
