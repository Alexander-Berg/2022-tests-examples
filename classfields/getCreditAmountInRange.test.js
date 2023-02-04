const getCreditAmountInRange = require('./getCreditAmountInRange');

const creditConfig = require('auto-core/react/dataDomain/banks/mocks/dealerCreditConfig.mock');

const { CREDIT_MAX_AMOUNT, CREDIT_MIN_AMOUNT } = creditConfig;

it('должен вернуть минимальную сумму по конфигу, если запрашиваемая сумма ниже минимальной', () => {
    expect(getCreditAmountInRange({
        amount: 7000,
        creditConfig,
    })).toBe(CREDIT_MIN_AMOUNT);
});

it('должен вернуть максимальную сумму по конфигу, если запрашиваемая сумма выше максимальной', () => {
    expect(getCreditAmountInRange({
        amount: 3300000,
        creditConfig,
    })).toBe(CREDIT_MAX_AMOUNT);
});

it('должен вернуть исходное значение суммы, если она укладывается в границы минимума и максимума конфига', () => {
    expect(getCreditAmountInRange({
        amount: 130000,
        creditConfig,
    })).toBe(130000);
});

it('должен вернуть исходное значение суммы, если она выше максимальной, но прокинут флаг shouldIgnoreMaxLimit', () => {
    expect(getCreditAmountInRange({
        amount: 3300000,
        creditConfig,
        shouldIgnoreMaxLimit: true,
    })).toBe(3300000);
});
