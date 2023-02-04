const _ = require('lodash');

const getListingOutputType = require('./getListingOutputType');

const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;
const listingViewCookie = require('auto-core/react/lib/listingViewCookie');

it('должен вернуть тип вывода листинга из параметров роута, если он там есть', () => {
    const params = {};
    const context = {
        req: {
            router: {
                params: {
                    output_type: LISTING_OUTPUT_TYPE.TABLE,
                },
            },
            cookies: {
                [ listingViewCookie.COOKIE_NAME ]: '{"output_type":"table","version":1}',
            },
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.TABLE);
});

it('должен вернуть тип вывода листинга из куки, если он там есть, а в параметрах роута нет', () => {
    const params = {};
    const context = {
        req: {
            params: {},
            cookies: {
                [ listingViewCookie.COOKIE_NAME ]: '{"output_type":"table","version":1}',
            },
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.TABLE);
});

it('должен вернуть тип вывода по-умолчанию для раздела новых авто, если ни в параметрах роутера, ни в куке данных нет', () => {
    const params = {
        category: 'cars',
        section: 'new',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.MODELS);
});

it('должен вернуть тип вывода по-умолчанию для других разделов, если ни в параметрах роутера, ни в куке данных нет', () => {
    const params = {
        category: 'cars',
        section: 'used',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.LIST);
});

it('должен вернуть тип вывода по-умолчанию для раздела новых мото и комтранс , если ни в параметрах роутера, ни в куке данных нет', () => {
    const paramsTrucks = {
        category: 'trucks',
        section: 'new',
    };
    const paramsMoto = {
        category: 'trucks',
        section: 'new',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params: paramsTrucks, context })).toBe(LISTING_OUTPUT_TYPE.LIST);
    expect(getListingOutputType({ params: paramsMoto, context })).toBe(LISTING_OUTPUT_TYPE.LIST);
});

it('должен вернуть тип вывода "по моделям" для листинга гуру', () => {
    const params = {
        category: 'cars',
        section: 'used',
        gurua0: 'blah',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: _.noop,
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.MODELS);
});

it('должен вернуть тип вывода "по объявлениям" для листинга гуру в экспе AUTORUFRONT-15766_autoguru_flat', () => {
    const params = {
        category: 'cars',
        section: 'used',
        gurua0: 'blah',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: (flag) => flag === 'AUTORUFRONT-15766_autoguru_flat',
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.LIST);
});

it('должен вернуть тип вывода "карусель" для листинга гуру в экспе AUTORUFRONT-19853_carousel', () => {
    const params = {
        category: 'cars',
        section: 'used',
    };
    const context = {
        req: {
            params: {},
            cookies: {},
            experimentsData: {
                has: (flag) => flag === 'AUTORUFRONT-19853_carousel',
            },
        },
    };
    expect(getListingOutputType({ params, context })).toBe(LISTING_OUTPUT_TYPE.CAROUSEL);
});
