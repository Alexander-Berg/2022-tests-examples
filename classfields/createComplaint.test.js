/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(() => Promise.resolve({})),
    };
});

const _ = require('lodash');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createComplaint = require('./createComplaint');
const MockDate = require('mockdate');
const getResource = require('auto-core/react/lib/gateApi').getResource;

const { NOTIFIER_SHOW_MESSAGE } = require('auto-core/react/dataDomain/notifier/types');

const userMock = require('auto-core/react/dataDomain/user/mocks/dealerWithAccess.mock');

let store;
let actionParams;

beforeEach(() => {
    const user = _.cloneDeep(userMock);
    user.data.yandex_staff_login = 'imperator';

    store = mockStore({
        user: user,
    });

    actionParams = {
        complaint: {
            reason: 'ID',
            comment: 'никогда такого не было и вот...',
            date: '2019-06-23',
            time: '12:06:12',
            phone: '+7 926 112-33-33',
        },
        redirect: {
            phone: '+7 916 112-33-33',
            original: '+7 906 112-33-33',
            telepony_info: { domain: 'autoru' },
        },
    };

    MockDate.set('2018-01-05T00:00:00+03:00');
});

afterEach(() => {
    MockDate.reset();
});

it('должен вызвать ресурс "createRedirectComplaint" с правильно замапленными параметрами', () => {
    getResource.mockImplementation(() => Promise.resolve({ status_code: 200 }));

    store.dispatch(
        createComplaint(actionParams),
    );

    expect(getResource).toHaveBeenCalledWith('createRedirectComplaint', {
        createRequest: JSON.stringify({
            cause: {
                causeType: 'ID',
                causeDescription: 'никогда такого не было и вот...',
            },
            callInfo: {
                callTime: '2019-06-23T12:06:12+03:00',
                callerNumber: '+79261123333',
            },
        }),
        domain: 'autoru',
        originalPhone: '+79061123333',
        redirectPhone: '+79161123333',
        staffLogin: 'imperator',
    });
});

it('для статус кода 200 должен показать успешную нотификацию', () => {
    expect.assertions(1);

    getResource.mockImplementation(() => Promise.resolve({ status_code: 200 }));

    return store.dispatch(
        createComplaint(actionParams),
    )
        .then(() => {
            const actions = store.getActions();

            const notificationAction = {
                type: NOTIFIER_SHOW_MESSAGE,
                payload: { message: 'Жалоба успешно добавлена', view: 'success' },
            };

            expect(actions).toEqual([ notificationAction ]);
        });
});

it('для статус кода 400 должен реджектить экшен и диспатчить нотификацию', () => {
    expect.assertions(1);

    getResource.mockImplementation(() => Promise.resolve({ status_code: 400 }));

    return store.dispatch(
        createComplaint(actionParams),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            const actions = store.getActions();

            const notificationAction = {
                type: NOTIFIER_SHOW_MESSAGE,
                payload: { message: 'Жалоба на подменный номер +79161123333 уже существует', view: 'error' },
            };

            return expect(actions).toEqual([ notificationAction ]);
        },
    );
});

it('для любых других неуспешных ответов должен режектить промис и показывать стандартную нотификацию с ошибкой', () => {
    expect.assertions(1);

    return store.dispatch(
        createComplaint(actionParams),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            const actions = store.getActions();

            const notificationAction = {
                type: NOTIFIER_SHOW_MESSAGE,
                payload: { message: expect.any(String), view: 'error' },
            };

            return expect(actions).toEqual([ notificationAction ]);
        },
    );
});
