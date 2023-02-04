import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mock';

import { selector } from './getCreditInitialData';

/**
 * @todo эти снапшоты нужно заменить на нормальные тесты.
 */
describe('getCreditInitialData', () => {
    it('нет данных, ничего не возвращает', () => {
        expect(selector({})).toBeUndefined();
        expect(selector({})).toBeUndefined();
    });

    it('есть данные, возвращает данные формы', () => {
        const searchParams = { on_credit: true, price_to: 6000000 };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('угадываем цену ДО по платежу (если есть) или берем минимальный amount и угадываем взнос при отсутствии параметров', () => {
        const searchParams = { on_credit: true };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('угадываем цену ДО по платежу (если есть) или берем минимальный amount + ВЗНОС 2', () => {
        const searchParams = { on_credit: true, credit_payment_to: 100, credit_payment_from: 200 };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('угадываем цену ДО по платежу (если есть) или берем минимальный amount + ВЗНОС 3', () => {
        const searchParams = { on_credit: true, credit_payment_to: 300, credit_payment_from: 200 };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('угадываем цену ОТ по платежу (если есть) или берем минимальный amount 1', () => {
        const searchParams = { on_credit: true, credit_payment_to: 100, credit_payment_from: 200 };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('угадываем цену ОТ по платежу (если есть) или берем минимальный amount 2', () => {
        const searchParams = { on_credit: true, credit_payment_to: 300, credit_payment_from: 200 };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('если что-то пошло не так выкидываем цену ДО', () => {
        const searchParams = {
            on_credit: true,
            credit_payment_to: 300,
            credit_payment_from: 200,
            credit_initial_fee: 400,
        };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });

    it('если кредит < цены ОТ, значит меняем местами платеж', () => {
        const searchParams = {
            on_credit: true,
            credit_payment_to: 300,
            credit_payment_from: 200,
            credit_initial_fee: 200,

        };
        const result = selector(searchParams, creditProductMock);

        result && (result.paymentSliderValues = []);

        expect(result).toMatchSnapshot();
    });
});
