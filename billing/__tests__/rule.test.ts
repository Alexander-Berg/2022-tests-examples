import { Rule } from '../../../types/domain';
import { parseRule } from '../rule';
import { BACKEND_RULE_SIMPLE } from './data';
import { extractRuleAttributesData, extractRuleElementData, extractRuleLinkData } from './utils';

describe('parsing - rule', () => {
    test('успешный разбор простого правила', () => {
        const rule: Rule = parseRule(BACKEND_RULE_SIMPLE);

        expect(extractRuleAttributesData(rule)).toEqual({
            caption: 'Имя правила',
            externalId: 'Идентификатор правила',
            type: 'contract',
            contractType: 'GENERAL'
        });

        expect(rule.blocks.length).toBe(1);

        expect(rule.blocks[0].elements.length).toBe(2);

        expect(extractRuleElementData(rule.blocks[0].elements[0])).toEqual({
            context: 'col0',
            attribute: 'COUNTRY',
            attributeComparisionType: 'single',
            value: '10090'
        });

        expect(extractRuleElementData(rule.blocks[0].elements[1])).toEqual({
            context: 'col0',
            attribute: 'PAYMENT_TYPE',
            attributeComparisionType: 'single',
            value: '2'
        });

        expect(rule.links.length).toBe(1);

        expect(extractRuleLinkData(rule.links[0])).toEqual({
            name: 'template',
            value: '/template-link'
        });
    });
});
