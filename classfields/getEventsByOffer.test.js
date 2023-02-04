const getEventsByOffer = require('auto-core/react/lib/getEventsByOffer');
const offerUsed = require('autoru-frontend/mockData/responses/offer.mock').offerUsed;
const offerNew = require('autoru-frontend/mockData/responses/offer.mock').offerNew;

it('Должен вернуть 5 событий для новых', () => {
    expect(getEventsByOffer(offerNew)).toEqual([
        { counter: 'criteo', type: 'trackTransaction' },
        { counter: 'adwords', type: 'conversion' },
        { counter: 'rtb', type: 'conversion' },
        { counter: 'soloway', type: 'purchase' },
        { counter: 'hybrid', type: 'purchase' },
    ]);
});

it('Должен вернуть 6 событий для бу легковых', () => {
    expect(getEventsByOffer(offerUsed)).toEqual([
        { counter: 'criteo', type: 'trackTransaction' },
        { counter: 'adwords', type: 'conversion' },
        { counter: 'rtb', type: 'conversion' },
        { counter: 'soloway', type: 'purchase' },
        { counter: 'hybrid', type: 'purchase' },
        { counter: 'myTarget', type: 'purchase' },
    ]);
});
