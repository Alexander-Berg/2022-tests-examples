const _ = require('lodash');

jest.mock('auto-core/lib/core/isMobileApp');

const getListingGrouping = require('./getListingGrouping');

const LISTING_OUTPUT_TYPE = require('auto-core/data/listing/OutputTypes').default;
const listingViewCookie = require('auto-core/react/lib/listingViewCookie');

const isMobile = require('auto-core/lib/core/isMobileApp');

const CONTEXT = {
    req: {
        router: {
            params: {
                output_type: LISTING_OUTPUT_TYPE.MODELS,
            },
        },
        cookies: {
            [ listingViewCookie.COOKIE_NAME ]: '{"output_type":"models_list","version":1}',
        },
        experimentsData: {
            has: _.noop,
        },
    },
};

beforeEach(() => {
    isMobile.mockReset();
});

// eslint-disable-next-line max-len
it('должен вернуть группировку по-умолчанию, если это не мобильное приложение, категория - легковые, тип выдачи - по-моделям и в параметрах не передана группировка', () => {
    const params = {
        category: 'cars',
    };
    isMobile.mockReturnValue(false);
    expect(getListingGrouping({ params, context: CONTEXT })).toStrictEqual([ 'CONFIGURATION' ]);
});

it('должен вернуть переданную в параметрах группировку', () => {
    const params = {
        category: 'cars',
        group_by: [ 'COMPLECTATION' ],
    };
    isMobile.mockReturnValue(false);
    expect(getListingGrouping({ params, context: CONTEXT })).toStrictEqual([ 'COMPLECTATION' ]);
});

it('должен вернуть группировку, если это мобильное приложение и новые тачки', () => {
    const params = {
        category: 'cars',
        section: 'new',
    };
    isMobile.mockReturnValue(true);
    expect(getListingGrouping({ params, context: CONTEXT })).toStrictEqual([ 'CONFIGURATION' ]);
});

it('не должен вернуть группировку, если это мобильное приложение и не новые тачки', () => {
    const params = {
        category: 'cars',
        section: 'all',
    };
    isMobile.mockReturnValue(true);
    expect(getListingGrouping({ params, context: CONTEXT })).toStrictEqual([ 'CONFIGURATION' ]);
});

it('не должен вернуть группировку, если это не легковые автомобили', () => {
    const params = {
        category: 'trucks',
    };
    isMobile.mockReturnValue(false);
    expect(getListingGrouping({ params, context: CONTEXT })).toBeUndefined();
});

it('не должен вернуть группировку, если тип выдачи не по-моделям', () => {
    const params = {
        category: 'trucks',
    };
    const context = {
        req: {
            router: {
                params: {
                    output_type: LISTING_OUTPUT_TYPE.LIST,
                },
            },
            cookies: {
                [ listingViewCookie.COOKIE_NAME ]: '{"output_type":"list","version":1}',
            },
            experimentsData: {
                has: _.noop,
            },
        },
    };
    isMobile.mockReturnValue(false);
    expect(getListingGrouping({ params, context })).toBeUndefined();
});
