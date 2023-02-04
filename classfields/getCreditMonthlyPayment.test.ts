import getCreditMonthlyPayment from './getCreditMonthlyPayment';

it('должен верно рассчитать сумму ежемесячного платежа для целого количества лет', () => {
    expect(getCreditMonthlyPayment({
        amount: 200000,
        term: 12,
        rate: 0.099,
        step: 50,
    })).toBe(17750);
});

it('должен верно рассчитать сумму ежемесячного платежа для нецелого количества лет', () => {
    expect(getCreditMonthlyPayment({
        amount: 787620,
        term: 52,
        rate: 0.139,
        step: 100,
    })).toBe(20500);
});
