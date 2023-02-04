/* tslint:disable:ter-max-len */

import {Table, Branch, Locale, InfoPageType, PageBlockType} from 'app/types/consts';
import {Schema} from 'app/types/db/pages-content';

const draftedRows: Schema[] = [
    {
        page_id: 1,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'restaurants'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'gde-est-pelmeni-v-ekaterinburge',
            image: {
                caption: '«Где есть пельмени» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1357607/2a00000167e122094146ae04ce4b5fa803ce/%s'
            },
            title: 'Где есть пельмени',
            rubric: {
                value: 'Кафе',
                useRemoteValue: false
            },
            properties: {
                meta: {
                    url: 'gde-est-pelmeni-v-ekaterinburge',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a00000167e121de5db9f90e683f1484c847/%s'
                    },
                    title: 'Где есть пельмени в Екатеринбурге — кафе, рестораны',
                    keywords: 'пельмени, лучшие пельмени, вареники, рестораны с пельменями, пельменные',
                    description:
                        'Феликс Сандалов сделал подборку кафе и ресторанов Екатеринбурга с главным гастрономическим брендом Урала.',
                    directAccess: false,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                },
                settings: {
                    displayMode: 'regular'
                }
            },
            provinceId: 11162,
            boundingBox: {
                northEast: {
                    lat: 56.858652999992536,
                    lon: 60.66215700000001
                },
                southWest: {
                    lat: 56.82362099999261,
                    lon: 60.556358999999986
                }
            },
            description:
                'Феликс Сандалов сделал подборку кафе и ресторанов Екатеринбурга с главным гастрономическим брендом Урала.',
            geoRegionId: 213,
            placeNumber: 10,
            schemaVersion: 0,
            needVerification: true
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                type: PageBlockType.ORGANIZATION,
                oid: '1197453631',
                tag: {
                    value: 'waterpark',
                    useRemoteValue: true
                },
                hours: {
                    text: 'пн-пт 10:00–22:00; сб,вс 9:00–22:00',
                    State: {
                        text: 'Открыто до 22:00',
                        isOpenNow: true,
                        shortText: 'До 22:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Friday: true,
                            Monday: true,
                            Tuesday: true,
                            Thursday: true,
                            Intervals: [
                                {
                                    to: '22:00:00',
                                    from: '10:00:00'
                                }
                            ],
                            Wednesday: true
                        },
                        {
                            Sunday: true,
                            Saturday: true,
                            Intervals: [
                                {
                                    to: '22:00:00',
                                    from: '09:00:00'
                                }
                            ]
                        }
                    ]
                },
                links: [],
                style: 'poor',
                title: {
                    value: 'Мореон',
                    useRemoteValue: true
                },
                images: [
                    {
                        width: 2000,
                        height: 1335,
                        caption: '«Мореон» фото 1',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/239474/2a0000015c2f5b80f74be2a0182cfd17234d/%s'
                    },
                    {
                        width: 2000,
                        height: 1333,
                        caption: '«Мореон» фото 2',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/223006/2a0000015b16db0f3535c4b86b3ade8f77fe/%s'
                    }
                ],
                rating: {
                    score: 9.8,
                    ratings: 11655,
                    reviews: 4778
                },
                rubric: {
                    value: 'Аквапарки',
                    useRemoteValue: true
                },
                address: {
                    value: 'Голубинская ул., 16',
                    useRemoteValue: true
                },
                seoname: 'moreon',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (495) 374-53-35',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 22:00',
                        useRemoteValue: true
                    }
                ],
                sentence:
                    '«Мореон» — огромный многофункциональный центр на юго-западе Столицы. Это не просто аквапарк, это целый город, где есть термы, SPA, бани, сауны, фитнес-центр, кафе и рестораны. Горки здесь подразделяются по уровням экстремальности от единицы до семёрки. Бассейны в «Мореоне» просторные. Для любителей релакса есть бассейн с водопадами и панорамными окнами.',
                coordinate: {
                    lat: 55.598274,
                    lon: 37.52737
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'hydro'
                },
                placemarkIcon: {
                    tag: 'hydro'
                },
                businessRating: {
                    score: 4.9,
                    ratings: 11655,
                    reviews: 4778
                },
                bookingLinksInfo: [
                    {
                        aref: '#instagram',
                        href: 'https://www.instagram.com/moreon_ru/',
                        type: 'social'
                    },
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/moreonru',
                        type: 'social'
                    }
                ],
                needVerification: true,
                experimentalMetaData: []
            },
            {
                type: PageBlockType.SHARE,
                style: 'large'
            },
            {
                type: PageBlockType.LINKS,
                pages: [
                    {
                        id: 3,
                        useAutoGenerate: true
                    },
                    {
                        id: 4,
                        useAutoGenerate: true
                    }
                ],
                title: 'Смотрите также'
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьмите Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 1
    },
    {
        page_id: 2,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'restaurants'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'kovurma-lagman',
            image: {
                caption: 'Где попробовать ковурму-лагман» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1532686/2a0000016e17fb10329011a3a43c8a722714/%s'
            },
            title: 'Где попробовать ковурму-лагман',
            rubric: {
                value: 'Кафе',
                useRemoteValue: false
            },
            properties: {
                meta: {
                    url: 'kovurma-lagman',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1532686/2a0000016e17fb032af043b7b40e3e05d1bd/%s'
                    },
                    title: 'Где попробовать ковурму-лагман',
                    description:
                        'Лагман — это не обязательно суп. В узбекской кухне есть ещё ковурма-лагман — жаркое с лапшой. Вот несколько мест, где хорошо готовят это блюдо.',
                    directAccess: true,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                },
                settings: {
                    displayMode: 'regular'
                }
            },
            provinceId: 1,
            boundingBox: {
                northEast: {
                    lat: 55.835775999993636,
                    lon: 37.670547
                },
                southWest: {
                    lat: 55.60022699999393,
                    lon: 37.455085
                }
            },
            description:
                'Лагман — это не обязательно суп. В узбекской кухне есть ещё ковурма-лагман — жаркое с лапшой. Вот несколько мест, где хорошо готовят это блюдо.',
            geoRegionId: 213,
            placeNumber: 8,
            tagLinksIds: [5],
            schemaVersion: 0,
            previewImageV3: {
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016e17fb25397948fbb15253b677fb/%s'
            },
            needVerification: false
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                oid: 'not-found-organization',
                tag: {
                    value: 'restaurants',
                    useRemoteValue: true
                },
                type: PageBlockType.ORGANIZATION,
                hours: {
                    text: 'пн-чт 12:00–1:00; пт,сб 12:00–5:00; вс 12:00–1:00',
                    State: {
                        text: 'Открыто до 1:00',
                        isOpenNow: true,
                        shortText: 'До 1:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Monday: true,
                            Tuesday: true,
                            Thursday: true,
                            Intervals: [
                                {
                                    to: '01:00:00',
                                    from: '12:00:00'
                                }
                            ],
                            Wednesday: true
                        },
                        {
                            Friday: true,
                            Saturday: true,
                            Intervals: [
                                {
                                    to: '05:00:00',
                                    from: '12:00:00'
                                }
                            ]
                        },
                        {
                            Sunday: true,
                            Intervals: [
                                {
                                    to: '01:00:00',
                                    from: '12:00:00'
                                }
                            ]
                        }
                    ]
                },
                links: [],
                style: 'rich',
                title: {
                    value: 'Чайхона № 1',
                    useRemoteValue: true
                },
                images: [
                    {
                        width: 1500,
                        height: 1000,
                        caption: '«Чайхона № 1» фото 1',
                        origUrl:
                            'https://avatars.mds.yandex.net//get-discovery-int/1674621/2a0000016e17c0d8b0f6dc8b51594b6bdc1e/orig',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016e17c0d8b0f6dc8b51594b6bdc1e/%s'
                    },
                    {
                        width: 1000,
                        height: 667,
                        caption: '«Чайхона № 1» фото 2',
                        origUrl:
                            'https://avatars.mds.yandex.net//get-discovery-int/1674621/2a0000016e17c0fbed4b97411de34a8f7492/orig',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016e17c0fbed4b97411de34a8f7492/%s'
                    }
                ],
                rating: {
                    score: 8.8,
                    ratings: 611,
                    reviews: 219
                },
                rubric: {
                    value: 'Рестораны',
                    useRemoteValue: true
                },
                address: {
                    value: 'Лодочная ул., 4, Москва',
                    useRemoteValue: true
                },
                seoname: 'chaykhona_1',
                features: [
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 1:00',
                        useRemoteValue: true
                    },
                    {
                        key: 'custom',
                        name: 'Цена',
                        value: '480 ₽'
                    }
                ],
                sentence: '',
                coordinate: {
                    lat: 55.835776,
                    lon: 37.455085
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'restaurants'
                },
                placemarkIcon: {
                    tag: 'restaurants'
                },
                businessRating: {
                    score: 4.4,
                    ratings: 611,
                    reviews: 219
                },
                bookingLinksInfo: [
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/chaihona1ru',
                        type: 'social'
                    },
                    {
                        aref: '#facebook',
                        href: 'https://www.facebook.com/chaihona1.ru',
                        type: 'social'
                    }
                ],
                needVerification: false,
                experimentalMetaData: []
            },
            {
                type: PageBlockType.SHARE,
                style: 'large'
            },
            {
                type: PageBlockType.LINKS,
                title: 'Смотрите также',
                pages: [
                    {
                        id: 1,
                        useAutoGenerate: true
                    },
                    {
                        id: 3,
                        useAutoGenerate: true
                    }
                ]
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьмите Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 2
    },
    {
        page_id: 3,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'architecture'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'gid-po-golicyn-loftu',
            image: {
                caption: '«Гид по Голицын-лофту» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a000001668de63f6ad4cc59d4c231919f1b/%s'
            },
            title: 'Гид по Голицын-лофту',
            rubric: {
                value: 'Кафе',
                useRemoteValue: false
            },
            properties: {
                meta: {
                    url: 'Gid-po-Golicyn-loftu',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/912415/2a000001668de6184d8f68b5e11fc8a784f1/%s'
                    },
                    title: 'Санкт-Петербург — Гид по Голицын-лофту — кафе, бары, рестораны',
                    keywords: 'Кафе, рестораны, бары, пабы, новые кафе, новые рестораны, новые бары, новые пабы',
                    description:
                        'Редакция «The Village» выбрала для вас бары, кафе и рестораны креативного кластера «Голицын Лофт».',
                    directAccess: false,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                }
            },
            provinceId: 10174,
            boundingBox: {
                northEast: {
                    lat: 59.94082499999101,
                    lon: 30.34223700000001
                },
                southWest: {
                    lat: 59.94057399999102,
                    lon: 30.341178999999983
                }
            },
            description:
                'За два года работы «Голицын-Лофт» — крупнейший в Петербурге креативный кластер, расположенный в особняке XVIII века на набережной Фонтанки — разросся до невероятных размеров: здесь уже под сотню самых разных арендаторов, от кафе, ресторанов и баров до шоу-румов и магазинов. Прогулка по самому особняку с его руинированными историческими интерьерами, лепниной, старинной кирпичной кладкой и мраморными каминами превратилась в настоящее приключение — никогда не знаешь, что скрывается за следующей дверью с очередным удивительным названием. Спустя два года, после выходы первого гида по «Голицын-Лофту» The Village вернулся в особняк и составил новый путеводитель по всем работающим в нем кафе, барам и ресторанам.',
            geoRegionId: 213,
            placeNumber: 21,
            schemaVersion: 0,
            needVerification: true
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                type: PageBlockType.ORGANIZATION,
                oid: '1393234578',
                tag: {
                    value: 'cultural center',
                    useRemoteValue: true
                },
                hours: {
                    text: 'ежедневно, 11:00–0:00',
                    State: {
                        text: 'Открыто до 0:00',
                        isOpenNow: true,
                        shortText: 'До 0:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Everyday: true,
                            Intervals: [
                                {
                                    to: '00:00:00',
                                    from: '11:00:00'
                                }
                            ]
                        }
                    ]
                },
                style: 'poor',
                title: {
                    value: 'Цифербург',
                    useRemoteValue: true
                },
                images: [
                    {
                        width: 2560,
                        height: 1600,
                        caption: '«Цифербург» фото 1',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/906486/2a00000164515a1d7ea96b95c86624a97909/%s'
                    },
                    {
                        width: 2560,
                        height: 1600,
                        caption: '«Цифербург» фото 2',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/906486/2a00000164515dc4adff2e73d120eb14ef12/%s'
                    }
                ],
                rating: {
                    score: 8.8,
                    ratings: 260,
                    reviews: 78
                },
                rubric: {
                    value: 'Культурные центры',
                    useRemoteValue: true
                },
                address: {
                    value: 'наб. реки Фонтанки, 20, Санкт-Петербург',
                    useRemoteValue: true
                },
                seoname: 'tsiferburg',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (981) 180-70-22',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 0:00',
                        useRemoteValue: true
                    }
                ],
                sentence:
                    'Открывшийся одним из первых в кластере коворкинг «Цифербург» до сих пор остается одним из самых просторных и оживленных мест. Здесь по прежнему действует принцип почасовой оплаты.  За эти деньги можно в неограниченном количестве пить кофе и чай, пользоваться кухней (приготовить или разогреть себе обед), но главное — работать или отдыхать в залах с исторической лепниной, камином и лучшим видом на Фонтанку и Михайловский замок.',
                coordinate: {
                    lat: 59.940623,
                    lon: 30.341495
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'architecture'
                },
                placemarkIcon: {
                    tag: 'architecture'
                },
                businessRating: {
                    score: 4.4,
                    ratings: 260,
                    reviews: 78
                },
                bookingLinksInfo: [
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/ziferburg',
                        type: 'social'
                    },
                    {
                        aref: '#facebook',
                        href: 'https://www.facebook.com/ziferburg',
                        type: 'social'
                    }
                ],
                needVerification: true,
                experimentalMetaData: []
            },
            {
                type: PageBlockType.SHARE,
                style: 'large'
            },
            {
                type: PageBlockType.LINKS,
                title: 'Смотрите также',
                pages: [
                    {
                        id: 1,
                        useAutoGenerate: true
                    },
                    {
                        id: 2,
                        useAutoGenerate: true
                    }
                ]
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьмите Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 1,
        archived_blocks: [
            {
                oid: '1393234578',
                tag: {
                    value: 'cultural center',
                    useRemoteValue: true
                },
                type: PageBlockType.ORGANIZATION,
                hours: {
                    text: 'ежедневно, 11:00–0:00',
                    State: {
                        text: 'Открыто до 0:00',
                        isOpenNow: true,
                        shortText: 'До 0:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Everyday: true,
                            Intervals: [
                                {
                                    to: '00:00:00',
                                    from: '11:00:00'
                                }
                            ]
                        }
                    ]
                },
                style: 'poor',
                title: {
                    value: 'Цифербург',
                    useRemoteValue: true
                },
                images: [
                    {
                        width: 2560,
                        height: 1600,
                        caption: '«Цифербург» фото 1',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/906486/2a00000164515a1d7ea96b95c86624a97909/%s'
                    },
                    {
                        width: 2560,
                        height: 1600,
                        caption: '«Цифербург» фото 2',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/906486/2a00000164515dc4adff2e73d120eb14ef12/%s'
                    }
                ],
                rating: {
                    score: 8.8,
                    ratings: 260,
                    reviews: 78
                },
                rubric: {
                    value: 'Культурные центры',
                    useRemoteValue: true
                },
                address: {
                    value: 'наб. реки Фонтанки, 20, Санкт-Петербург',
                    useRemoteValue: true
                },
                seoname: 'tsiferburg',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (981) 180-70-22',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 0:00',
                        useRemoteValue: true
                    }
                ],
                sentence:
                    'Открывшийся одним из первых в кластере коворкинг «Цифербург» до сих пор остается одним из самых просторных и оживленных мест. Здесь по прежнему действует принцип почасовой оплаты.  За эти деньги можно в неограниченном количестве пить кофе и чай, пользоваться кухней (приготовить или разогреть себе обед), но главное — работать или отдыхать в залах с исторической лепниной, камином и лучшим видом на Фонтанку и Михайловский замок.',
                coordinate: {
                    lat: 59.940623,
                    lon: 30.341495
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'architecture'
                },
                placemarkIcon: {
                    tag: 'architecture'
                },
                businessRating: {
                    score: 4.4,
                    ratings: 260,
                    reviews: 78
                },
                bookingLinksInfo: [
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/ziferburg',
                        type: 'social'
                    },
                    {
                        aref: '#facebook',
                        href: 'https://www.facebook.com/ziferburg',
                        type: 'social'
                    }
                ],
                needVerification: false,
                experimentalMetaData: []
            }
        ]
    },
    {
        page_id: 4,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'cafe'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'kofe-v-turke',
            image: {
                caption: '«Где пить кофе на песке» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/912415/2a000001651a25cc629c8a9658550c45cc09/%s'
            },
            title: 'Где пить кофе на песке',
            rubric: {
                value: 'Кофейни',
                useRemoteValue: false
            },
            properties: {
                meta: {
                    url: 'kofe-v-turke',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001651a25ba93207b9ea50e63da93f0/%s'
                    },
                    title: 'Где пить кофе на песке в Москве — кафе, кофейни, бары',
                    keywords: 'кафе, кофейни',
                    description: 'Редакция The Village рассказывает о местах, где подают кофе по-восточному.',
                    directAccess: false,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                },
                settings: {
                    displayMode: 'regular'
                }
            },
            provinceId: 1,
            boundingBox: {
                northEast: {
                    lat: 55.82408099999366,
                    lon: 37.65008999999998
                },
                southWest: {
                    lat: 55.6978319999938,
                    lon: 37.495637999999985
                }
            },
            description:
                'Кемекс, V60 или аэропресс уже давно никого не удивляют: альтернативные методы заварки кофе прочно укрепились на своих местах и только продолжают свою экспансию. А вот кофе в турке или на песке в Москве встречается все еще нечасто. Редакция The Village рассказывает, где искать кофе по-восточному.',
            geoRegionId: 2,
            placeNumber: 6,
            schemaVersion: 0,
            needVerification: false
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                oid: '18354308579',
                tag: {
                    value: 'cafe',
                    useRemoteValue: true
                },
                type: PageBlockType.ORGANIZATION,
                hours: {
                    text: 'пн-пт 8:00–22:00; сб,вс 10:00–23:00',
                    State: {
                        text: 'Открыто до 22:00',
                        isOpenNow: true,
                        shortText: 'До 22:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Friday: true,
                            Monday: true,
                            Tuesday: true,
                            Thursday: true,
                            Intervals: [
                                {
                                    to: '22:00:00',
                                    from: '08:00:00'
                                }
                            ],
                            Wednesday: true
                        },
                        {
                            Sunday: true,
                            Saturday: true,
                            Intervals: [
                                {
                                    to: '23:00:00',
                                    from: '10:00:00'
                                }
                            ]
                        }
                    ]
                },
                links: [],
                style: 'poor',
                title: {
                    value: 'Cezve Coffee',
                    useRemoteValue: false
                },
                images: [
                    {
                        width: 5184,
                        height: 3456,
                        caption: '«Cezve Coffee» фото 1',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/1344805/2a00000165a4801e1fdeb5eb5d25f14236bf/%s'
                    },
                    {
                        width: 4500,
                        height: 3000,
                        caption: '«Cezve Coffee» фото 2',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/1622057/2a00000167310dbf59a45c333f35b25d0b3b/%s'
                    }
                ],
                rating: {
                    score: 10,
                    ratings: 230,
                    reviews: 108
                },
                rubric: {
                    value: 'Кофейни',
                    useRemoteValue: true
                },
                address: {
                    value: 'Подсосенский пер., 8, стр. 2',
                    useRemoteValue: true
                },
                seoname: 'cezve_coffee',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (495) 134-57-73',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 22:00',
                        useRemoteValue: true
                    },
                    {
                        key: 'average_bill2',
                        name: 'Средний счёт',
                        value: 'от 700 ₽',
                        useRemoteValue: true
                    }
                ],
                sentence:
                    'Одно из главных турецких кафе в городе открыли два года назад Николай и Марина Хюппенен. Оба — ветераны кофейной индустрии, Николай — судья чемпионатов бариста, Марина — чемпион России по приготовлению кофе в джезве, то есть в турке. Cezve Coffee специализируется как раз на нем, хотя классику в кафе тоже варят, а также готовят отличную турецкую еду. Кофе подают как надо: на красивых металлических подносах, в турке, с резным стаканом и такой же сахарницей. В меню: кофе по-восточному, чемпионская cezve, турка «пряная черешня» и еще несколько позиций кофе на песке, а также напитки на основе эспрессо и альтернатива.',
                coordinate: {
                    lat: 55.758249,
                    lon: 37.65009
                },
                customTitle: '',
                description: 'SOME DESCRIPTION',
                paragraphIcon: {
                    tag: 'cafe'
                },
                placemarkIcon: {
                    tag: 'cafe'
                },
                businessRating: {
                    score: 5,
                    ratings: 230,
                    reviews: 108
                },
                bookingLinksInfo: [
                    {
                        aref: '#facebook',
                        href: 'https://www.facebook.com/CezveCoffeeKurskaya/',
                        type: 'social'
                    },
                    {
                        aref: '#instagram',
                        href: 'https://www.instagram.com/cezve_coffee',
                        type: 'social'
                    }
                ],
                needVerification: false,
                experimentalMetaData: []
            },
            {
                oid: '1106890691',
                tag: {
                    value: 'cafe',
                    useRemoteValue: true
                },
                type: PageBlockType.ORGANIZATION,
                hours: {
                    text: 'ежедневно, 12:00–23:00',
                    State: {
                        text: 'Открыто до 23:00',
                        isOpenNow: true,
                        shortText: 'До 23:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Everyday: true,
                            Intervals: [
                                {
                                    to: '23:00:00',
                                    from: '12:00:00'
                                }
                            ]
                        }
                    ]
                },
                links: [],
                style: 'poor',
                title: {
                    value: 'Бардак',
                    useRemoteValue: false
                },
                images: [
                    {
                        width: 1392,
                        height: 940,
                        caption: '«Бардак» фото 1',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-altay/247136/2a0000015b17ba0d708165ba2371dfa331c3/%s'
                    }
                ],
                rating: {
                    score: 9.6,
                    ratings: 608,
                    reviews: 279
                },
                rubric: {
                    value: 'Кафе',
                    useRemoteValue: true
                },
                address: {
                    value: 'ул. Маросейка, 6-8с1',
                    useRemoteValue: true
                },
                seoname: 'bardak',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (495) 624-88-78',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 23:00',
                        useRemoteValue: true
                    },
                    {
                        key: 'average_bill2',
                        name: 'Средний счёт',
                        value: '1000–1500 ₽',
                        useRemoteValue: true
                    }
                ],
                sentence:
                    'Большое и хорошее кафе в районе «Китай-города» — одно из самых важных на карте турецких заведений Москвы: меню здесь большое, а за соседними столиками часто можно встретить турок. Кроме всевозможных кебабов и кефте, тут можно найти симит — турецкий бублик в кунжуте и, конечно, кофе по-турецки. Варят на песке, приносят на красивом подносе в джезве.',
                coordinate: {
                    lat: 55.757257,
                    lon: 37.634409
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'restaurants'
                },
                placemarkIcon: {
                    tag: 'restaurants'
                },
                businessRating: {
                    score: 4.8,
                    ratings: 608,
                    reviews: 279
                },
                bookingLinksInfo: [
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/bardak.cafe',
                        type: 'social'
                    },
                    {
                        aref: '#facebook',
                        href: 'https://www.facebook.com/bardak.cafe/',
                        type: 'social'
                    },
                    {
                        aref: '#instagram',
                        href: 'https://www.instagram.com/bardakcafe',
                        type: 'social'
                    }
                ],
                needVerification: false,
                experimentalMetaData: []
            },
            {
                type: PageBlockType.SHARE,
                style: 'large'
            },
            {
                type: PageBlockType.LINKS,
                title: 'Смотрите также',
                pages: [
                    {
                        id: 3,
                        useAutoGenerate: true
                    }
                ]
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьми Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 2
    },
    {
        page_id: 5,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'museum'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'pereslavl-zalesskiy-voronino',
            image: {
                caption: '«Частные музеи в Переславле-Залесском» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016d8cb66710f0dff17d1c948afd12/%s'
            },
            title: 'Частные музеи в Переславле-Залесском',
            rubric: {
                value: 'Музеи',
                useRemoteValue: true
            },
            properties: {
                meta: {
                    url: 'pereslavl-zalesskiy-voronino',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001651a25ba93207b9ea50e63da93f0/%s'
                    },
                    title: 'Лучшие музеи в Переславле-Залесском',
                    keywords:
                        'достопримечательности Переславля, музеи Переславля, парки, музеи, коллекции раритетов, музеи старины',
                    description: 'Знакомство с крестьянским дизайном, коллекции утюгов, чайников.',
                    directAccess: false,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                },
                settings: {
                    displayMode: 'regular'
                }
            },
            provinceId: 1,
            boundingBox: {
                northEast: {
                    lat: 55.82408099999366,
                    lon: 37.65008999999998
                },
                southWest: {
                    lat: 55.6978319999938,
                    lon: 37.495637999999985
                }
            },
            description: 'Знакомство с крестьянским дизайном, коллекции утюгов, чайников.',
            geoRegionId: 213,
            placeNumber: 6,
            schemaVersion: 0,
            needVerification: false
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                oid: '1123659643',
                tag: {
                    value: 'museum',
                    useRemoteValue: true
                },
                type: PageBlockType.ORGANIZATION,
                hours: {
                    text: 'вт-пт 10:00–16:00; сб,вс 10:00–17:00',
                    State: {
                        text: 'Открыто до 16:00',
                        isOpenNow: true,
                        shortText: 'До 16:00'
                    },
                    tzOffset: 10800,
                    Availabilities: [
                        {
                            Friday: true,
                            Tuesday: true,
                            Thursday: true,
                            Intervals: [
                                {
                                    to: '16:00:00',
                                    from: '10:00:00'
                                }
                            ],
                            Wednesday: true
                        },
                        {
                            Sunday: true,
                            Saturday: true,
                            Intervals: [
                                {
                                    to: '17:00:00',
                                    from: '10:00:00'
                                }
                            ]
                        }
                    ]
                },
                links: [],
                style: 'poor',
                title: {
                    value: 'Парк-музей «Рождение сказки»',
                    useRemoteValue: false
                },
                images: [
                    {
                        width: 1280,
                        height: 853,
                        caption: '«Парк-музей «Рождение сказки»» фото 9',
                        origUrl:
                            'https://avatars.mds.yandex.net//get-discovery-int/1674621/2a0000016d7fbfdc7a807b6f2bb819f07015/orig',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016d7fbfdc7a807b6f2bb819f07015/%s'
                    },
                    {
                        width: 1200,
                        height: 800,
                        caption: '«Парк-музей «Рождение сказки»» фото 10',
                        origUrl:
                            'https://avatars.mds.yandex.net//get-discovery-int/1674621/2a0000016d7fc149862eab6df5665f5df5e4/orig',
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016d7fc149862eab6df5665f5df5e4/%s'
                    }
                ],
                rating: {
                    score: 9.8,
                    ratings: 597,
                    reviews: 302
                },
                rubric: {
                    value: 'Музеи',
                    useRemoteValue: true
                },
                address: {
                    value: 'Россия, Ярославская область, городской округ Переславль-Залесский, деревня Василево, улица Московская, 1',
                    useRemoteValue: true
                },
                seoname: 'muzey_rozhdeniye_skazki',
                features: [
                    {
                        key: 'phone',
                        name: 'Телефон',
                        value: '+7 (906) 526-77-77',
                        useRemoteValue: true
                    },
                    {
                        key: 'working_time',
                        name: 'Время работы',
                        value: 'Открыто до 16:00',
                        useRemoteValue: true
                    },
                    {
                        key: 'tickets',
                        name: 'Билеты',
                        value: '150–250 ₽'
                    }
                ],
                sentence: 'Парк-музей находится в деревне Василево',
                coordinate: {
                    lat: 56.560782,
                    lon: 38.580913
                },
                customTitle: '',
                description: '',
                paragraphIcon: {
                    tag: 'museum'
                },
                placemarkIcon: {
                    tag: 'museum'
                },
                businessRating: {
                    score: 4.9,
                    ratings: 597,
                    reviews: 302
                },
                bookingLinksInfo: [
                    {
                        aref: '#vkontakte',
                        href: 'https://vk.com/club21338980',
                        type: 'social'
                    },
                    {
                        href: 'http://www.muzeiskazki.ru/',
                        type: 'self'
                    }
                ],
                needVerification: false,
                experimentalMetaData: []
            },
            {
                type: PageBlockType.LINKS,
                pages: [
                    {
                        id: 1,
                        useAutoGenerate: true
                    },
                    {
                        id: 3,
                        useAutoGenerate: true
                    }
                ],
                title: 'Смотрите также'
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьмите Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 1
    },
    {
        page_id: 6,
        branch: Branch.DRAFT,
        info: {
            icon: {
                tag: 'some-tag'
            },
            type: InfoPageType.ORGANIZATION_LIST,
            alias: 'pereslavl-zalesskiy-voronino-empty',
            image: {
                caption: '«Частные музеи в Переславле-Залесском» фото материала',
                urlTemplate:
                    'https://avatars.mds.yandex.net/get-discovery-int/1674621/2a0000016d8cb66710f0dff17d1c948afd12/%s'
            },
            title: 'Частные музеи в Переславле-Залесском',
            rubric: {
                value: 'Музеи',
                useRemoteValue: true
            },
            properties: {
                meta: {
                    url: 'pereslavl-zalesskiy-voronino',
                    image: {
                        urlTemplate:
                            'https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001651a25ba93207b9ea50e63da93f0/%s'
                    },
                    title: 'Лучшие музеи в Переславле-Залесском',
                    keywords:
                        'достопримечательности Переславля, музеи Переславля, парки, музеи, коллекции раритетов, музеи старины',
                    description: 'Знакомство с крестьянским дизайном, коллекции утюгов, чайников.',
                    directAccess: false,
                    noAutoRubrics: false,
                    randomOrderOfOrganizations: false
                },
                settings: {
                    displayMode: 'regular'
                }
            },
            provinceId: 1,
            boundingBox: {
                northEast: {
                    lat: 55.82408099999366,
                    lon: 37.65008999999998
                },
                southWest: {
                    lat: 55.6978319999938,
                    lon: 37.495637999999985
                }
            },
            description: 'Знакомство с крестьянским дизайном, коллекции утюгов, чайников.',
            geoRegionId: 213,
            placeNumber: 6,
            schemaVersion: 0,
            needVerification: false
        },
        blocks: [
            {
                type: PageBlockType.SHARE,
                style: 'small'
            },
            {
                type: PageBlockType.LINKS,
                pages: [
                    {
                        id: 1,
                        useAutoGenerate: true
                    },
                    {
                        id: 3,
                        useAutoGenerate: true
                    }
                ],
                title: 'Смотрите также'
            },
            {
                type: PageBlockType.PROMO,
                image: {
                    urlTemplate:
                        'https://avatars.mds.yandex.net/get-discovery-int/1327602/2a00000164b184fd68e1de40a97592200382/%s'
                },
                title: 'Возьмите Яндекс Карты с собой',
                description: 'Кафе, развлечения и другие интересные места уже в мобильных Яндекс Картах.'
            }
        ],
        locale: Locale.RU,
        partner_id: 2
    }
];

// Create published pages by draft
const rows: Schema[] = [...draftedRows];
draftedRows.forEach((row) =>
    rows.push({
        ...row,
        branch: Branch.PUBLIC
    })
);

const pagesContent = {
    table: Table.PAGES_CONTENT,
    rows: rows.map((row) => ({
        ...row,
        info: JSON.stringify(row.info),
        blocks: JSON.stringify(row.blocks),
        ...(row.archived_blocks
            ? {
                  archived_blocks: JSON.stringify(row.archived_blocks)
              }
            : {})
    }))
};

export {draftedRows, rows, pagesContent};
