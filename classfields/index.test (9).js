const preparer = require('./index');
const _ = require('lodash');
const offer = require('autoru-frontend/mockData/responses/offer.mock.json');

it('counters.calls_all => counters.calls', () => {
    const offerWithCalls = Object.assign({}, _.cloneDeep(offer), { counters: { calls_all: 101 } });
    expect(preparer(offerWithCalls).counters).toEqual({
        calls_all: 101,
        calls: 101,
    });
});
