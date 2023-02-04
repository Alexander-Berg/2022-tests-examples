const getFullState = require('./getFullState');
const createHttpReq = require('autoru-frontend/mocks/createHttpReq');

const reqMock = createHttpReq();
const yaCounterIdListingCarousel = require('../../data/dicts/yaCounterIdListingCarousel.json');

const req = {
    ...reqMock,
    geoIds: [ 213 ],
    geoParents: [],
    geoIdsInfo: [],
    regionByIp: { type: 1 },
    regionByIpParents: [],
    gradius: undefined,
};

it('Должен отдайт стейт с metrikId = defaultMetrikaId', () => {
    expect(getFullState(req, {}).config.metrika[0].id).toEqual(22753222);
});

it(`Должен отдайт стейт с metrikaId = yaCounterIdListingCarousel для widget-listing-carousel`, () => {
    expect(getFullState(req, {
        routeName: 'widget-listing-carousel',
    }).config.metrika[0].id).toEqual(yaCounterIdListingCarousel);
});
