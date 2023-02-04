const geoLocation = {
    rgid: 587795,
    name: 'Москва'
};

/* eslint-disable max-len */
module.exports = [
    {
        name: 'offer',
        data: {
            breadcrumbsParams: {
                type: 'SELL',
                category: 'APARTMENT',
                rentTime: 'LARGE',
                newFlat: 'NO',
                roomsTotal: 1,
                siteInfo: {},
                offerInfo: {
                    type: 'SELL',
                    category: 'APARTMENT',
                    area: {
                        value: 55,
                        unit: 'SQUARE_METER'
                    },
                    floorsOffered: 1,
                    floorsTotal: 4
                },
                controller: 'search',
                searchType: 'offer'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира', 'Однокомнатные', 'Купить квартиру, 55 м², 1/4 этаж' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kvartira/', '/moskva/kupit/kvartira/odnokomnatnaya/', '' ]
    },
    {
        name: 'newbuilding',
        data: {
            breadcrumbsParams: {
                type: 'SELL',
                controller: 'newbuilding-search',
                searchType: 'offer',
                name: 'Бутово парк 2',
                fullName: 'микрорайон «Бутово парк 2»'
            },
            geoLocation
        },
        controller: 'search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Квартира в новостройке', 'Микрорайон «Бутово парк 2»' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/novostrojka/', '' ]
    },
    {
        name: 'village',
        data: {
            breadcrumbsParams: {
                type: 'SELL',
                controller: 'village-search',
                searchType: 'offer',
                name: 'Серебряная лагуна',
                fullName: 'Коттеджный посёлок «Серебряная лагуна»'
            },
            geoLocation
        },
        controller: 'village-search',
        titles: [ 'Я.Недвижимость', 'Москва', 'Купить', 'Коттеджные поселки', 'Коттеджный посёлок «Серебряная лагуна»' ],
        links: [ '/', '/moskva/', '/moskva/kupit/', '/moskva/kupit/kottedzhnye-poselki/', '' ]
    }
];
