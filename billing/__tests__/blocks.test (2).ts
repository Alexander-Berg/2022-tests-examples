import { serializeRuleBlocks } from '../blocks';
import { SIMPLE_RULE_BLOCKS, COMPLEX_RULE_BLOCKS, INVALID_RULE_BLOCKS } from './data';

describe('serialization - blocks', () => {
    test('успешная сериализация простого правила', () => {
        expect(serializeRuleBlocks(SIMPLE_RULE_BLOCKS)).toEqual({
            interleave: [
                {
                    attr: 'COUNTRY',
                    context: 'col0',
                    value: '35'
                },
                {
                    alternation: [
                        {
                            attr: 'FIRM',
                            context: 'col0',
                            value: '1'
                        },
                        {
                            attr: 'FIRM',
                            context: 'col0',
                            value: '111'
                        }
                    ]
                },
                {
                    not_: {
                        attr: 'SERVICES',
                        context: 'col0',
                        value: '35'
                    }
                },
                {
                    attr: 'SERVICES',
                    context: 'col0',
                    value: '7'
                }
            ]
        });
    });

    test('успешная сериализация сложного правила', () => {
        expect(serializeRuleBlocks(COMPLEX_RULE_BLOCKS)).toEqual({
            alternation: [
                {
                    interleave: [
                        {
                            not_: {
                                alternation: [
                                    {
                                        attr: 'FIRM',
                                        context: 'col0',
                                        value: '1'
                                    },
                                    {
                                        attr: 'FIRM',
                                        context: 'col0',
                                        value: '111'
                                    }
                                ]
                            }
                        },
                        {
                            alternation: [
                                {
                                    attr: 'SERVICES',
                                    context: 'col0',
                                    value: '2'
                                },
                                {
                                    attr: 'SERVICES',
                                    context: 'col0',
                                    value: '35'
                                }
                            ]
                        },
                        {
                            attr: 'START_DATE',
                            context: 'col0',
                            value: '7'
                        }
                    ]
                },
                {
                    interleave: [
                        {
                            alternation: [
                                {
                                    attr: 'COUNTRY',
                                    context: 'col0',
                                    value: '35'
                                }
                            ]
                        },
                        {
                            not_: {
                                attr: 'PAYMENT_TYPE',
                                context: 'col0',
                                value: '2'
                            }
                        }
                    ]
                }
            ]
        });
    });

    test('ошибка сериализации правила - Элемент должен содержать корректный тип сравнения', () => {
        expect(() => {
            serializeRuleBlocks(INVALID_RULE_BLOCKS);
        }).toThrowError('Элемент должен содержать корректный тип сравнения');
    });
});
