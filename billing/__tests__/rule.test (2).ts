import { serializeRule } from '../rule';
import { SIMPLE_RULE } from './data';

describe('serialization - rule', () => {
    test('успешная сериализация простого правила', () => {
        expect(serializeRule(SIMPLE_RULE)).toEqual({
            printForms: [{ caption: 'template', link: '/template-link' }],
            caption: 'Имя правила',
            externalId: 'Идентификатор правила',
            ruleType: 'contract',
            ctype: 'GENERAL',
            content: {
                interleave: [
                    {
                        value: '10090',
                        attr: 'COUNTRY',
                        context: 'col0'
                    }
                ]
            }
        });
    });
});
