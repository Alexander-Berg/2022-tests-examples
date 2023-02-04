import { RuleBlock } from '../../../types/domain';
import { parseRuleBlocks } from '../blocks';
import {
    BACKEND_ITEM_SIMPLE,
    BACKEND_ITEM_COMPLEX,
    BACKEND_ITEM_ERROR_1,
    BACKEND_ITEM_ERROR_2,
    BACKEND_ITEM_ERROR_3,
    BACKEND_ITEM_ERROR_4,
    BACKEND_ITEM_ERROR_5,
    BACKEND_ITEM_ERROR_6,
    BACKEND_ITEM_ERROR_7
} from './data';
import { extractRuleElementData } from './utils';

describe('parsing - item', () => {
    test('успешный разбор простого правила', () => {
        const ruleBlocks: RuleBlock[] = parseRuleBlocks(BACKEND_ITEM_SIMPLE);

        expect(ruleBlocks.length).toBe(1);

        const ruleElements = ruleBlocks[0].elements;

        expect(ruleElements.length).toBe(5);

        expect(extractRuleElementData(ruleElements[0])).toEqual({
            context: 'col0',
            attribute: 'COUNTRY',
            attributeComparisionType: 'single',
            value: '10090'
        });

        expect(extractRuleElementData(ruleElements[1])).toEqual({
            context: 'col0',
            attribute: 'PAYMENT_TYPE',
            attributeComparisionType: 'single',
            value: '2'
        });

        expect(extractRuleElementData(ruleElements[2])).toEqual({
            context: 'col0',
            attribute: 'FIRM',
            attributeComparisionType: 'multiple',
            values: ['1', '111']
        });

        expect(extractRuleElementData(ruleElements[3])).toEqual({
            context: 'col0',
            attribute: 'SERVICES',
            attributeComparisionType: 'single_not',
            value: '35'
        });

        expect(extractRuleElementData(ruleElements[4])).toEqual({
            context: 'col0',
            attribute: 'SERVICES',
            attributeComparisionType: 'single',
            value: '7'
        });
    });

    test('успешный разбор сложного правила', () => {
        const ruleBlocks: RuleBlock[] = parseRuleBlocks(BACKEND_ITEM_COMPLEX);

        expect(ruleBlocks.length).toBe(2);

        const ruleElements1 = ruleBlocks[0].elements;

        expect(ruleElements1.length).toBe(1);

        expect(extractRuleElementData(ruleElements1[0])).toEqual({
            context: 'col0',
            attribute: 'SERVICES',
            attributeComparisionType: 'single_not',
            value: '7'
        });

        const ruleElements2 = ruleBlocks[1].elements;

        expect(ruleElements2.length).toBe(5);

        expect(extractRuleElementData(ruleElements2[0])).toEqual({
            context: 'col0',
            attribute: 'COUNTRY',
            attributeComparisionType: 'single',
            value: '10090'
        });

        expect(extractRuleElementData(ruleElements2[1])).toEqual({
            context: 'col0',
            attribute: 'FIRM',
            attributeComparisionType: 'multiple',
            values: ['1', '111']
        });

        expect(extractRuleElementData(ruleElements2[2])).toEqual({
            context: 'col0',
            attribute: 'SERVICES',
            attributeComparisionType: 'multiple_not',
            values: ['1', '111']
        });

        expect(extractRuleElementData(ruleElements2[3])).toEqual({
            context: 'col0',
            attribute: 'SERVICES',
            attributeComparisionType: 'single',
            value: '7'
        });

        expect(extractRuleElementData(ruleElements2[4])).toEqual({
            context: 'col0',
            attribute: 'START_DATE',
            attributeComparisionType: 'multiple',
            values: ['1', '2']
        });
    });

    test('ошибка разбора правила - Некорректная структура правила', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_1 as any);
        }).toThrowError('Некорректная структура правила');
    });

    test('ошибка разбора правила - Неподдерживаемый формат блока', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_2 as any);
        }).toThrowError('Неподдерживаемый формат блока');
    });

    test('ошибка разбора правила - Неподдерживаемое перечисление внутри элемента', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_3 as any);
        }).toThrowError('Неподдерживаемое перечисление внутри элемента');
    });

    test('ошибка разбора правила - Все значения в перечислении должны относиться к одному атрибуту', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_4 as any);
        }).toThrowError('Все значения в перечислении должны относиться к одному атрибуту');
    });

    test('ошибка разбора правила - Внутри отрицательного пересечения допустимы только элементы', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_5 as any);
        }).toThrowError('Внутри отрицательного пересечения допустимы только элементы');
    });

    test('ошибка разбора правила - Все значения в отрицательном пересечении должны относиться к одному атрибуту', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_6 as any);
        }).toThrowError(
            'Все значения в отрицательном пересечении должны относиться к одному атрибуту'
        );
    });

    test('ошибка разбора правила - Отрицательное перечисление недопустимо', () => {
        expect(() => {
            parseRuleBlocks(BACKEND_ITEM_ERROR_7 as any);
        }).toThrowError('Отрицательное перечисление недопустимо');
    });
});
