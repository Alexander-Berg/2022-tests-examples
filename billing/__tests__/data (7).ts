import { Permissions } from 'common/constants';

export const perms = Object.values(Permissions);

export const initialData = {
    data: [
        {
            discount: {
                classname: 'BelMarketAgencyDiscount',
                next_budget: null,
                name: 'bel_market_discount',
                currency: 'RUR',
                budget: null,
                budget_discount: null,
                discount: '12.00',
                next_discount: null,
                dt: '2020-02-17T20:35:55.507528',
                type: 'fixed',
                without_taxes: true
            },
            contract: {
                update_dt: '2020-02-14T13:13:45',
                type: 'GENERAL',
                id: 1367198,
                client_id: 111882400,
                person_id: 10557232,
                external_id: '533874/17',
                passport_id: 16571028
            }
        },
        {
            discount: {
                classname: 'Bel2012FixedDiscount',
                next_budget: null,
                name: 'bel_2012_fixed_agency_discount',
                currency: 'RUR',
                budget: null,
                budget_discount: null,
                discount: '12.00',
                next_discount: null,
                dt: '2020-02-17T20:35:55.507528',
                type: 'fixed',
                without_taxes: true
            },
            contract: {
                update_dt: '2020-02-14T13:13:07',
                type: 'GENERAL',
                id: 1367176,
                client_id: 111882400,
                person_id: 10557232,
                external_id: '533852/17',
                passport_id: 16571028
            }
        }
    ]
};
