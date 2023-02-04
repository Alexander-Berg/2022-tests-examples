jest.mock('auto-core/react/lib/gateApi');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const updateEmails = require('./updateEmails').default;
const gateApi = require('auto-core/react/lib/gateApi');

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve(true));
});

it('должен добавить новый email', () => {
    const store = mockStore();
    return store.dispatch(updateEmails({
        clientId: 'clientId',
        emailValue: 'arisots@ma2.ru, aristos@ma.ru, new@ma.ru',
        settingsSubscriptions: {
            money: [
                { emailAddress: 'arisots@ma2.ru', id: 191272 },
                { emailAddress: 'aristos@ma.ru', id: 191270 },
            ],
        },
    })).then(() => {
        expect(store.getActions()).toEqual(
            [
                {
                    type: 'ADD_SUBSCRIPTION_EMAIL_FIELD',
                    payload: {
                        category: 'money',
                    },
                },
                {
                    type: 'SUBSCRIBE',
                    payload: {
                        category: 'money',
                        client_id: 'clientId',
                        emailAddress: 'new@ma.ru',
                        id: undefined,
                        type: 'add',
                    },
                } ],
        );
    });
});

it('должен обновить текущий email', () => {
    const store = mockStore();
    return store.dispatch(updateEmails({
        clientId: 'clientId',
        emailValue: 'new@ma.ru',
        settingsSubscriptions: {
            money: [
                { emailAddress: 'arisots@ma2.ru', id: 191272 },
            ],
        },
    })).then(() => {
        expect(gateApi.getResource.mock.calls[1][1].id).toBe(191272);
        expect(store.getActions()).toEqual([
            {
                type: 'ADD_SUBSCRIPTION_EMAIL_FIELD',
                payload: {
                    category: 'money',
                },
            },
            {
                type: 'SUBSCRIBE',
                payload: {
                    category: 'money',
                    client_id: 'clientId',
                    emailAddress: 'new@ma.ru',
                    id: undefined,
                    type: 'add',
                },
            },
            {
                type: 'UNSUBSCRIBE',
                payload: {
                    category: 'money',
                    client_id: 'clientId',
                    id: undefined,
                },
            },
        ]);
    });
});

it('должен удалить email', () => {
    const store = mockStore();
    return store.dispatch(updateEmails({
        clientId: 'clientId',
        emailValue: 'arisots@ma2.ru',
        settingsSubscriptions: {
            money: [
                { emailAddress: 'arisots@ma2.ru', id: 191272 },
                { emailAddress: 'aristos@ma.ru', id: 191270 },
            ],
        },
    })).then(() => {
        expect(gateApi.getResource.mock.calls[0][1].id).toEqual(191270);
        expect(store.getActions()).toEqual([
            {
                type: 'UNSUBSCRIBE',
                payload: {
                    category: 'money',
                    client_id: 'clientId',
                    id: undefined,
                },
            },
        ]);
    });
});
