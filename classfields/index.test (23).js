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
        compare: {},
        favorites: {},
        routing: {},
        state: {},
        subscriptions: {},
    });
});
