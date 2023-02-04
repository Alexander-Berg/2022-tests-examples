/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const sendComplaint = require('./sendComplaint');
const gateApi = require('auto-core/react/lib/gateApi');

const CALL_BILLING_COMPLAINT_STATUSES = require('www-cabinet/data/calls/call-billing-complaint-states.json');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const withCalls = require('www-cabinet/react/dataDomain/calls/mocks/withCalls.mock');

beforeEach(() => {
    gateApi.getResource.mockImplementation(() => Promise.resolve());
});

it('должен вызвать getResource с параметрами тарифа звонков', () => {
    const store = mockStore({
        calls: withCalls,
    });

    store.dispatch(
        sendComplaint({
            call: { call_id: 'call_id_1' },
            text: 'text',
            email: 'email',
        }),
    );

    expect(gateApi.getResource).toHaveBeenCalledWith('createCallComplaint', {
        call_id: 'call_id_1',
        text: 'text',
        email: 'email',
    });
});

it('должен вызвать экшен нотификации об ошибке, если ручка ответила с ошибкой', () => {
    expect.assertions(1);
    const store = mockStore({});

    gateApi.getResource.mockImplementation(() => Promise.reject());

    return store.dispatch(sendComplaint({})).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        () => {
            expect(store.getActions()).toContainEqual({
                type: 'NOTIFIER_SHOW_MESSAGE',
                payload: {
                    message: 'Произошла ошибка, попробуйте ещё раз',
                    view: 'error',
                },
            });
        },
    );
});

it('должен обновить жалобы в звонке на статус "в ревью" после успешного ответа', () => {
    expect.assertions(2);
    const store = mockStore({
        calls: withCalls,
    });

    return store.dispatch(
        sendComplaint({ call: { call_id: '123' } }),
    ).then(() => {
        const oldComplaintState = withCalls.callsList.calls[0].billing.complaint_state;
        const complaintState = store.getActions()[0].payload.calls[0].billing.complaint_state;

        expect(oldComplaintState).not.toBe(complaintState);
        expect(complaintState).toBe(CALL_BILLING_COMPLAINT_STATUSES.REVIEW);
    });
});
