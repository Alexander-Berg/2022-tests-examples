const getListingText = require('./getListingText');
const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const getState = (catalogFilter, relatedDealers, totalOffers, params, minPrice) => {
    return {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        router: {
            current: {
                params: {
                    catalog_filter: catalogFilter,
                    category: 'cars',
                    ...params,
                },
            },
        },
        listing: {
            data: {
                relatedDealers: {
                    pager: {
                        count: relatedDealers,
                    },
                },
                pagination: {
                    total_offers_count: totalOffers,
                },
                search_parameters: {
                    catalog_filter: catalogFilter,
                    category: 'cars',
                },
                price_range: {
                    min: {
                        price: minPrice,
                    },
                },
            },
        },
        listingSectionsCount: {
            'new': 126,
            used: 1,
        },
        geo: {
            gidsInfo: {
                linguistics: {
                    prepositional: 'Москве',
                },
            },
        },
        listingPriceRanges: {
            data: [ { price_to: 50000, offers_count: 17 }, { price_to: 100000, offers_count: 115 } ],
        },
    };
};

it('Должен сгенерировать текст, когда есть параметры марки и модели для десктопа', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'ECOSPORT',
        } ],
    12,
    127,
    );
    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford EcoSport в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
        'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст, когда есть параметры марки и модели для мобилки', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'ECOSPORT',
        } ],
    12,
    127,
    );
    const snippet = getListingText(state, true);
    const result = {
        firstColumn: 'Купить Ford EcoSport в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
        'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст, когда нет дилеров', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'ECOSPORT',
        } ],
    0,
    127,
    );
    const snippet = getListingText(state, true);
    const result = {
        firstColumn: 'Купить Ford EcoSport в Москве - выберите свой автомобиль из 127 автомобилей от дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст, когда нет офферов', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'ECOSPORT',
        } ],
    12,
    0,
    );
    const snippet = getListingText(state, true);
    const result = {
        firstColumn: '',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать пустую строку, если нет никаких параметров', () => {
    const snippet = getListingText({});

    expect(snippet).toEqual('');
});

it('Должен сгенерировать текст с маркой + моделью + шильдиком', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'FOCUS',
            nameplate_name: 'active',
        } ],
    12,
    127,
    );
    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford Focus Active в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с маркой и трансмиссией', () => {
    const state = getState([
        {
            mark: 'FORD',
        } ],
    12,
    127,
    {
        transmission: [ 'VARIATOR' ],
        price_to: 0,
    },
    900000,
    );

    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля и выбрать ' +
            'по реальным отзывам владельцев. Полный модельный ряд Ford с вариатором по цене от 900 000 руб. в Москве - ' +
            'более 127 свежих объявлений о продаже Форд от частников и дилеров на Авто.ру',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с маркой и автоматической трансмиссией', () => {
    const state = getState([
        {
            mark: 'FORD',
        } ],
    12,
    127,
    {
        transmission: [ 'ROBOT', 'AUTOMATIC', 'VARIATOR', 'AUTO' ],
        price_to: 0,
    },
    900000,
    );

    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля и выбрать ' +
            'по реальным отзывам владельцев. Полный модельный ряд Ford с АКПП по цене от 900 000 руб. в Москве - ' +
            'более 127 свежих объявлений о продаже Форд от частников и дилеров на Авто.ру',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с маркой без трансмиссии', () => {
    const state = getState([
        {
            mark: 'FORD',
        } ],
    12,
    127,
    {
        transmission: [ 'AUTOMATIC' ],
        price_to: 0,
    },
    900000,
    );

    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев. Полный модельный ряд Ford по цене от 900 000 руб. ' +
            'в Москве - более 127 свежих объявлений о продаже Форд от частников и дилеров на Авто.ру',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с объёмом двигателя', () => {
    const state = getState([
        {
            mark: 'FORD',
        } ],
    12,
    127,
    {
        displacement_from: 3000,
        displacement_to: 3000,
    },
    900000,
    );

    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford 3.0л в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев. Полный модельный ряд Ford 3.0л по цене от 900 000 руб. ' +
            'в Москве - более 127 свежих объявлений о продаже Форд от частников и дилеров на Авто.ру',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с моделью + шильд', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'C_MAX',
            nameplate_name: 'grand',
        } ],
    12,
    127,
    );
    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford Grand C-MAX в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст с ценой, когда цена есть в справочнике priceRanges', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'C_MAX',
            nameplate_name: 'grand',
        } ],
    12,
    127,
    {
        price_to: 100000,
    },
    );
    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford Grand C-MAX до 100 000 рублей в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});

it('Должен сгенерировать текст без цены, когда цены нет в справочнике priceRanges', () => {
    const state = getState([
        {
            mark: 'FORD',
            model: 'C_MAX',
            nameplate_name: 'grand',
        } ],
    12,
    127,
    {
        price_to: 100777,
    },
    );
    const snippet = getListingText(state);
    const result = {
        firstColumn: 'Купить Ford Grand C-MAX в Москве - выберите свой автомобиль из 127 автомобилей от 12 дилеров и частников.',
        secondColumn: 'На Авто.ру вы можете оформить кредит, проверить автомобиль по VIN, узнать полную историю автомобиля ' +
            'и выбрать по реальным отзывам владельцев.',
    };

    expect(snippet).toEqual(result);
});
