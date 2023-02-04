/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const mockDefaultId = '12345';
jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(() => Promise.resolve({ id: mockDefaultId })),
}));

const { getResource } = require('auto-core/react/lib/gateApi');
const actionTypes = require('../actionTypes');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const notifierActionTypes = require('auto-core/react/dataDomain/notifier/types');

const subscriptions = require('./subscriptions');

let store;

beforeEach(() => {
    store = mockStore({});
    getResource.mockImplementation(() => Promise.resolve({ id: mockDefaultId }));
});

const actionParams = {
    category: 'booking',
    emailAddress: 'demo@demo.ru',
};

describe('подписка', () => {

    it('вызовет ресурс добавления подписки, если не передан id', () => {
        store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress));

        expect(getResource).toHaveBeenCalledWith('addClientSubscription', { ...actionParams, type: 'add' });
    });

    it('вызовет ресурс обновления подписки, если передан id', () => {
        store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress, mockDefaultId));

        expect(getResource).toHaveBeenCalledWith('updateClientSubscription', { ...actionParams, id: mockDefaultId, type: 'update' });
    });

    it('для успешного ответа должен диспатчить экшн обновления подписок и стандартную успешную нотификацию', () => {
        expect.assertions(1);

        return store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress, mockDefaultId))
            .then(() => {
                const actions = store.getActions();

                const subscribeAction = {
                    type: actionTypes.SUBSCRIBE,
                    payload: { ...actionParams, type: 'update', id: mockDefaultId },
                };

                const notificationAction = {
                    type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                    payload: { message: 'Данные успешно сохранены', view: 'success' },
                };

                expect(actions).toEqual([ subscribeAction, notificationAction ]);
            });
    });

    describe('для неуспешных ответов должен реджектить промис и показывать', () => {
        it('нотификацию со стандартной ошибкой', () => {
            expect.assertions(1);

            getResource.mockImplementation(() => Promise.reject());

            return store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress))
                .then(() => {
                    const actions = store.getActions();

                    const notificationAction = {
                        type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                        payload: { message: 'Произошла ошибка при изменении данных. Повторите попытку позже.', view: 'error' },
                    };

                    return expect(actions).toEqual([ notificationAction ]);
                });
        });

        it('нотификацию с сообщением о невалидном адресе почты', () => {
            expect.assertions(1);

            getResource.mockImplementation(() => Promise.resolve({ errorCode: 'IllegalArgument' }));

            return store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress))
                .then(() => {
                    const actions = store.getActions();

                    const notificationAction = {
                        type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                        payload: { message: 'Произошла ошибка при изменении данных: невалидный почтовый адрес', view: 'error' },
                    };

                    return expect(actions).toEqual([ notificationAction ]);
                });
        });

        it('нотификацию о попытке оформления уже существующей подписки', () => {
            expect.assertions(1);

            getResource.mockImplementation(() => Promise.reject({ code: 409 }));

            return store.dispatch(subscriptions.subscribe(actionParams.category, actionParams.emailAddress))
                .then(() => {
                    const actions = store.getActions();

                    const notificationAction = {
                        type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                        payload: { message: 'Эта подписка уже существует', view: 'error' },
                    };

                    return expect(actions).toEqual([ notificationAction ]);
                });
        });
    });

});

describe('отписка', () => {
    it('для успешного ответа должен диспатчить экшн обновления подписок и стандартную успешную нотификацию', () => {
        expect.assertions(1);

        return store.dispatch(subscriptions.unsubscribe(actionParams.category, mockDefaultId))
            .then(() => {
                const actions = store.getActions();

                const subscribeAction = {
                    type: actionTypes.UNSUBSCRIBE,
                    payload: { category: actionParams.category, id: mockDefaultId },
                };

                const notificationAction = {
                    type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                    payload: { message: 'Данные успешно сохранены', view: 'success' },
                };

                expect(actions).toEqual([ subscribeAction, notificationAction ]);
            });
    });

    it('для неуспешного ответа должен диспатчить нотификаццию с ошибкой', () => {
        expect.assertions(1);

        getResource.mockImplementation(() => Promise.reject());

        return store.dispatch(subscriptions.unsubscribe(actionParams.category, mockDefaultId))
            .then(() => {
                const actions = store.getActions();

                const notificationAction = {
                    type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                    payload: { message: 'Произошла ошибка при изменении данных. Повторите попытку позже.', view: 'error' },
                };

                return expect(actions).toEqual([ notificationAction ]);
            });
    });
});
