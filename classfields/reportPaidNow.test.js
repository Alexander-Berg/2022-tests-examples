const reportPaidNow = require('./reportPaidNow');

const TEST_CASES = [
    {
        description: 'один и тот же отчёт по vin, был бесплатным, стал оплаченым',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        result: true,
    },
    {
        description: 'один и тот же отчёт по offer_id, был бесплатным, стал оплаченым',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "offerID": "OFFER_ID" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "offerID": "OFFER_ID" }',
        },
        result: true,
    },
    {
        description: 'статус отчёта не изменился',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        result: false,
    },
    {
        description: 'это не один и тот же отчёт (по vin)',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN_1" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN_2" }',
        },
        result: false,
    },
    {
        description: 'это не один и тот же отчёт (по offer_id)',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "offerID": "OFFER_ID_1" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "offerID": "OFFER_ID_2" }',
        },
        result: false,
    },
    {
        description: 'это просмотр разных оплаченных отчётов по очереди',
        prevProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN_1" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN_2" }',
        },
        result: false,
    },
    {
        description: 'пользователь залогинился под аккаунтом, для которого отчёт оплачен',
        prevProps: {
            isAuth: false,
            vinReport: { report: { report_type: 'FREE_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        currentProps: {
            isAuth: true,
            vinReport: { report: { report_type: 'PAID_REPORT' } },
            vinReportParams: '{ "vin_or_license_plate": "VIN" }',
        },
        result: false,
    },
];

describe('Проверка что произошло событие оплаты:', () => {
    TEST_CASES.forEach(({ currentProps, description, prevProps, result }) => {
        it(`Если ${ description } должен вернуть ${ result }`, () => {
            expect(reportPaidNow(prevProps, currentProps)).toEqual(result);
        });
    });
});
