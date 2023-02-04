const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const OrdersDumb = require('./OrdersDumb');

const ROUTES = require('auto-core/router/cabinet.auto.ru/route-names');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

it('должен рендерить контент с навигацией, если разделов с правами больше 1', () => {
    const tree = shallow(
        <ContextProvider>
            <OrdersDumb
                isLoading={ false }
                routeName={ ROUTES.matchApplications }
                canReadMatchApplications={ true }
                canReadTradeIn={ true }
            >
                Контент
            </OrdersDumb>
        </ContextProvider>,
    ).dive();

    const ordersContent = tree.children();
    expect(shallowToJson(ordersContent)).toMatchSnapshot();
});

it('должен рендерить контент без навигации, если доступен только один элемент навигации', () => {
    const tree = shallow(
        <ContextProvider>
            <OrdersDumb
                isLoading={ false }
                routeName={ ROUTES.booking }
                canReadMatchApplications={ false }
                canReadTradeIn={ false }
            >
                Контент
            </OrdersDumb>
        </ContextProvider>,
    ).dive();

    const ordersContent = tree.children();
    expect(shallowToJson(ordersContent)).toMatchSnapshot();
});
