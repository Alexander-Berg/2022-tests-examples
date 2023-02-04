/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/proofOfWork');

const fetchVinReport = require('./fetchVinReport');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

let responseGood;
let response404;
let response500;
let store;
beforeEach(() => {
    store = mockStore({});

    responseGood = {
        billing: {},
        fetched: true,
        report: {},
        status: 'SUCCESS',
    };
    response404 = { error: 'VIN_CODE_NOT_FOUND' };
    response500 = { error: 'UNKNOWN_ERROR' };
});

it('должен сохранить отчет в стор при хорошем ответе', () => {
    fetch.mockResponseOnce(JSON.stringify(responseGood));

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then((response) => {
        expect(response).toEqual(responseGood);

        expect(store.getActions()).toEqual([
            {
                type: 'VIN_REPORT_FETCHING',
                payload: {
                    reportParams: { vin_or_license_plate: '123', isCardPage: false, decrement_quota: false },
                    paymentParams: {},
                },
            },
            {
                type: 'VIN_REPORT_RESOLVED',
                payload: responseGood,
            },
        ]);
    });
});

it('должен сохранить ошибку в стор при ответе 404', () => {
    fetch.mockResponseOnce(JSON.stringify(response404));

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (response) => {
            expect(response).toEqual('VIN_CODE_NOT_FOUND');

            expect(store.getActions()).toEqual([
                {
                    type: 'VIN_REPORT_FETCHING',
                    payload: {
                        reportParams: { vin_or_license_plate: '123', isCardPage: false, decrement_quota: false },
                        paymentParams: {},
                    },
                },
                {
                    type: 'VIN_REPORT_REJECTED',
                    payload: 'VIN_CODE_NOT_FOUND',
                },
            ]);
        });
});

it('должен сохранить ошибку в стор при ответе 500', () => {
    fetch.mockResponseOnce(JSON.stringify(response500));

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (response) => {
            expect(response).toEqual('UNKNOWN_ERROR');

            expect(store.getActions()).toEqual([
                {
                    type: 'VIN_REPORT_FETCHING',
                    payload: {
                        reportParams: { vin_or_license_plate: '123', isCardPage: false, decrement_quota: false },
                        paymentParams: {},
                    },
                },
                {
                    type: 'VIN_REPORT_REJECTED',
                    payload: 'UNKNOWN_ERROR',
                },
            ]);
        });
});

it('должен сохранить ошибку в стор при неответе', () => {
    // эмулируем аборт запроса
    fetch.mockAbortOnce();

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then(
        () => Promise.reject('UNEXPECTED_RESOLVE'),
        (response) => {
            expect(response).toEqual('CLIENT_ERROR_AbortError');

            expect(store.getActions()).toEqual([
                {
                    type: 'VIN_REPORT_FETCHING',
                    payload: {
                        reportParams: { vin_or_license_plate: '123', isCardPage: false, decrement_quota: false },
                        paymentParams: {},
                    },
                },
                {
                    type: 'VIN_REPORT_REJECTED',
                    payload: 'CLIENT_ERROR_AbortError',
                },
            ]);
        });
});

it('должен отправить isCardPage=true, если вызываем экшн с карточки', () => {
    fetch.mockResponseOnce(JSON.stringify(responseGood));

    const store = mockStore({
        config: { data: { pageType: 'card-something' } },
    });

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then((response) => {
        expect(response).toEqual(responseGood);

        expect(store.getActions()).toEqual([
            {
                type: 'VIN_REPORT_FETCHING',
                payload: {
                    reportParams: { vin_or_license_plate: '123', isCardPage: true, decrement_quota: false },
                    paymentParams: {},
                },
            },
            {
                type: 'VIN_REPORT_RESOLVED',
                payload: responseGood,
            },
        ]);
    });
});

it('должен отправить isCardPage=true, если вызываем экшн со страницы перекупа', () => {
    fetch.mockResponseOnce(JSON.stringify(responseGood));

    const store = mockStore({
        config: { data: { pageType: 'reseller-sales' } },
    });

    return store.dispatch(
        fetchVinReport(
            JSON.stringify({ vin_or_license_plate: '123' }),
            JSON.stringify({}),
            false,
        ),
    ).then((response) => {
        expect(response).toEqual(responseGood);

        expect(store.getActions()).toEqual([
            {
                type: 'VIN_REPORT_FETCHING',
                payload: {
                    reportParams: { vin_or_license_plate: '123', isCardPage: true, decrement_quota: false },
                    paymentParams: {},
                },
            },
            {
                type: 'VIN_REPORT_RESOLVED',
                payload: responseGood,
            },
        ]);
    });
});
