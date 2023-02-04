export const list = {
    data: {
        client: { single_account_number: 105964 },
        invoices: [
            {
                amount: '4198.00',
                amount_nds: '699.67',
                amount_nsp: '0.00',
                contract: {
                    dt: '2019-07-16',
                    external_id: 'test/test',
                    id: 10032136
                },
                currency: 'RUB',
                dt: '2019-09-20',
                external_id: '\u0411-1920416898-1',
                id: 101034482,
                person: { id: 71231798, name: null }
            },
            {
                amount: '1234.00',
                amount_nds: '205.67',
                amount_nsp: '0.00',
                contract: {
                    dt: '2019-07-16',
                    external_id: 'test/test',
                    id: 10032136
                },
                currency: 'RUB',
                dt: '2019-09-20',
                external_id: '\u0411-1920416895-1',
                id: 101034481,
                person: { id: 71231798, name: null }
            }
        ],
        invoices_count: 2,
        item_count: 1,
        items: [
            {
                client_id: 109457871,
                dt: '2019-09-23T16:43:55',
                id: 306,
                order_id: 1526859726,
                qty: '10.00',
                service_id: 7,
                service_order_id: 298647084,
                title:
                    '\u0423\u0441\u043b\u0443\u0433\u0438 \u00ab\u042f\u043d\u0434\u0435\u043a\u0441.\u0414\u0438\u0440\u0435\u043a\u0442\u00bb',
                units_name: '\u0440\u0443\u0431.'
            }
        ]
    },
    version: { butils: '2.164', muzzle: 'UNKNOWN', snout: '1.0.261' }
};

export const checkRequest = {
    version: { snout: '1.0.261', muzzle: 'UNKNOWN', butils: '2.164' },
    data: { available_payment: true }
};

export const createRequest = {
    version: { snout: '1.0.261', muzzle: 'UNKNOWN', butils: '2.164' },
    data: { user_path: 'путь' }
};
