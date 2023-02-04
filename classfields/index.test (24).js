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
        breadcrumbsPublicApi: {},
        bunker: {},
        callsStats: {},
        compare: {},
        comparison: {},
        favorites: {},
        footerGeoInfo: {},
        routing: {},
        state: {},
        subscriptions: {},
        versus: {},
    });
});
