import {SeoCase, SeoFile} from '../types';

const BASE: Pick<SeoCase, 'breadcrumbList'> = {
    breadcrumbList: [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'}
    ]
};

const contents: SeoFile = {
    name: 'Лендинг панели ОТ',
    specs: [
        {
            name: 'Автобусы',
            ...BASE,
            url: '/213/moscow/transport/buses/',
            canonical: 'https://yandex.ru/maps/213/moscow/transport/buses/',
            h1: 'Маршруты автобусов',
            title: 'Маршруты автобусов на карте Москвы — Яндекс Карты',
            description:
                'Автобусы в Москве: расписание, маршруты, остановки, местоположение автобусов на карте в режиме онлайн. Общественный транспорт Москвы на Яндекс Картах.',
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/transport/buses/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/transport/buses/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'Троллейбусы',
            ...BASE,
            url: '/213/moscow/transport/trolleybuses/',
            canonical: 'https://yandex.ru/maps/213/moscow/transport/trolleybuses/',
            h1: 'Маршруты троллейбусов',
            title: 'Маршруты троллейбусов на карте Москвы — Яндекс Карты',
            description:
                'Троллейбусы в Москве: расписание, маршруты, остановки, местоположение троллейбусов на карте в режиме онлайн. Общественный транспорт Москвы на Яндекс Картах.'
        },
        {
            name: 'Трамваи',
            ...BASE,
            url: '/213/moscow/transport/trams/',
            canonical: 'https://yandex.ru/maps/213/moscow/transport/trams/',
            h1: 'Маршруты трамваев',
            title: 'Маршруты трамваев на карте Москвы — Яндекс Карты',
            description:
                'Трамваи в Москве: расписание, маршруты, остановки, местоположение трамваев на карте в режиме онлайн. Общественный транспорт Москвы на Яндекс Картах.'
        },
        {
            name: 'Маршрутки',
            ...BASE,
            url: '/213/moscow/transport/minibuses/',
            canonical: 'https://yandex.ru/maps/213/moscow/transport/minibuses/',
            h1: 'Движение маршруток',
            title: 'Маршрутки на карте Москвы — Яндекс Карты',
            description:
                'Маршрутные такси в Москве: расписание, маршруты, остановки, местоположение маршруток на карте в режиме онлайн. Общественный транспорт Москвы на Яндекс Картах.'
        }
    ]
};

export default contents;
