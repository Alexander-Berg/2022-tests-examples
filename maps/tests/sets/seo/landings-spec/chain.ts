import {SeoFile} from '../types';

const contents: SeoFile = {
    name: 'Лендинг сети организаций',
    specs: [
        {
            name: 'Обычный',
            url: '/213/moscow/chain/shokoladnica/6002084/',
            canonical: 'https://yandex.ru/maps/213/moscow/chain/shokoladnica/6002084/',
            title: 'Шоколадница: адреса кафе в Москве — Яндекс Карты',
            description:
                'Все кафе Шоколадница на карте Москвы. Адреса, телефоны, время работы кафе. Маршруты проезда на автомобиле и общественном транспорте',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Кафе', url: 'https://yandex.ru/maps/213/moscow/category/cafe/184106390/'},
                {name: 'Шоколадница', url: 'https://yandex.ru/maps/213/moscow/chain/shokoladnica/6002084/'}
            ],
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&spn=0.290815,0.219484&ll=37.679390,55.745454&lg=0&cr=0&pt=37.657177,55.759554,round~37.622914,55.768069,round~37.605746,55.764281,round~37.593261,55.767282,round~37.678614,55.773031,round~37.635329,55.730681,round~37.578609,55.780217,round~37.595668,55.775989,round~37.679636,55.746838,round~37.682394,55.746304,round~37.558951,55.790827,round~37.634491,55.789962,round~37.570062,55.761508,round~37.617417,55.795329,round~37.637556,55.807356,round~37.580141,55.725140,round~37.568554,55.683756,round~37.612817,55.735169,round~37.667682,55.739571,round~37.564417,55.762966,round~37.664292,55.731899,round~37.748764,55.789235,round~37.615640,55.756221,round~37.588874,55.752923,round~37.658520,55.757353,round&signature=goP_8CzHfZ9-tZ7XflkGIvKaq_YR8ydFtKYjH49iKWk='
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/chain/shokoladnica/6002084/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/chain/shokoladnica/6002084/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'Без seoname',
            url: '/68/chita/chain/undefined/64259484573/',
            redirectUrl: '/68/chita/chain/64259484573/64259484573/',
            canonical: 'https://yandex.ru/maps/68/chita/chain/64259484573/64259484573/',
            title: 'Лайса: адреса в Чите — Яндекс Карты',
            description:
                'Лайса на карте Читы. Адреса, телефоны, время работы организаций. Маршруты проезда на автомобиле и общественном транспорте',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Чита', url: 'https://yandex.ru/maps/68/chita/'},
                {
                    name: 'Рекламные агентства',
                    url: 'https://yandex.ru/maps/68/chita/category/advertising_agency/184107180/'
                },
                {name: 'Лайса', url: 'https://yandex.ru/maps/68/chita/chain/64259484573/64259484573/'}
            ]
        }
    ]
};

export default contents;
