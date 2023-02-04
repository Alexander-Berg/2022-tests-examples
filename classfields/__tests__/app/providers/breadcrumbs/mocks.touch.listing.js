const geoLocation = {
    rgid: 587795,
    name: 'Москва'
};

/* eslint-disable max-len */
module.exports = [
    {
        name: 'category',
        data: {
            searchParams: {
                type: 'RENT',
                category: 'ROOMS'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Снять', 'Комната' ],
        links: [ '/', '/moskva/', '/moskva/snyat/', '/moskva/snyat/komnata/' ]
    },
    {
        name: 'categoryCommercial',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'COMMERCIAL'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Коммерческая недвижимость' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kommercheskaya-nedvizhimost/' ]
    },
    {
        name: 'categoryCommercialType',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'FREE_PURPOSE'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Коммерческая недвижимость', 'Помещение свободного назначения' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kommercheskaya-nedvizhimost/', '/moskva/kupit/kommercheskaya-nedvizhimost/pomeshchenie-svobodnogo-naznacheniya/' ]
    },
    {
        name: 'categoryCommercialType1',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'COMMERCIAL',
                commercialType: 'RETAIL'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Коммерческая недвижимость', 'Торговое помещение' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kommercheskaya-nedvizhimost/', '/moskva/kupit/kommercheskaya-nedvizhimost/torgovoe-pomeshchenie/' ]
    },
    {
        name: 'rooms',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'APARTMENT',
                roomsTotal: [ '2', 'STUDIO' ]
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира', 'Студия, 2-комнатные' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kvartira/', '/moskva/kupit/kvartira/?roomsTotal=2&roomsTotal=STUDIO' ]
    },
    {
        name: 'one-filter-provided',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'APARTMENT',
                roomsTotal: '2',
                balcony: 'BALCONY'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира', 'Двухкомнатные', 'С балконом' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kvartira/', '/moskva/kupit/kvartira/dvuhkomnatnaya/', '/moskva/kupit/kvartira/dvuhkomnatnaya/s-balkonom/' ]
    },
    {
        name: 'two-filters-provided',
        data: {
            searchParams: {
                type: 'SELL',
                category: 'APARTMENT',
                roomsTotal: '2',
                balcony: 'BALCONY',
                buildingType: 'BRICK'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира', 'Двухкомнатные', 'В кирпичном доме, с балконом' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kvartira/', '/moskva/kupit/kvartira/dvuhkomnatnaya/', '/moskva/kupit/kvartira/dvuhkomnatnaya/kirpich-i-s-balkonom/' ]
    },
    {
        name: 'with street and without agent',
        data: {
            searchParams: {
                rgid: 582357,
                streetId: '34983',
                streetName: 'ulica-baumana',
                agents: 'NO',
                sort: 'RELEVANCE',
                type: 'RENT',
                category: 'APARTMENT'
            },
            geoLocation: {
                rgid: 582357,
                name: 'Казань'
            },
            refinements: {
                street: {
                    _name: 'street',
                    shortName: '1 улица',
                    name: 'адрес',
                    list: [
                        { name: 'Казань, улица Баумана', id: 34983 }
                    ]
                }
            }
        },
        controller: 'search',
        titles: [
            'Я.Недвижимость',
            'Казань',
            'Снять',
            'Квартира',
            'Казань, улица Баумана',
            'Без посредников'
        ],
        links: [ '/', '/kazan/',
            '/kazan/snyat/',
            '/kazan/snyat/kvartira/',
            '/kazan/snyat/kvartira/st-ulica-baumana-34983/',
            '/kazan/snyat/kvartira/st-ulica-baumana-34983/bez-posrednikov/' ]
    },
    {
        name: 'newbuilding',
        data: {
            searchParams: {
                type: 'SELL'
            },
            geoLocation
        },
        controller: 'sites-search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира в новостройке' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/novostrojka/' ]
    },
    {
        name: 'villages',
        data: {
            searchParams: {
                type: 'SELL'
            },
            geoLocation
        },
        controller: 'villages-search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Коттеджные поселки' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kottedzhnye-poselki/' ]
    },
    {
        name: 'search with street and house',
        data: {
            searchParams: {
                rgid: 582357,
                streetId: '34983',
                streetName: 'ulica-baumana',
                buildingIds: '8017462998175267261',
                houseNumber: '12',
                sort: 'RELEVANCE',
                type: 'RENT',
                category: 'APARTMENT'
            },
            geoLocation: {
                rgid: 582357,
                name: 'Казань'
            },
            refinements: {
                street: {
                    _name: 'street',
                    shortName: '1 улица',
                    name: 'адрес',
                    list: [
                        {
                            name: 'улица Баумана',
                            id: 34983,
                            buildingIds: '8017462998175267261',
                            houseNumber: '12'
                        }
                    ]
                }
            }
        },
        controller: 'search',
        titles: [
            'Я.Недвижимость',
            'Казань',
            'Снять',
            'Квартира',
            'Улица Баумана',
            'Дом 12'
        ],
        links: [ '/', '/kazan/',
            '/kazan/snyat/',
            '/kazan/snyat/kvartira/',
            '/kazan/snyat/kvartira/st-ulica-baumana-34983/',
            '/kazan/snyat/kvartira/st-ulica-baumana-34983/dom-12-8017462998175267261/' ]
    }
];
