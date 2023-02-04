const _ = require('lodash');
const vinReportMock = require('auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock');
const hasEnoughDataForSellerReport = require('./hasEnoughDataForSellerReport');

let state;

beforeEach(() => {
    state = {
        vinReport: { data: _.cloneDeep(vinReportMock) },
    };
});

describe('вернет false', () => {
    it('если отчёт еще не получен', () => {
        state.vinReport.data.fetched = false;
        expect(hasEnoughDataForSellerReport(state)).toBe(false);
    });

    it('если отчёт уже оплачен', () => {
        state.vinReport.data.report.report_type = 'PAID_REPORT';
        expect(hasEnoughDataForSellerReport(state)).toBe(false);
    });

    it('если в отчёте нет записей про тех осмотр и только 2 записи про пробег', () => {
        state.vinReport.data.report.quality = 2;
        expect(hasEnoughDataForSellerReport(state)).toBe(false);
    });
});

describe('вернет true', () => {
    it('если отчёт получен, он не оплачен и есть 3 и более записи про пробег', () => {
        state.vinReport.data.report.quality = 3;
        expect(hasEnoughDataForSellerReport(state)).toBe(true);
    });
});
