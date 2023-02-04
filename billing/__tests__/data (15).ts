import { HOST } from 'common/utils/test-utils/common';

export const search = '?contract_type=SPENDABLE&rule_type=col&is_published=true&pn=1&ps=10';

export const attributeReference = {
    request: {
        url: `${HOST}/contract/print-form-rules/attributes`,
        data: {
            ctype: 'GENERAL'
        }
    },
    response: {
        contexts: [
            { isMacro: true, ruleTypes: ['contract', 'col'], id: 'macro', label: 'Макросы' },
            {
                isMacro: false,
                ruleTypes: ['contract', 'col'],
                id: 'editable',
                label: 'Текущие действующие условия договора'
            },
            {
                isMacro: false,
                ruleTypes: ['col'],
                id: 'col',
                label: 'Дополнительное соглашение договора'
            },
            {
                isMacro: false,
                ruleTypes: ['contract', 'col'],
                id: 'col0',
                label: 'Начальное соглашение договора'
            }
        ],
        attributeTypes: [
            { allowNot: 'false', allowMultiple: 'false', id: 'single' },
            { allowNot: 'false', allowMultiple: 'true', id: 'multiple' },
            { allowNot: 'true', allowMultiple: 'true', id: 'multiple_w_not' }
        ],
        ruleTypes: [
            { id: 'col', label: 'Для ДС' },
            { id: 'contract', label: 'Для договора' }
        ],
        attributes: [
            {
                contexts: ['col0', 'editable'],
                values: [
                    { content: null, id: 0, label: 'не задан' },
                    { content: null, id: 1, label: 'по сроку' },
                    { content: null, id: 2, label: 'по сроку и сумме' }
                ],
                type: 'single',
                id: 'CREDIT_TYPE',
                label: 'Вид кредита'
            }
        ],
        macros: [
            {
                contexts: ['macro'],
                id: 'multiple_unified_services',
                label: 'Несколько единых сервисов'
            }
        ]
    }
};

export const printFormRules = {
    request: {
        url: `${HOST}/contract/print-form-rules/list`,
        data: {
            ctype: 'GENERAL',
            pn: 1,
            ps: 10
        }
    },
    response: {
        totalCount: 2,
        items: [
            {
                ruleType: 'contract',
                printForms: [
                    {
                        caption: 'Единый договор (Постоплата)',
                        link:
                            '/sales/processing/billing-agreements/bju-jandeks-taksi/usluga-korporativnoe-taksi/klientskaja-sxema/edinyjj-dogovor/'
                    }
                ],
                ctype: 'GENERAL',
                content: {
                    interleave: [
                        { value: 0, attr: 'COMMISSION', context: 'editable' },
                        { value: 13, attr: 'FIRM', context: 'editable' },
                        { value: 3, attr: 'PAYMENT_TYPE', context: 'editable' },
                        { attr: 'multiple_unified_services', context: 'macro' }
                    ]
                },
                caption: 'edinyij dogovor postoplata',
                version: 1,
                branch: 'pre-prod',
                externalId: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
                isPublished: true
            },
            {
                ruleType: 'contract',
                printForms: [
                    {
                        caption: 'Единый договор (Предоплата)',
                        link:
                            '/sales/processing/billing-agreements/bju-jandeks-taksi/usluga-korporativnoe-taksi/klientskaja-sxema/19/'
                    }
                ],
                ctype: 'GENERAL',
                content: {
                    interleave: [
                        { value: 0, attr: 'COMMISSION', context: 'editable' },
                        { value: 13, attr: 'FIRM', context: 'editable' },
                        { value: 2, attr: 'PAYMENT_TYPE', context: 'editable' },
                        { attr: 'multiple_unified_services', context: 'macro' }
                    ]
                },
                caption: 'edinyij dogovor predoplata',
                version: 1,
                branch: 'pre-prod',
                externalId: '__automatic_rtrunk_tplno956_pfno963_GENERAL',
                isPublished: true
            }
        ]
    }
};
