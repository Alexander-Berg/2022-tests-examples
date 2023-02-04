const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const prepare = require('./prepareSearchCarsListingParams');

let blockArgs;
let req;
beforeEach(() => {
    req = createHttpReq();
    blockArgs = {
        context: { req },
        params: {},
    };
});

it('должен заменить nameplate_name на -- в случае бесшильдовой модификации', () => {
    blockArgs.params = {
        catalog_filter: [ { mark: 'AUDI', model: '100', nameplate_name: '100' } ],
        section: 'used',
        category: 'cars',
    };
    const result = prepare(blockArgs);

    expect(result).toEqual({
        section: 'used',
        category: 'cars',
        catalog_filter: [ { mark: 'AUDI', model: '100', nameplate_name: '--' } ],
        currency: undefined,
        offer_grouping: 'false',
        sort: undefined,
        top_days: undefined,
        with_delivery: 'BOTH',
    });
});

it('должен добавить валюту из куки, если ее нет в запросе', () => {
    req.cookies.listing_view = '{"version":1,"currency":"USD"}';
    req.cookies.listing_view_session = '{"top_days":"2","sort":"cr_date-desc"}';
    blockArgs.params = {
        catalog_filter: [ { mark: 'AUDI' } ],
        section: 'used',
        category: 'cars',
    };
    const result = prepare(blockArgs);

    expect(result).toEqual({
        section: 'used',
        category: 'cars',
        catalog_filter: [ { mark: 'AUDI' } ],
        currency: 'USD',
        offer_grouping: 'false',
        with_delivery: 'BOTH',
    });
});
