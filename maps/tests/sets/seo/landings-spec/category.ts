import {SeoFile, SeoBreadcrumb} from '../types';

const categoryBreadcrumbs = {
    hotel: getCategoryBreadcrumb('Гостиницы', 'hotel', '184106414'),
    cafe: getCategoryBreadcrumb('Кафе', 'cafe', '184106390'),
    restaurants: getCategoryBreadcrumb('Рестораны', 'restaurant', '184106394'),
    carDealer: getCategoryBreadcrumb('Автосалоны', 'car_dealership', '184105322'),
    cosmetology: getCategoryBreadcrumb('Косметология', 'cosmetology', '892508964')
};

const contents: SeoFile = {
    name: 'Лендинг категории',
    specs: [
        {
            name: 'Обычный',
            url: '/213/moscow/category/hotel/',
            canonical: 'https://yandex.ru/maps/213/moscow/category/hotel/184106414/',
            redirectUrl: '/213/moscow/category/hotel/184106414/',
            title: 'Гостиницы в Москве, гостиницы рядом со мной на карте — Яндекс Карты',
            description:
                'Гостиницы в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.hotel),
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&spn=0.145407,0.109704&ll=37.610048,55.759049&lg=0&cr=0&pt=37.612982,55.757364,round~37.616459,55.756649,round~37.621501,55.758365,round~37.625899,55.748215,round~37.625871,55.758546,round~37.621511,55.760043,round~37.623439,55.760349,round~37.621503,55.758407,round~37.614112,55.756791,round~37.616557,55.762980,round~37.616351,55.764323,round~37.654616,55.744383,round~37.632189,55.760998,round~37.625344,55.757456,round~37.643862,55.733434,round~37.627840,55.737785,round~37.628177,55.746587,round~37.619642,55.763507,round~37.633612,55.755811,round~37.565479,55.751505,round~37.626166,55.784722,round~37.602158,55.765331,round~37.604153,55.757976,round~37.599406,55.768348,round~37.632483,55.757095,round&signature=jJq5yhZADz6jLw33sK1bANM5TwWazVFy3Nzat4w4wj4='
            },
            alternates: [
                {
                    href: 'https://yandex.ru/maps/213/moscow/category/hotel/184106414/',
                    hreflang: 'ru'
                },
                {
                    href: 'https://yandex.com/maps/213/moscow/category/hotel/184106414/',
                    hreflang: 'en'
                }
            ]
        },
        {
            name: 'С id категории',
            url: '/213/moscow/category/hotel/184106414/',
            canonical: 'https://yandex.ru/maps/213/moscow/category/hotel/184106414/',
            title: 'Гостиницы в Москве, гостиницы рядом со мной на карте — Яндекс Карты',
            description:
                'Гостиницы в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.hotel)
        },
        {
            name: 'Гео: район.',
            url: '/213/moscow/geo/hamovniki/53211698/category/cafe/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/category/cafe/184106390/',
            redirectUrl: '/213/moscow/geo/rayon_khamovniki/53211698/category/cafe/184106390/',
            title: 'Кафе в Хамовниках, Москва — Яндекс Карты',
            description:
                'Кафе в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.cafe)
        },
        {
            name: 'Гео: метро.',
            url: '/213/moscow/geo/metro_arbatskaya/100079048/category/cafe/184106390/',
            canonical: 'https://yandex.ru/maps/213/moscow/geo/metro_arbatskaya/100079048/category/cafe/184106390/',
            redirectUrl: '/213/moscow/geo/metro_arbatskaya/100079048/category/cafe/184106390/',
            title: 'Кафе на Арбатской в Москве — Яндекс Карты',
            description:
                'Кафе в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.cafe)
        },
        {
            name: 'Гео: аэропорт.',
            url: '/213/moscow/geo/aeroport_sheremetyevo_imeni_a_s_pushkina/1519988203/category/cafe/184106390/',
            canonical:
                'https://yandex.ru/maps/213/moscow/geo/aeroport_sheremetyevo_imeni_a_s_pushkina/1519988203/category/cafe/184106390/',
            redirectUrl: '/213/moscow/geo/aeroport_sheremetyevo_imeni_a_s_pushkina/1519988203/category/cafe/184106390/',
            title: 'Кафе в Шереметьево, Москва — Яндекс Карты',
            description:
                'Кафе в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.cafe)
        },
        {
            name: 'Фильтр: булевый',
            url: '/213/moscow/category/restaurant/filter/business_lunch/',
            canonical: 'https://yandex.ru/maps/213/moscow/category/restaurant/184106394/filter/business_lunch/',
            redirectUrl: '/213/moscow/category/restaurant/184106394/filter/business_lunch/',
            title: 'Бизнес-ланч — рестораны в Москве, рестораны рядом со мной на карте — Яндекс Карты',
            description:
                'Бизнес-ланч — рестораны в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.restaurants)
        },
        {
            name: 'Фильтр: перечисление',
            url: '/213/moscow/category/restaurant/filter/price_category/price_reasonable/',
            canonical:
                'https://yandex.ru/maps/213/moscow/category/restaurant/184106394/filter/price_category/price_reasonable/',
            redirectUrl: '/213/moscow/category/restaurant/184106394/filter/price_category/price_reasonable/',
            title: 'Цены: средние — рестораны в Москве, рестораны рядом со мной на карте — Яндекс Карты',
            description:
                'Цены: средние — рестораны в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.restaurants)
        },
        {
            name: 'Фильтр: автосалон + выбрана марка автомобиля',
            url: '/213/moscow/category/car_dealership/filter/car_brand/ford/',
            canonical: 'https://yandex.ru/maps/213/moscow/category/car_dealership/184105322/filter/car_brand/ford/',
            redirectUrl: '/213/moscow/category/car_dealership/184105322/filter/car_brand/ford/',
            // Не "марка автомобиля: Ford"
            title: 'Ford — автосалоны в Москве, автосалоны рядом со мной на карте — Яндекс Карты',
            description:
                'Ford — автосалоны в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.carDealer)
        },
        {
            name: 'Фильтр: кухни',
            url: '/213/moscow/category/restaurant/filter/type_cuisine/italian_cuisine/',
            canonical:
                'https://yandex.ru/maps/213/moscow/category/restaurant/184106394/filter/type_cuisine/italian_cuisine/',
            redirectUrl: '/213/moscow/category/restaurant/184106394/filter/type_cuisine/italian_cuisine/',
            title: 'Итальянская кухня — рестораны в Москве, рестораны рядом со мной на карте — Яндекс Карты',
            description:
                'Итальянская кухня — рестораны в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.restaurants)
        },
        {
            name: 'Фильтр: топ-фильтр',
            url: '/213/moscow/category/cosmetology/filter/cosmetology_services/eyebrow/',
            canonical:
                'https://yandex.ru/maps/213/moscow/category/cosmetology/892508964/filter/cosmetology_services/eyebrow/',
            redirectUrl: '/213/moscow/category/cosmetology/892508964/filter/cosmetology_services/eyebrow/',
            title: 'Коррекция бровей — косметология в Москве, косметология рядом со мной на карте — Яндекс Карты',
            // Не "косметологические услуги: корреция бровей"
            description:
                'Коррекция бровей — косметология в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.cosmetology)
        },
        {
            name: 'Гео + фильтр.',
            url: '/213/moscow/geo/rayon_khamovniki/53211698/category/restaurant/filter/type_cuisine/italian_cuisine/',
            canonical:
                'https://yandex.ru/maps/213/moscow/geo/rayon_khamovniki/53211698/category/restaurant/184106394/filter/type_cuisine/italian_cuisine/',
            redirectUrl:
                '/213/moscow/geo/rayon_khamovniki/53211698/category/restaurant/184106394/filter/type_cuisine/italian_cuisine/',
            title: 'Итальянская кухня — рестораны в Хамовниках, Москва — Яндекс Карты',
            description:
                'Итальянская кухня — рестораны в Москве с адресами, телефонами, отзывами. Карты покажут режим работы, панорамы и фото места, помогут добраться общественным транспортом, пешком или на машине.',
            breadcrumbList: getBreadcrumbList(categoryBreadcrumbs.restaurants),
            og: {
                image:
                    'https://static-maps.yandex.ru/1.x/?api_key=01931952-3aef-4eba-951a-8afd26933ad6&theme=light&lang=ru_RU&size=520,440&l=map&spn=0.072704,0.054892&ll=37.576582,55.730441&lg=0&cr=0&pt=37.578093,55.743626,round~37.595950,55.736863,round~37.584917,55.721979,round~37.575103,55.739016,round~37.588323,55.746203,round~37.592343,55.735421,round~37.571174,55.713760,round~37.579945,55.726429,round~37.576949,55.730963,round~37.579192,55.730283,round~37.584063,55.735751,round~37.567636,55.730123,round~37.567846,55.727730,round~37.583320,55.736072,round~37.581090,55.738398,round~37.580382,55.746269,round~37.561853,55.732952,round~37.584907,55.735075,round~37.566764,55.752323,round~37.571061,55.746002,round~37.581185,55.746421,round~37.609837,55.727318,round~37.574793,55.739041,round~37.609613,55.740999,round~37.594297,55.754026,round&signature=tYfHT5JiPQRb_-sIEQwDbWugje5xIwAA28XtwG3hRHU='
            }
        }
    ]
};

function getCategoryBreadcrumb(name: string, seoname: string, id: string): SeoBreadcrumb {
    return {name, url: `https://yandex.ru/maps/213/moscow/category/${seoname}/${id}/`};
}

function getBreadcrumbList(lastBreadcrumb: SeoBreadcrumb): SeoBreadcrumb[] {
    return [
        {name: 'Карты', url: 'https://yandex.ru/maps/'},
        {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
        {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
        lastBreadcrumb
    ];
}

export default contents;
