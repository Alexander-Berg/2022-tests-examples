const getSummaryInfo = require('./getSummaryInfo');

const formatPrice = require('auto-core/lib/util/formatPrice');

const tariffs = require('../mock/tariffs');
const tariffsCounts = require('../mock/counts');

it('должен корректно форматировать промежуточную сумму для тарифов', () => {
    const state = {
        tariffs,
        tariffsCounts,
        summaryPeriod: 1,
    };

    const expectedSummaryInfo = {
        CARS_NEW: `${ formatPrice(1250, 'RUR', true) } / 1 день`,
        CARS_USED: `${ formatPrice(1690, 'RUR', true) } / 1 день`,
        MOTO: `${ formatPrice(70, 'RUR', true) } / 1 день`,
        TRUCKS: '',
    };

    expect(getSummaryInfo({ calculator: state })).toEqual(expectedSummaryInfo);
});
