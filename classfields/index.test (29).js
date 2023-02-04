const reducer = require('./index');

it('должен правильно обработать неизвестное событие', () => {
    expect(reducer(undefined, { type: '@TEST_ACTION' })).toMatchObject({
        // base
        autoPopup: {},
        config: {},
        cookies: {},
        geo: {},
        notifier: {},
        user: {},

        // project
        ads: {},
        bunker: {},
        compare: {},
        favorites: {},
        footerGeoInfo: {},
        mag: {},
        magListing: {},
        offers: {},
        panoramaHotSpots: {},
        reviewComments: {},
        routing: {},
        state: {},
        subscriptions: {},
    });
});
