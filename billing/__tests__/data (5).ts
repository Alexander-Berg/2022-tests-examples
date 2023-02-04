import { HOST } from 'common/utils/test-utils/common';

export const contracts = {
    request: {
        url: `${HOST}/contract/client-credit/limit`,
        data: {
            client_id: 123
        }
    },
    response: [
        {
            contractCreditInvoiceType: 8,
            contractDisabled: false,
            contract: {
                currency: 'RUR',
                externalId: '32400/15',
                id: 202987
            },
            contractClientsCreditLimit: [],
            contractCreditLimit: [
                {
                    limit: 6146533,
                    id: 0,
                    spent: 4432292
                }
            ],
            contract_personal_accountInvoiceId: 39073021
        }
    ]
};

export const activityTypes = {
    request: {
        url: `${HOST}/client/activity-types`
    },
    response: [
        {
            parentId: null,
            hidden: true,
            id: 1,
            name: 'name-1'
        },
        {
            parentId: 1,
            hidden: true,
            id: 4,
            name: 'name-2'
        }
    ]
};

export const restrictions = {
    request: {
        url: `${HOST}/client/available-credits`,
        data: {
            client_id: 123
        }
    },
    response: [
        {
            delayExceededDays: 7,
            nearlyExceeded: true,
            nearlyExceededInvoices: [
                {
                    dt: '2020-03-31T00:00:00',
                    externalId: '2709344595-1',
                    id: 112312580
                },
                {
                    dt: '2020-03-31T00:00:00',
                    externalId: '2709344586-1',
                    id: 112312578
                }
            ],
            hasLimits: true,
            exceeded: true,
            externalId: '32400/15',
            id: 202987,
            exceededInvoices: [
                {
                    dt: '2020-03-31T00:00:00',
                    externalId: '2709344595-1',
                    id: 112312580
                },
                {
                    dt: '2020-03-31T00:00:00',
                    externalId: '2709344586-1',
                    id: 112312578
                }
            ]
        }
    ]
};
