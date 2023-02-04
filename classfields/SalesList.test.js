/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const SalesList = require('www-cabinet/react/components/SalesList/SalesList');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const { Provider } = require('react-redux');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const Context = createContextProvider(contextMock);

it('Должен вызвать hideGroupActions в момент загрузки списка объявлений', () => {
    const store = mockStore({
        autostrategy: {},
        bunker: {},
        badgesSettings: {},
        cookies: {},
        config: {
            customerRole: 'client',
            client: {
                id: 16453,
            },
        },
        sales: {
            items: [],
        },
    });
    const hideGroupActions = jest.fn();
    const tree = shallow(
        <Provider store={ store }>
            <Context>
                <SalesList
                    checkedSalesIds={{}}
                    hideGroupActions={ hideGroupActions }
                    pager={{}}
                />
            </Context>
        </Provider>,
    );

    store.dispatch(tree.dive().dive().dive().dive().find('Listing').props().loadAction());
    expect(hideGroupActions).toHaveBeenCalled();
});
