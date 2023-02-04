const isAlwaysVisibleSearchOfferForm = require('./isAlwaysVisibleSearchOfferForm');

const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');
const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const TEST_CASES = [
    { customerRole: CUSTOMER_ROLES.client, result: false },
    { customerRole: CUSTOMER_ROLES.agency, result: true },
    { customerRole: CUSTOMER_ROLES.company, result: true },
    { customerRole: CUSTOMER_ROLES.manager, result: true },
];

TEST_CASES.forEach((testCase) => {
    it(`должен отдавать ${ testCase.result } для роли ${ testCase.customerRole } на роуте sales`, () => {
        const state = {
            config: {
                customerRole: testCase.customerRole,
                routeName: ROUTES.sales,
            },
        };

        const result = isAlwaysVisibleSearchOfferForm(state);

        expect(result).toBe(testCase.result);
    });
});

it('не должен отдавать true на любом другом роуте', () => {
    const state = {
        config: {
            customerRole: CUSTOMER_ROLES.company,
            routeName: ROUTES.wallet,
        },
    };

    const result = isAlwaysVisibleSearchOfferForm(state);

    expect(result).toBe(false);
});
