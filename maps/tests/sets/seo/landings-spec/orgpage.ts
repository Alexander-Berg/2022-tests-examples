import {getSchemaPropSelector, getSchemaScopeSelector} from '../utils';
import {SeoFile} from '../types';
import cssSelectors from '../../../common/css-selectors';
import {Tld} from '../../../lib/func/url';

const getOrgpageMeta = (tld: Tld) => [
    {
        selector: getSchemaScopeSelector('LocalBusiness'),
        amount: 1
    },
    {
        selector: getSchemaPropSelector('name', 'LocalBusiness'),
        value: tld === 'tr' ? 'Natakhtari' : 'Натахтари'
    },
    {
        selector: getSchemaPropSelector('telephone', 'LocalBusiness'),
        value: '+7 (495) 924-94-44'
    },
    {
        selector: getSchemaPropSelector('image', 'LocalBusiness'),
        content:
            'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAFoEvQfAAAABGdBTUEAALGPC/xhBQAAAA1JREFUCB1j+Pd96X8ACSsDmpuH9AEAAAAASUVORK5CYII='
    },
    {
        selector: getSchemaPropSelector('url', 'LocalBusiness'),
        value: 'cafenatahtari.ru'
    },
    {
        selector: getSchemaPropSelector('address', 'LocalBusiness'),
        content:
            tld === 'tr'
                ? 'Russia, Moscow, Bolshoy Cherkassky Lane, 13с4, 109012'
                : 'Россия, Москва, Большой Черкасский переулок, 13с4, 109012'
    },
    {
        selector: getSchemaPropSelector('priceRange', 'LocalBusiness'),
        content: '1000–1500 ₽'
    },
    {
        selector: getSchemaPropSelector('openingHours', 'LocalBusiness'),
        content:
            tld === 'tr'
                ? [
                      'Mo 11:00-22:30',
                      'Tu 11:00-22:30',
                      'We 11:00-22:30',
                      'Th 11:00-22:30',
                      'Fr 11:00-22:30',
                      'Sa 11:00-22:30',
                      'Su 11:00-22:30'
                  ]
                : [
                      'Mo 11:00-23:00',
                      'Tu 11:00-23:00',
                      'We 11:00-23:00',
                      'Th 11:00-23:00',
                      'Fr 11:00-23:00',
                      'Sa 11:00-23:00',
                      'Su 11:00-23:00'
                  ]
    },
    {
        selector: getSchemaPropSelector('sameAs', 'LocalBusiness'),
        amount: 2
    },
    {
        selector: getSchemaScopeSelector('SubwayStation'),
        amount: 3
    },
    {
        selector: getSchemaScopeSelector('BusStop'),
        amount: 5
    }
];

const ratingMeta = [
    {
        selector: getSchemaScopeSelector('AggregateRating'),
        amount: 1
    },
    {
        selector: getSchemaPropSelector('reviewCount', 'AggregateRating'),
        content: '326'
    },
    {
        selector: getSchemaPropSelector('bestRating', 'AggregateRating'),
        content: '5'
    },
    {
        selector: getSchemaPropSelector('worstRating', 'AggregateRating'),
        content: '1'
    },
    {
        selector: getSchemaPropSelector('ratingCount', 'AggregateRating'),
        content: '794'
    },
    {
        selector: getSchemaPropSelector('ratingValue', 'AggregateRating'),
        content: '4.6'
    },
    {
        selector: getSchemaScopeSelector('Rating'),
        amount: 3
    },
    {
        selector: getSchemaPropSelector('bestRating', 'Rating'),
        content: ['5', '5', '5']
    },
    {
        selector: getSchemaPropSelector('worstRating', 'Rating'),
        content: ['1', '1', '1']
    },
    {
        selector: getSchemaPropSelector('ratingValue', 'Rating'),
        content: ['2.0', '5.0', '4.0']
    }
];

const reviewsMeta = [
    {
        selector: getSchemaPropSelector('review', 'LocalBusiness'),
        amount: 5
    },
    {
        selector: `${cssSelectors.ugc.review.card.item} [itemType="http://schema.org/Review"]`,
        amount: 5
    },
    {
        selector: getSchemaPropSelector('name', 'Person'),
        amount: 5
    },
    {
        selector: getSchemaPropSelector('author', 'Review'),
        amount: 5
    },
    {
        selector: getSchemaPropSelector('image', 'Person'),
        content: [
            'https://avatars.mds.yandex.net/get-yapic/60687/enc-b39c58e347ad1fe66216460c5f23de72d06d7da692952ec33e1aeca6dbc76a7f/islands-68'
        ]
    },
    {
        selector: getSchemaPropSelector('datePublished', 'Review'),
        content: [
            '2018-11-26T07:15:57.538Z',
            '2018-11-20T10:37:14.000Z',
            '2018-11-18T16:52:15.162Z',
            '2018-11-16T17:49:58.337Z',
            '2018-11-10T11:58:19.149Z'
        ]
    },
    {
        selector: getSchemaPropSelector('reviewBody', 'Review'),
        value: [
            'Одно из лучьших заведентй грузинской кухни',
            'Кухня так себе, официант попался не приветливый. Заказали водку Белуга, явно принесли не её!!!',
            'Очень вкусная кухня. Адекватные цены. Красивый интерьер.',
            'Среди ресторанов грузинской кухни в Москве Натахтаре у нас на первом месте.',
            'Очень нравится эта сеть. Всегда дружелюбные официанты, приятная атмосфера и оооочень вкусные блюда. Хинкали и хачапури по аджарски - восторг!'
        ]
    }
];

const contents: SeoFile = {
    name: 'Лендинг организации',
    specs: [
        {
            name: 'Обычный',
            url: '/org/natakhtari/80353362241/',
            canonical: 'https://yandex.ru/maps/org/natakhtari/80353362241/',
            h1: 'Натахтари',
            title: 'Натахтари, кафе, Большой Черкасский пер., 13, стр. 4 — Яндекс Карты',
            description:
                'Натахтари ⭐ , Ⓜ Китай-город, Большой Черкасский пер., 13, стр. 4: ✔ фотографии, адрес и ☎️ телефон, часы работы, фото и отзывы посетителей на Яндекс Картах.',
            schemaVerifications: [...getOrgpageMeta('ru'), ...ratingMeta],
            og: {
                title: 'Натахтари, кафе, Большой Черкасский пер., 13, стр. 4 — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.',
                image:
                    'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAFoEvQfAAAABGdBTUEAALGPC/xhBQAAAA1JREFUCB1j+Pd96X8ACSsDmpuH9AEAAAAASUVORK5CYII='
            },
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Кафе', url: 'https://yandex.ru/maps/213/moscow/category/cafe/184106390/'},
                {name: 'Натахтари', url: 'https://yandex.ru/maps/org/natakhtari/80353362241/'}
            ],
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
            name: 'Сетевая организация',
            url: '/org/dzhondzholi/6153681391/',
            canonical: 'https://yandex.ru/maps/org/dzhondzholi/6153681391/',
            h1: 'Джонджоли',
            title: 'Джонджоли, ресторан, просп. Мира, 118, Москва — Яндекс Карты',
            description:
                'Джонджоли ⭐ , Ⓜ Алексеевская, просп. Мира, 118, Москва: ✔ фотографии, адрес и ☎️ телефон, часы работы, фото и отзывы посетителей на Яндекс Картах.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Рестораны', url: 'https://yandex.ru/maps/213/moscow/category/restaurant/184106394/'},
                {name: 'Джонджоли', url: 'https://yandex.ru/maps/213/moscow/chain/dzhondzholi/240324490423/'},
                {name: 'Джонджоли', url: 'https://yandex.ru/maps/org/dzhondzholi/6153681391/'}
            ]
        },
        {
            name: '.com.tr',
            url: '/org/natakhtari/80353362241/',
            mockVersion: '2',
            tld: 'tr',
            canonical: 'https://yandex.com.tr/harita/org/natakhtari/80353362241/',
            h1: 'Natakhtari',
            title: 'Natakhtari, kafe, Russia, Moscow, Bolshoy Cherkassky Lane, 13с4 - Yandex Haritalar',
            description:
                "Natakhtari ⭐ , Ⓜ Китай-город, Russia, Moscow, Bolshoy Cherkassky Lane, 13с4: ✔ fotoğrafları, adresi, ☎️ telefon numarası, çalışma saatleri, fotoğrafları ve müşteri yorumları Yandex Haritalar'da.",
            breadcrumbList: [
                {name: 'Haritalar', url: 'https://yandex.com.tr/harita/'},
                {name: 'Moskova', url: 'https://yandex.com.tr/harita/213/moscow/'},
                {name: 'Kafe', url: 'https://yandex.com.tr/harita/213/moscow/category/cafe/184106390/'},
                {name: 'Natakhtari', url: 'https://yandex.com.tr/harita/org/natakhtari/80353362241/'}
            ],
            schemaVerifications: getOrgpageMeta('tr'),
            canonicalBrowserPath: '/org/natakhtari/80353362241/'
        },
        {
            name: 'Лендинг отзывов',
            url: '/org/natakhtari/80353362241/reviews/',
            canonical: 'https://yandex.ru/maps/org/natakhtari/80353362241/reviews/',
            h1: 'Натахтари',
            title:
                'Отзывы о «Натахтари» на Китай-городе, Россия, Москва, Большой Черкасский переулок, 13с4 — Яндекс Карты',
            description:
                '794 отзыва посетителей ✉ «Натахтари» Россия, Москва, Большой Черкасский переулок, 13с4 с фото, рейтингом и информацией об организации.',
            og: {
                title:
                    'Отзывы о «Натахтари» на Китай-городе, Россия, Москва, Большой Черкасский переулок, 13с4 — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/natakhtari/80353362241/reviews/',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Кафе', url: 'https://yandex.ru/maps/213/moscow/category/cafe/184106390/'},
                {name: 'Натахтари', url: 'https://yandex.ru/maps/org/natakhtari/80353362241/'}
            ],
            schemaVerifications: reviewsMeta
        },
        {
            name: 'Лендинг меню',
            url: '/org/kave_krymskaya_kukhnya/40030534995/menu/',
            canonical: 'https://yandex.ru/maps/org/kave_krymskaya_kukhnya/40030534995/menu/',
            h1: 'Къаве. Крымская кухня',
            title: 'Меню и цены «Къаве. Крымская кухня» на Профсоюзной в Москве — Яндекс Карты',
            description:
                'Полное меню «Къаве. Крымская кухня» в Москве с фотографиями и ценами. Смотрите меню, рейтинг и отзывы о местах в Москве на Яндекс Картах.',
            og: {
                title: 'Меню и цены «Къаве. Крымская кухня» на Профсоюзной в Москве — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/kave_krymskaya_kukhnya/40030534995/menu/',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Кафе', url: 'https://yandex.ru/maps/213/moscow/category/cafe/184106390/'},
                {name: 'Къаве. Крымская кухня', url: 'https://yandex.ru/maps/org/kave_krymskaya_kukhnya/40030534995/'}
            ],
            jsonLd: {
                type: 'Menu',
                value: {
                    '@context': 'https://schema.org',
                    '@type': 'Menu',
                    hasMenuSection: [
                        {
                            '@type': 'MenuSection',
                            name: 'Выпечка',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Янтыкъ с сыром и зеленью'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Горячее (мясные блюда / Рыба и дары моря)',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Медальоны из говядины'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Горячее (основное меню)',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Плов с рапанами'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Завтрак - бранч целый день',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Советский завтрак'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Морепродукты',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Мидии'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Мясо / Meat / Горячие мясные блюда',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Мясной Сай'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Рыба',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Барабуля'
                                }
                            ]
                        },
                        {
                            '@type': 'MenuSection',
                            name: 'Салат',
                            hasMenuItem: [
                                {
                                    '@type': 'MenuItem',
                                    name: 'Салат с хрустящими баклажанами и кинзой'
                                }
                            ]
                        }
                    ]
                }
            }
        },
        {
            name: 'Лендинг цен',
            url: '/org/fjord/108891128119/prices/',
            canonical: 'https://yandex.ru/maps/org/fjord/108891128119/prices/',
            h1: 'Fjord',
            title: 'Цены «Fjord» на Третьяковской в Москве — Яндекс Карты',
            description:
                'Актуальные цены «Fjord» на Третьяковской в Москве: описание, фотографии, телефоны и отзывы — Яндекс Карты.',
            og: {
                title: 'Цены «Fjord» на Третьяковской в Москве — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/fjord/108891128119/prices/',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Барбершопы', url: 'https://yandex.ru/maps/213/moscow/category/barber_shop/239628851835/'},
                {name: 'Fjord', url: 'https://yandex.ru/maps/org/fjord/108891128119/'}
            ],
            jsonLd: {
                type: 'Product',
                value: {
                    '@context': 'https://schema.org',
                    '@graph': [
                        {
                            '@type': 'Product',
                            name: 'Бритье головы',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/2773996/2a00000170d4659100a41125a231e0235b6b/medium',
                            offers: {
                                '@type': 'Offer',
                                price: '1000',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Бритье опасной бритвой',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/218134/2a0000016c0f583c999b70442b4b69d093b7/medium',
                            description:
                                'Процедура выполняется с помощью опасной бритвы. В стоимость входит нанесение средств, включающие эфирные масла, распаривание перед бритьем и увлажнение кожи после бритья с использованием специальных профессиональных средств из Италии.',
                            offers: {
                                '@type': 'Offer',
                                price: '1800',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Детская стрижка',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/2757887/2a00000170d467dcd9585df55fd62eb013ee/medium',
                            description:
                                'Стрижка для наших маленьких клиентов в возрасте от 5 до 12 лет. Мастера выслушают Ваши пожелания или помогут подобрать стрижку подходящую типу лица и структуре волос. В стоимость стрижки входит мытье головы и укладка профессиональными средствами.',
                            offers: {
                                '@type': 'Offer',
                                price: '1400',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Мужская стрижка',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/1644120/2a0000016c0f56a58b5b2738e1318824ec0e/medium',
                            description:
                                'Мастера Fjord работают со стрижками любого уровня сложности и стиля. Ребята внимательно выслушают предпочтения или помогут подобрать ту стрижку, которая подойдет вашему типу лица, структуре волос и стилю жизни. Помимо самой стрижки в услугу входит мы',
                            offers: {
                                '@type': 'Offer',
                                price: '2000',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Оформление бороды и усов',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/1530321/2a0000016c0f59171f8fe3976d5ce89bcf5e/medium',
                            description:
                                'При оформлении бороды и усов наши мастера, выслушают Ваши пожелания или предложат сделать форму бороды, подходящую именно Вам. В стоимость так же входит использование средств для ухода за бородой.',
                            offers: {
                                '@type': 'Offer',
                                price: '1200',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Первая стрижка',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/1521147/2a0000016c0f560ca5cb37491e5c7b245e8c/medium',
                            description:
                                'Мастера Fjord работают со стрижками любого уровня сложности и стиля. Ребята внимательно выслушают предпочтения или помогут подобрать ту стрижку, которая подойдет вашему типу лица, структуре волос и стилю жизни. Помимо самой стрижки в услугу входит мы',
                            offers: {
                                '@type': 'Offer',
                                price: '1700',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        },
                        {
                            '@type': 'Product',
                            name: 'Стрижка машинкой',
                            category: 'Стрижки',
                            image:
                                'https://avatars.mds.yandex.net/get-sprav-products/1358381/2a0000016c0f573f9915ee35426e425b49d9/medium',
                            description:
                                'Стрижка машинкой одной или двумя насадками. В стоимость услуги входит мытье головы, стрижка, а так же укладка с помощью профессиональных средств, подобранных для Ваших волос.',
                            offers: {
                                '@type': 'Offer',
                                price: '1000',
                                priceCurrency: 'RUB',
                                availability: 'InStock'
                            }
                        }
                    ]
                }
            }
        },
        {
            name: 'Лендинг организаций внутри организации',
            url: '/org/petrovskiy/38471990018/inside/',
            canonical: 'https://yandex.ru/maps/org/petrovskiy/38471990018/inside/',
            h1: 'Петровский',
            title: 'Организации внутри «Петровский» — Яндекс Карты',
            description: 'Все организации внутри «Петровский» — Яндекс Карты.',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Торговые центры', url: 'https://yandex.ru/maps/213/moscow/category/shopping_mall/184108083/'},
                {name: 'Петровский', url: 'https://yandex.ru/maps/org/petrovskiy/38471990018/'}
            ],
            og: {
                title: 'Организации внутри «Петровский» — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/petrovskiy/38471990018/inside/'
        },
        {
            name: 'Лендинг панорамы',
            url: '/org/petrovskiy/38471990018/panorama/',
            canonical: 'https://yandex.ru/maps/org/petrovskiy/38471990018/panorama/',
            title: 'Панорама: Петровский, торговый центр, Новопетровская ул., 6 — Яндекс Карты',
            description: 'Панорамы места Петровский от Яндекс Карт',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Торговые центры', url: 'https://yandex.ru/maps/213/moscow/category/shopping_mall/184108083/'},
                {name: 'Петровский', url: 'https://yandex.ru/maps/org/petrovskiy/38471990018/'}
            ],
            og: {
                title: 'Панорама: Петровский, торговый центр, Новопетровская ул., 6 — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/petrovskiy/38471990018/panorama/'
        },
        {
            name: 'Лендинг фото',
            url: '/org/petrovskiy/38471990018/gallery/',
            canonical: 'https://yandex.ru/maps/org/petrovskiy/38471990018/gallery/',
            title: 'Фото: Петровский, торговый центр, Новопетровская ул., 6 — Яндекс Карты',
            description: 'Фотографии места Петровский от организации и пользователей Яндекс Карт',
            breadcrumbList: [
                {name: 'Карты', url: 'https://yandex.ru/maps/'},
                {name: 'Москва', url: 'https://yandex.ru/maps/213/moscow/'},
                {name: 'Каталог организаций', url: 'https://yandex.ru/maps/213/moscow/catalog/'},
                {name: 'Торговые центры', url: 'https://yandex.ru/maps/213/moscow/category/shopping_mall/184108083/'},
                {name: 'Петровский', url: 'https://yandex.ru/maps/org/petrovskiy/38471990018/'}
            ],
            og: {
                title: 'Фото: Петровский, торговый центр, Новопетровская ул., 6 — Яндекс Карты',
                description:
                    'Поможем найти информацию о местных компаниях: адреса, телефоны, фото и отзывы. Построить маршрут пешком, на общественном транспорте или на автомобиле с учётом пробок на картах.'
            },
            canonicalBrowserPath: '/org/petrovskiy/38471990018/gallery/'
        },
        {
            name: 'Лендинг новостей',
            url: '/org/kusochki/1292093992/posts/',
            title: 'Кусочки — новости кафе в Москве, улица Шаболовка, 63, корп. 1 — Яндекс Карты',
            description:
                'Новости, события и акции кафе «Кусочки», Ⓜ Шаболовская, Россия, Москва, улица Шаболовка, 63, корп. 1.'
        },
        {
            name: 'Лендинг особенностей',
            url: '/org/kusochki/1292093992/features/',
            title: 'Особенности кафе Кусочки в Москве, улица Шаболовка, 63, корп. 1 — Яндекс Карты',
            description:
                'Подробная информация про кафе «Кусочки», Ⓜ Шаболовская, Россия, Москва, улица Шаболовка, 63, корп. 1.'
        },
        {
            name: 'Лендинг похожих мест',
            url: '/org/myata_lounge/234913491786/related/',
            title: 'Кальян-бары похожие на Мята Lounge, Раушская наб., 4/5с1, Москва — Яндекс Карты',
            description: 'Подборка мест в Москве, похожих на кальян-бар «Мята Lounge», Раушская набережная, 4/5с1.'
        },
        {
            name: 'Лендинг каталога скидок',
            url: '/org/vkusvill/201444302948/edadeal/',
            title: 'ВкусВилл — скидки и акции магазина продуктов в Москве, улица Шаболовка, 61/21к1 — Яндекс Карты',
            description:
                'Каталог акций и скидок магазина продуктов «ВкусВилл», Россия, Москва, улица Шаболовка, 61/21к1.'
        },
        {
            name: 'Карточка со старым id',
            url: '/org/dapino/1118970372/',
            redirectUrl: '/org/dapino/1094008369/'
        },
        {
            name: 'Карточка с неправильным sublanding',
            url: '/org/vysota_5642/1388278711/prices/',
            redirectUrl: '/org/vysota_5642/1388278711/menu'
        },
        {
            name: 'Карточка остановки',
            url: '/org/metro_kitay_gorod/109977770187/',
            redirectUrl: '/213/moscow/stops/stop__10187978'
        },
        {
            name: 'Описание с кавычками',
            url: '/org/gbou_shkola_1533_lit_/1099513636/',
            description:
                'ГБОУ школа № 1533 "ЛИТ" ⭐ , Ⓜ Университет, Ломоносовский просп., 16, Москва: ✔ фотографии, адрес и ☎️ телефон, часы работы, фото и отзывы посетителей на Яндекс Картах.'
        },
        {
            name: 'Саблендинг /reviews организации с выключенными отзывами в конфиге',
            url: '/org/atakent_karde_ler_pide_kebap_salonu',
            check404: true,
            tld: 'tr'
        },
        {
            name: 'Саблендинг /prices организации без цен',
            url: '/org/atakent_karde_ler_pide_kebap_salonu',
            check404: true,
            tld: 'ru'
        },
        {
            name: 'Саблендинг /inside организации без организаций внутри',
            url: '/org/atakent_karde_ler_pide_kebap_salonu',
            check404: true,
            tld: 'tr'
        }
    ]
};

export default contents;
