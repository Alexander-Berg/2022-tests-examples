export const BACKEND_RULE_SIMPLE = {
    printForms: [{ caption: 'template', link: '/template-link' }],
    caption: 'Имя правила',
    externalId: 'Идентификатор правила',
    ruleType: 'contract',
    ctype: 'GENERAL',
    isPublished: false,
    content: {
        interleave: [
            {
                value: '10090',
                attr: 'COUNTRY',
                context: 'col0'
            },
            {
                value: '2',
                attr: 'PAYMENT_TYPE',
                context: 'col0'
            }
        ]
    }
};

export const BACKEND_ITEM_SIMPLE = {
    interleave: [
        {
            value: '10090',
            attr: 'COUNTRY',
            context: 'col0'
        },
        {
            value: '2',
            attr: 'PAYMENT_TYPE',
            context: 'col0'
        },
        {
            alternation: [
                {
                    value: '1',
                    attr: 'FIRM',
                    context: 'col0'
                },
                {
                    value: '111',
                    attr: 'FIRM',
                    context: 'col0'
                }
            ]
        },
        {
            interleave: [
                {
                    not_: {
                        value: '35',
                        attr: 'SERVICES',
                        context: 'col0'
                    }
                },
                {
                    value: '7',
                    attr: 'SERVICES',
                    context: 'col0'
                }
            ]
        }
    ]
};

export const BACKEND_ITEM_COMPLEX = {
    alternation: [
        {
            interleave: [
                {
                    not_: {
                        value: '7',
                        attr: 'SERVICES',
                        context: 'col0'
                    }
                }
            ]
        },
        {
            interleave: [
                {
                    value: '10090',
                    attr: 'COUNTRY',
                    context: 'col0'
                },
                {
                    alternation: [
                        {
                            value: '1',
                            attr: 'FIRM',
                            context: 'col0'
                        },
                        {
                            value: '111',
                            attr: 'FIRM',
                            context: 'col0'
                        }
                    ]
                },
                {
                    interleave: [
                        {
                            not_: {
                                interleave: [
                                    {
                                        value: '1',
                                        attr: 'SERVICES',
                                        context: 'col0'
                                    },
                                    {
                                        value: '111',
                                        attr: 'SERVICES',
                                        context: 'col0'
                                    }
                                ]
                            }
                        },
                        {
                            value: '7',
                            attr: 'SERVICES',
                            context: 'col0'
                        },
                        {
                            alternation: [
                                {
                                    value: '1',
                                    attr: 'START_DATE',
                                    context: 'col0'
                                },
                                {
                                    value: '2',
                                    attr: 'START_DATE',
                                    context: 'col0'
                                }
                            ]
                        }
                    ]
                }
            ]
        }
    ]
};

export const BACKEND_ITEM_ERROR_1 = {
    not_: {
        value: '10090',
        attr: 'COUNTRY',
        context: 'col0'
    }
};

export const BACKEND_ITEM_ERROR_2 = {
    alternation: [
        {
            value: '10090',
            attr: 'COUNTRY',
            context: 'col0'
        }
    ]
};

export const BACKEND_ITEM_ERROR_3 = {
    interleave: [
        {
            alternation: [
                {
                    not_: {
                        value: '10090',
                        attr: 'COUNTRY',
                        context: 'col0'
                    }
                }
            ]
        }
    ]
};

export const BACKEND_ITEM_ERROR_4 = {
    interleave: [
        {
            alternation: [
                {
                    value: '10090',
                    attr: 'COUNTRY',
                    context: 'col0'
                },
                {
                    value: '100',
                    attr: 'START_DATE',
                    context: 'col0'
                }
            ]
        }
    ]
};

export const BACKEND_ITEM_ERROR_5 = {
    interleave: [
        {
            not_: {
                interleave: [
                    {
                        not_: {
                            value: '10090',
                            attr: 'COUNTRY',
                            context: 'col0'
                        }
                    },
                    {
                        value: '100',
                        attr: 'START_DATE',
                        context: 'col0'
                    }
                ]
            }
        }
    ]
};

export const BACKEND_ITEM_ERROR_6 = {
    interleave: [
        {
            not_: {
                interleave: [
                    {
                        value: '10090',
                        attr: 'COUNTRY',
                        context: 'col0'
                    },
                    {
                        value: '100',
                        attr: 'START_DATE',
                        context: 'col0'
                    }
                ]
            }
        }
    ]
};

export const BACKEND_ITEM_ERROR_7 = {
    interleave: [
        {
            not_: {
                alternation: [
                    {
                        value: '10090',
                        attr: 'COUNTRY',
                        context: 'col0'
                    },
                    {
                        value: '100',
                        attr: 'START_DATE',
                        context: 'col0'
                    }
                ]
            }
        }
    ]
};
