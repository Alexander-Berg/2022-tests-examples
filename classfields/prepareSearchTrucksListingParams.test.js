const createHttpReq = require('autoru-frontend/mocks/createHttpReq');
const listingViewCookie = require('auto-core/react/lib/listingViewCookie');

const prepare = require('./prepareSearchTrucksListingParams');

let blockArgs;
let req;
beforeEach(() => {
    req = createHttpReq();
    blockArgs = {
        context: { req },
        params: {},
    };
});

it('должен добавить сортировку и срок из куки, если их нет в запросе', () => {
    req.cookies.listing_view = '{"version":1,"currency":"USD"}';
    req.cookies.listing_view_session = '{"top_days":"2","sort":"cr_date-desc"}';
    blockArgs.params = {
        catalog_filter: [ { mark: 'AUDI' } ],
        section: 'used',
        category: 'trucks',
        trucks_category: 'LCV',
    };
    const result = prepare(blockArgs);

    expect(result).toEqual({
        section: 'used',
        category: 'trucks',
        trucks_category: 'LCV',
        catalog_filter: [ { mark: 'AUDI' } ],
        sort: 'cr_date-desc',
        top_days: '2',
        with_delivery: 'BOTH',
    });
});

it('не добавит сортировку из куки если она не валидная', () => {
    req.cookies.listing_view = '{"version":1,"currency":"USD"}';
    req.cookies.listing_view_session = JSON.stringify({ sort: listingViewCookie.SORT_BLACK_LIST.trucks[0] });
    blockArgs.params = {
        catalog_filter: [ { mark: 'AUDI' } ],
        section: 'used',
        category: 'trucks',
        trucks_category: 'LCV',
    };
    const result = prepare(blockArgs);

    expect(result).toEqual({
        section: 'used',
        category: 'trucks',
        trucks_category: 'LCV',
        catalog_filter: [ { mark: 'AUDI' } ],
        sort: undefined,
        top_days: undefined,
        with_delivery: 'BOTH',
    });
});
