jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const gateApi = require('auto-core/react/lib/gateApi');

const fetchApplicationPhone = require('./fetchApplicationPhone');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const actionTypes = require('../actionTypes');

const APPLICATION_PHONE_STATUSES = require('www-cabinet/data/matchApplications/applications-phones-statuses');

it('должен добавлять к телефонам новый телефон в статусе "pending", и проставлять в "success" при успешном ответе', () => {
    expect.assertions(1);

    const store = mockStore({
        matchApplications: {
            applicationsPhones: {
                application_id_1: {
                    status: APPLICATION_PHONE_STATUSES.SUCCESS,
                },
            },
        },
    });

    const responseMock = Promise.resolve({ info: { foo: '123' } });

    gateApi.getResource.mockImplementation(() => responseMock);

    return store.dispatch(
        fetchApplicationPhone('application_id_2'),
    )
        .then(() => {
            expect(store.getActions()).toEqual([
                {
                    type: actionTypes.UPDATE_APPLICATIONS_PHONES,
                    payload: {
                        application_id_1: {
                            status: APPLICATION_PHONE_STATUSES.SUCCESS,
                        },
                        application_id_2: {
                            status: APPLICATION_PHONE_STATUSES.PENDING,
                        },
                    },
                },
                {
                    type: actionTypes.UPDATE_APPLICATIONS_PHONES,
                    payload: {
                        application_id_1: {
                            status: APPLICATION_PHONE_STATUSES.SUCCESS,
                        },
                        application_id_2: {
                            status: APPLICATION_PHONE_STATUSES.SUCCESS,
                            foo: '123',
                        },
                    },
                },
            ]);
        });
});

it('должен проставлять телефон в статус "expired", если бэк ответил 403', () => {
    expect.assertions(1);

    const store = mockStore({
        matchApplications: {
            applicationsPhones: {},
        },
    });

    const badResponseMock = Promise.reject({ status_code: 403 });

    gateApi.getResource.mockImplementation(() => badResponseMock);

    return store.dispatch(
        fetchApplicationPhone('application_id_1'),
    )
        .then(() => {
            const lastAction = store.getActions()[1];

            expect(lastAction.payload.application_id_1.status).toBe(APPLICATION_PHONE_STATUSES.EXPIRED);
        });
});

it('должен проставлять телефон в статус "error", если бэк ответил какой-то ошибкой, кроме 403', () => {
    expect.assertions(1);

    const store = mockStore({
        matchApplications: {
            applicationsPhones: {},
        },
    });

    const badResponseMock = Promise.reject({ status_code: 500 });

    gateApi.getResource.mockImplementation(() => badResponseMock);

    return store.dispatch(
        fetchApplicationPhone('application_id_1'),
    )
        .then(() => {
            const lastAction = store.getActions()[1];

            expect(lastAction.payload.application_id_1.status).toBe(APPLICATION_PHONE_STATUSES.ERROR);
        });

});
