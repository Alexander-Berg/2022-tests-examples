const reducer = require('./index');

it('должен правильно обработать неизвестное событие', () => {
    expect(reducer(undefined, { type: '@TEST_ACTION' })).toMatchObject({
        // base
        config: {},
        cookies: {},
        creditApplicationTimezone: {},
        geo: {},
        notifier: {},

        // project
        user: {},
        bunker: {},
        credit: {},
        dicts: {},
        routing: {},
        state: {},
        yateamUser: {},
    });
});
