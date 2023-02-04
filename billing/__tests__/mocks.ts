import { HOST } from 'common/utils/test-utils/common';

export const attributeReferenceGeneral = {
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
                    { content: null, id: '0', label: 'не задан' },
                    { content: null, id: '1', label: 'по сроку' },
                    { content: null, id: '2', label: 'по сроку и сумме' }
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

export const attributeReferenceDistribution = {
    request: {
        url: `${HOST}/contract/print-form-rules/attributes`,
        data: {
            ctype: 'DISTRIBUTION'
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
                    { content: null, id: '0', label: 'не задан' },
                    { content: null, id: '1', label: 'по сроку' },
                    { content: null, id: '2', label: 'по сроку и сумме' }
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

export const unpublishedPrintFormRule = {
    request: {
        url: `${HOST}/contract/print-form-rules/rule`,
        data: {
            external_id: 'someExternalId'
        }
    },
    response: {
        ruleType: 'contract',
        printForms: [
            {
                caption: 'Единый договор (Постоплата)',
                link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
            }
        ],
        ctype: 'GENERAL',
        content: {
            interleave: [{ value: '0', attr: 'CREDIT_TYPE', context: 'editable' }]
        },
        caption: 'edinyij dogovor postoplata',
        version: 1,
        branch: 'pre-prod',
        externalId: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
        isPublished: false
    }
};

export const publishedPrintFormRule = {
    request: {
        url: `${HOST}/contract/print-form-rules/rule`,
        data: {
            external_id: 'someExternalId'
        }
    },
    response: {
        ruleType: 'contract',
        printForms: [
            {
                caption: 'Единый договор (Постоплата)',
                link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
            }
        ],
        ctype: 'GENERAL',
        content: {
            interleave: [{ value: '0', attr: 'CREDIT_TYPE', context: 'editable' }]
        },
        caption: 'edinyij dogovor postoplata',
        version: 1,
        branch: 'pre-prod',
        externalId: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
        isPublished: true
    }
};

export const validateRuleWithIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/validate`,
        data: {
            external_id: 'someExternalId'
        }
    },
    response: {
        intersections: [
            { caption: 'test-test-test-1', externalId: 'test-test-test-3' },
            { caption: 'test-test-test-2', externalId: 'test-test-test-2' },
            { caption: 'test-test-test-3', externalId: 'test-test-test-1' }
        ]
    }
};

export const validateRuleWithoutIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/validate`,
        data: {
            external_id: 'someExternalId'
        }
    },
    response: {
        intersections: []
    }
};

export const publishRuleWithIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/publish`,
        data: {
            external_id: 'someExternalId',
            force: false
        },
        isJSON: true
    },
    response: {
        intersections: [
            { caption: 'test-test-test-1', externalId: 'test-test-test-3' },
            { caption: 'test-test-test-2', externalId: 'test-test-test-2' },
            { caption: 'test-test-test-3', externalId: 'test-test-test-1' }
        ]
    }
};

export const publishRuleWithForce = {
    request: {
        url: `${HOST}/contract/print-form-rules/publish`,
        data: {
            external_id: 'someExternalId',
            force: true
        },
        isJSON: true
    },
    response: {
        intersections: []
    }
};

export const publishRuleWithoutIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/publish`,
        data: {
            external_id: 'someExternalId',
            force: false
        },
        isJSON: true
    },
    response: {
        intersections: []
    }
};

export const createRuleWithIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/create`,
        data: {
            rule_params: {
                caption: 'edinyij dogovor postoplata',
                content: {
                    interleave: [
                        {
                            attr: 'CREDIT_TYPE',
                            context: 'editable',
                            value: '0'
                        }
                    ]
                },
                create: false,
                ctype: 'GENERAL',
                external_id: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
                print_forms: [
                    {
                        caption: 'Единый договор (Постоплата)',
                        link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
                    }
                ],
                rule_type: 'contract'
            }
        },
        isJSON: true
    },
    response: {
        intersections: [
            { caption: 'test-test-test-1', externalId: 'test-test-test-3' },
            { caption: 'test-test-test-2', externalId: 'test-test-test-2' },
            { caption: 'test-test-test-3', externalId: 'test-test-test-1' }
        ]
    }
};

export const createRuleWithoutIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/create`,
        data: {
            rule_params: {
                caption: 'edinyij dogovor postoplata',
                content: {
                    interleave: [
                        {
                            attr: 'CREDIT_TYPE',
                            context: 'editable',
                            value: '0'
                        }
                    ]
                },
                create: true,
                ctype: 'GENERAL',
                external_id: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
                print_forms: [
                    {
                        caption: 'Единый договор (Постоплата)',
                        link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
                    }
                ],
                rule_type: 'contract'
            }
        },
        isJSON: true
    },
    response: {
        intersections: []
    }
};

export const updateRuleWithoutIntersections = {
    request: {
        url: `${HOST}/contract/print-form-rules/create`,
        data: {
            rule_params: {
                caption: 'edinyij dogovor postoplata',
                content: {
                    interleave: [
                        {
                            attr: 'CREDIT_TYPE',
                            context: 'editable',
                            value: '0'
                        }
                    ]
                },
                create: false,
                ctype: 'GENERAL',
                external_id: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
                print_forms: [
                    {
                        caption: 'Единый договор (Постоплата)',
                        link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
                    }
                ],
                rule_type: 'contract'
            }
        },
        isJSON: true
    },
    response: {
        intersections: []
    }
};

export const createRuleWithForce = {
    request: {
        url: `${HOST}/contract/print-form-rules/create`,
        data: {
            rule_params: {
                caption: 'edinyij dogovor postoplata',
                content: {
                    interleave: [
                        {
                            attr: 'CREDIT_TYPE',
                            context: 'editable',
                            value: '0'
                        }
                    ]
                },
                ctype: 'GENERAL',
                force: true,
                create: false,
                external_id: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
                print_forms: [
                    {
                        caption: 'Единый договор (Постоплата)',
                        link: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
                    }
                ],
                rule_type: 'contract'
            }
        },
        isJSON: true
    },
    response: {
        intersections: []
    }
};
