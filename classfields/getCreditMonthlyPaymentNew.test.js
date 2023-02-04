const getCreditMonthlyPaymentNew = require('./getCreditMonthlyPaymentNew');

const creditConfig = require('auto-core/react/dataDomain/banks/mocks/dealerCreditConfig.mock');

it('должен верно рассчитать сумму ежемесячного платежа 1', () => {
    expect(getCreditMonthlyPaymentNew({
        amount: 200000,
        term: 1,
        creditConfig,
    })).toBe(17200);
});

it('должен верно рассчитать сумму ежемесячного платежа 2', () => {
    expect(getCreditMonthlyPaymentNew({
        amount: 500000,
        term: 5,
        creditConfig,
    })).toBe(9500);
});
