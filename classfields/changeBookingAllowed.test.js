/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(() => Promise.resolve({ status: 'SUCCESS' })),
}));

const { getResource } = require('auto-core/react/lib/gateApi');
const actionTypes = require('../actionTypes');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const notifierActionTypes = require('auto-core/react/dataDomain/notifier/types');

const changeBookingAllowed = require('./changeBookingAllowed');

const clientId = '20101';
const offerID = '16064346';
const hash = 'fdba6bea';
const offerHashId = offerID + '-' + hash;
let store;
let actionParams;

beforeEach(() => {
    store = mockStore({ config: { client: { id: clientId } } });

    store = mockStore({
        config: {
            customerRole: 'manager',
            client: {
                id: clientId,
            },
        },
        sales: {
            items: [
                {
                    additional_info: { booking: {} },
                    id: offerID,
                    hash: hash,
                    category: 'CARS',
                },
            ],
        },
    });

    actionParams = {
        allowed: true,
        offerID: offerHashId,
    };
});

it('должен вызвать ресурс "offerChangeBookingAllowed" с правильно замапленными параметрами', () => {
    store.dispatch(
        changeBookingAllowed(actionParams),
    );

    expect(getResource).toHaveBeenCalledWith('offerChangeBookingAllowed', {
        allowed: true,
        offerID: offerHashId,
        category: 'cars',
        dealer_id: clientId,
    });
});

describe('для успешного ответа', () => {
    it('на разрешение возможности бронирования должен диспатчить правильный экшен и показывать правильный текст в нотификации', () => {
        expect.assertions(1);

        return store.dispatch(
            changeBookingAllowed(actionParams),
        )
            .then(() => {
                const actions = store.getActions();

                const changeBookingAllowedAction = {
                    type: actionTypes.CHANGE_BOOKING_ALLOWED,
                    payload: { allowed: true, saleId: offerID },
                };

                const notificationAction = {
                    type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                    payload: { message: 'Объявление теперь доступно для бронирования', view: 'success' },
                };

                expect(actions).toEqual([ changeBookingAllowedAction, notificationAction ]);
            });
    });

    it('на запрещение возможности бронирования должен диспатчить правильный экшен и показывать правильный текст в нотификации', () => {
        expect.assertions(1);

        return store.dispatch(
            changeBookingAllowed({ allowed: false, offerID: offerHashId }),
        )
            .then(() => {
                const actions = store.getActions();

                const changeBookingAllowedAction = {
                    type: actionTypes.CHANGE_BOOKING_ALLOWED,
                    payload: { allowed: false, saleId: offerID },
                };

                const notificationAction = {
                    type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                    payload: { message: 'Объявление теперь недоступно для бронирования', view: 'success' },
                };

                expect(actions).toEqual([ changeBookingAllowedAction, notificationAction ]);
            });
    });
});

it('для неуспешных ответов должен реджектить промис и показывать стандартную нотификацию с ошибкой', () => {
    expect.assertions(1);

    getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(
        changeBookingAllowed(actionParams),
    )
        .then(() => {
            const actions = store.getActions();

            const notificationAction = {
                type: notifierActionTypes.NOTIFIER_SHOW_MESSAGE,
                payload: { message: expect.any(String), view: 'error' },
            };

            return expect(actions).toEqual([ notificationAction ]);
        });
});
