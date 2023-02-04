const { getCreditMonthlyPayment } = require('./index');

describe('Tinkoff utils', () => {
    describe('Рассчёт кредита', () => {
        it('Кредит 200 000 на 1 год', () => {
            expect(getCreditMonthlyPayment(200000, 1)).toBe(17200);
        });

        it('Кредит 500 000 на 5 год', () => {
            expect(getCreditMonthlyPayment(500000, 5)).toBe(9500);
        });
    });
});
