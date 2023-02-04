import { ContractType } from 'common/constants/print-form-rules';

import { AttributeComparisionType } from '../../../constants';
import {
    buildNewBlock,
    buildNewElement,
    buildNewLink,
    buildNewRule,
    buildNewRuleAttributes
} from '../../factories';

export const CONTRACT_ATTRIBUTE_REFERENCE = {
    contexts: [
        {
            label: 'Начальное соглашение договора',
            id: 'col0',
            ruleTypes: ['contract', 'col']
        },
        {
            label: 'Дополнительное соглашение договора',
            id: 'col',
            ruleTypes: ['col']
        },
        {
            label: 'Текущие действующие условия договора',
            id: 'cs',
            ruleTypes: ['col']
        }
    ],
    attributeTypes: [
        {
            allowNot: false,
            allowMultiple: false,
            id: 'single'
        },
        {
            allowNot: false,
            allowMultiple: true,
            id: 'multiple'
        },
        {
            allowNot: true,
            allowMultiple: true,
            id: 'multiple_w_not'
        }
    ],
    attributes: [
        {
            contexts: ['col0', 'cs'],
            values: [
                {
                    id: '1',
                    label: 'ООО «Яндекс»'
                }
            ],
            type: 'single',
            id: 'FIRM',
            label: 'Фирма'
        }
    ],
    ruleTypes: [
        { id: 'contract', label: 'CONTRACT' },
        { id: 'col0', label: 'COL0' }
    ]
};

export const SIMPLE_RULE = buildNewRule(
    buildNewRuleAttributes(ContractType.GENERAL, CONTRACT_ATTRIBUTE_REFERENCE, {
        caption: 'Имя правила',
        externalId: 'Идентификатор правила'
    }),
    {
        blocks: [
            buildNewBlock({
                elements: [
                    buildNewElement({
                        attributeId: 'COUNTRY',
                        attributeComparisionType: AttributeComparisionType.SINGLE,
                        contextId: 'col0',
                        value: '10090'
                    })
                ]
            })
        ],
        links: [
            buildNewLink({
                name: 'template',
                value: '/template-link'
            })
        ]
    }
);

export const SIMPLE_RULE_BLOCKS = [
    buildNewBlock({
        elements: [
            buildNewElement({
                attributeId: 'COUNTRY',
                attributeComparisionType: AttributeComparisionType.SINGLE,
                contextId: 'col0',
                value: '35'
            }),
            buildNewElement({
                attributeId: 'FIRM',
                attributeComparisionType: AttributeComparisionType.MULTIPLE,
                contextId: 'col0',
                values: ['1', '111']
            }),
            buildNewElement({
                attributeId: 'SERVICES',
                attributeComparisionType: AttributeComparisionType.SINGLE_NOT,
                contextId: 'col0',
                value: '35'
            }),
            buildNewElement({
                attributeId: 'SERVICES',
                attributeComparisionType: AttributeComparisionType.SINGLE,
                contextId: 'col0',
                value: '7'
            })
        ]
    })
];

export const COMPLEX_RULE_BLOCKS = [
    buildNewBlock({
        elements: [
            buildNewElement({
                attributeId: 'FIRM',
                attributeComparisionType: AttributeComparisionType.MULTIPLE_NOT,
                contextId: 'col0',
                values: ['1', '111']
            }),
            buildNewElement({
                attributeId: 'SERVICES',
                attributeComparisionType: AttributeComparisionType.MULTIPLE,
                contextId: 'col0',
                values: ['2', '35']
            }),
            buildNewElement({
                attributeId: 'START_DATE',
                attributeComparisionType: AttributeComparisionType.SINGLE,
                contextId: 'col0',
                value: '7'
            })
        ]
    }),
    buildNewBlock({
        elements: [
            buildNewElement({
                attributeId: 'COUNTRY',
                attributeComparisionType: AttributeComparisionType.MULTIPLE,
                contextId: 'col0',
                values: ['35']
            }),
            buildNewElement({
                attributeId: 'PAYMENT_TYPE',
                attributeComparisionType: AttributeComparisionType.SINGLE_NOT,
                contextId: 'col0',
                value: '2'
            })
        ]
    })
];

export const INVALID_RULE_BLOCKS = [
    buildNewBlock({
        elements: [
            buildNewElement({
                attributeId: 'COUNTRY',
                contextId: 'col0',
                value: '35'
            })
        ]
    })
];
