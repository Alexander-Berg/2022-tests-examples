import { parseDiscounts } from '../discounts';

describe('Test parseDiscounts', () => {
    test('parseDiscounts', () => {
        const couples = [
            {
                discount: {
                    classname: 'BelMarketAgencyDiscount',
                    name: 'bel_market_discount',
                    currency: 'RUR',
                    budget: null,
                    discount: 12,
                    dt: '2020-02-17T20:35:55.507528',
                    type: 'fixed',
                    nextBudget: null,
                    budgetDiscount: null,
                    nextDiscount: null,
                    withoutTaxes: true
                },
                contract: {
                    type: 'GENERAL',
                    id: 1367198,
                    updateDt: '2020-02-14T13:13:45',
                    clientId: 111882400,
                    personId: 10557232,
                    externalId: '533874/17',
                    passportId: 16571028
                }
            },
            {
                discount: {
                    classname: 'Bel2012FixedDiscount',
                    name: 'bel_2012_fixed_agency_discount',
                    currency: 'RUR',
                    budget: 100,
                    discount: 12,
                    dt: '2020-02-17T20:35:55.507528',
                    type: 'fixed',
                    nextBudget: null,
                    budgetDiscount: null,
                    nextDiscount: null,
                    withoutTaxes: true
                },
                contract: {
                    type: 'GENERAL',
                    id: 1367176,
                    updateDt: '2020-02-14T13:13:07',
                    clientId: 111882400,
                    personId: 10557232,
                    externalId: '533852/17',
                    passportId: 16571028
                }
            }
        ];

        const expected = [
            {
                budget: null,
                name: 'bel_market_discount',
                currency: 'RUR',
                discount: 12,
                nextDiscount: null,
                budgetDiscount: null,
                nextBudget: null,
                contractExternalId: '533874/17',
                contractId: 1367198
            },
            {
                budget: 100,
                name: 'bel_2012_fixed_agency_discount',
                currency: 'RUR',
                discount: 12,
                nextDiscount: null,
                budgetDiscount: null,
                nextBudget: null,
                contractExternalId: '533852/17',
                contractId: 1367176
            }
        ];

        expect(parseDiscounts(couples)).toEqual(expected);
    });
});
