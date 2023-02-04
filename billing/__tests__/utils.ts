import { Rule, RuleElement, RuleLink } from '../../../types/domain';

export function extractRuleElementData(ruleElement: RuleElement) {
    return {
        context: ruleElement.contextId,
        attribute: ruleElement.attributeId,
        attributeComparisionType: ruleElement.attributeComparisionType,
        value: ruleElement.value,
        values: ruleElement.values
    };
}

export function extractRuleAttributesData(rule: Rule) {
    return {
        caption: rule.attributes.caption,
        externalId: rule.attributes.externalId,
        type: rule.attributes.typeId,
        contractType: rule.attributes.contractType
    };
}

export function extractRuleLinkData(ruleLink: RuleLink) {
    return {
        name: ruleLink.name,
        value: ruleLink.value
    };
}
