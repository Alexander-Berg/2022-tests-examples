const reducer = require('./index');

it('должен правильно обработать неизвестное событие', () => {
    expect(reducer(undefined, { type: '@TEST_ACTION' })).toMatchObject({
        additionalOptions: {},
        autoPopup: {},
        bunker: {},
        card: {},
        compare: {},
        config: {},
        currencies: {},
        equipmentDictionary: {},
        favorites: {},
        formFields: {},
        formVas: {},
        geo: {},
        notifier: {},
        routing: {},
        state: {},
        statsPredict: {},
        subscriptions: {},
        user: {},
    });
});
