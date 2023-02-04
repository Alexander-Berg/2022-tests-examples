import {
    creditInitialFee,
    creditPaymentFrom,
    maxInitialFee,
    priceFromFieldName,
    priceToFieldName,
} from '../creditFilterChange';

import { getValue } from './getValue';

describe('getValue', () => {
    it('вернет как есть, если maxAmount = 0', () => {
        const inputValue = 100;
        const result = getValue({
            fieldName: creditInitialFee,
            inputValue,
            loanTerm: 10,
            maxAmount: 0,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(inputValue);
    });

    it('creditInitialFee не дает сделать цену от > 10к', () => {
        const result = getValue({
            fieldName: creditInitialFee,
            inputValue: maxInitialFee + 100,
            loanTerm: 10,
            maxAmount: 10,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(maxInitialFee);
    });

    it('creditInitialFee возвращает цену как есть, если поле: creditInitialFee, и цена меньше maxInitialFee', () => {
        const inputValue = 100;
        const result = getValue({
            fieldName: creditInitialFee,
            inputValue: inputValue,
            loanTerm: 10,
            maxAmount: 10,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(inputValue);
    });

    it('priceFromFieldName не даем сделать цену ОТ больше максимальной суммы', () => {
        const maxAmount = 100;
        const result = getValue({
            fieldName: priceFromFieldName,
            inputValue: maxAmount + 10,
            loanTerm: 10,
            maxAmount,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(maxAmount);
    });

    it('priceFromFieldName возвращает цену как есть, если цена меньше максимальной суммы', () => {
        const inputValue = 100;
        const result = getValue({
            fieldName: priceFromFieldName,
            inputValue: inputValue,
            loanTerm: 10,
            maxAmount: inputValue + 100,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(inputValue);
    });

    it('priceToFieldName не даем сделать цену ДО больше 5кк + 10кк (макс. ВЗНОС)', () => {
        const maxAmount = 100;
        const result = getValue({
            fieldName: priceToFieldName,
            inputValue: maxAmount + maxInitialFee,
            loanTerm: 10,
            maxAmount,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(maxInitialFee + maxAmount);
    });

    it('priceToFieldName возвращает цену как есть, если цену ДО меньше 5кк + 10кк (макс. ВЗНОС)', () => {
        const inputValue = 100;
        const result = getValue({
            fieldName: priceToFieldName,
            inputValue: inputValue,
            loanTerm: 10,
            maxAmount: 100,
            minRate: 10,
            paymentSliderValues: [],
        });

        expect(result).toEqual(inputValue);
    });

    it('creditPaymentFrom вернет ближайшее значение из слайдера', () => {
        const result = getValue({
            fieldName: creditPaymentFrom,
            inputValue: 2020,
            loanTerm: 10,
            maxAmount: 100,
            minRate: 10,
            paymentSliderValues: [
                { value: 1 },
                { value: 2 },
            ],
        });

        expect(result).toEqual(2);
    });

    it('creditPaymentFrom возвращает цену как есть, если нет мин ставки', () => {
        const inputValue = 100;
        const result = getValue({
            fieldName: creditPaymentFrom,
            inputValue: inputValue,
            loanTerm: 10,
            maxAmount: 100,
            minRate: 0,
            paymentSliderValues: [],
        });

        expect(result).toEqual(inputValue);
    });
});
