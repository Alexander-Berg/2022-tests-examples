const reducer = require('./index');

it('должен правильно обработать неизвестное событие', () => {
    expect(reducer(undefined, { type: '@TEST_ACTION' })).toMatchObject({
        // base
        config: {},
        cookies: {},
        geo: {},
        notifier: {},
        user: {},

        // project
        bunker: {},
    });
});
