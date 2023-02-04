import { isAmountValid, isInvoiceExternalIdValid } from '../validation';

describe('Валидация переносов', () => {
    describe('Валидация номера счёта', () => {
        it('валидное значение', () => {
            expect(isInvoiceExternalIdValid('фывфыв')).toBeTruthy();
        });

        it('невалидное значение - пустое', () => {
            expect(isInvoiceExternalIdValid('')).toBeFalsy();
        });

        it('невалидное значение - пробелы', () => {
            expect(isInvoiceExternalIdValid('')).toBeFalsy();
        });
    });

    describe('Валидация суммы', () => {
        it('валидное значение - больше нуля и меньше лимита', () => {
            expect(isAmountValid('50', '100')).toBeTruthy();
        });

        it('валидное значение - равно лимиту', () => {
            expect(isAmountValid('100', '100')).toBeTruthy();
        });

        it('невалидное значение - пустое', () => {
            expect(isAmountValid('', '100')).toBeFalsy();
        });

        it('невалидное значение - не число', () => {
            expect(isAmountValid('фыв', '100')).toBeFalsy();
        });

        it('невалидное значение - превышает лимит', () => {
            expect(isAmountValid('101', '100')).toBeFalsy();
        });

        it('невалидное значение - отрицательное число', () => {
            expect(isAmountValid('-10', '100')).toBeFalsy();
        });

        it('невалидное значение - ноль', () => {
            expect(isAmountValid('0', '100')).toBeFalsy();
        });
    });
});
