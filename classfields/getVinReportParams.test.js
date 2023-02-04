const getVinReportParams = require('./getVinReportParams');

it('Должен вернуть параметры отчёта, если это отчёт по vin', () => {
    expect(getVinReportParams({
        vin_or_license_plate: '123456',
    })).toEqual({
        vinReportParams: '{"vin_or_license_plate":"123456"}',
        vinReportPaymentParams: '{"vin_or_license_plate":"123456"}',
    });
});

it('Должен вернуть параметры отчёта, если это отчёт по офферу', () => {
    expect(getVinReportParams({
        offer_id: '123-456',
    })).toEqual({
        vinReportParams: '{"offerID":"123-456","category":"cars"}',
        vinReportPaymentParams: '{"offerId":"123-456"}',
    });
});
