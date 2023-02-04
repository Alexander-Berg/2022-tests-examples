const stateMock = require('www-cabinet/react/dataDomain/state/mocks/withNavigation.mock');

const getNavigationItems = require('./getNavigationItems');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');
const CUSTOMER_ROLES = require('www-cabinet/data/client/customer-roles');

const TEST_CASES = [
    { route: ROUTES.calculator, result: '/calculator/' },
    { route: ROUTES.feedsHistoryItem, params: { id: 123 }, result: '/feeds/' },
];

TEST_CASES.forEach((testCase) => {
    it(`должен проставлять активным элемент с url ${ testCase.result } при роуте ${ testCase.route }`, () => {
        const state = {
            state: stateMock,
            config: {
                routeName: testCase.route,
                routeParams: testCase.params,
            },
        };

        const result = getNavigationItems(state);

        const activeItems = result.filter((item) => item.active);

        expect(activeItems).toHaveLength(1);
        expect(activeItems[0].url).toBe(testCase.result);
    });
});

describe('роут sales, категория trucks', () => {
    it('добавит статус активен для элемента подменю и самого меню', () => {
        const state = {
            state: {
                ...stateMock,
                navigation: [
                    {
                        name: 'Объявления',
                        alias: 'sales',
                        host: 'cabinet',
                        submenu: [
                            {
                                name: 'Легковые новые',
                                url: '/sales/cars/new/',
                                host: 'cabinet',
                                cnt: 0,
                                amount: null,
                                unlim: false,
                                tag: 'cars_new',
                            },
                            {
                                name: 'Коммерческие',
                                url: '/sales/trucks/',
                                host: 'cabinet',
                                cnt: 0,
                                amount: null,
                                unlim: false,
                                tag: 'trucks',
                            },
                        ],
                    },
                ],
            },
            config: {
                routeName: 'sales',
                routeParams: {
                    category: 'trucks',
                },
            },
        };

        const result = getNavigationItems(state);

        const activeItems = result.filter((item) => item.active);

        expect(activeItems).toMatchSnapshot();
    });
});

it('должен проставлять активным и чилдрена и его родителя, если чилдрен активен', () => {
    const state = {
        state: stateMock,
        config: {
            routeName: ROUTES.sales,
            routeParams: { section: 'used', category: 'cars' },
        },
    };

    const result = getNavigationItems(state);

    const item = result.find((item) => item.alias === 'sales');

    expect(item).toMatchSnapshot();
});

it('должен обогащать ссылки client_id, если роль не клиента', () => {
    const state = {
        state: stateMock,
        config: {
            routeName: ROUTES.wallet,
            customerRole: CUSTOMER_ROLES.agency,
            client: { id: 123 },
        },
    };

    const result = getNavigationItems(state);

    expect(result.slice(0, 2)).toMatchSnapshot();
});
