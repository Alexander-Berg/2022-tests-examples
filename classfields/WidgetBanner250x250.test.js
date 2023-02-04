const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const WidgetBanner250x250 = require('./WidgetBanner250x250');

const store = mockStore({
    widgetBanner: {
        offers: [],
        showMoreParams: {},
    },
});

it('не должен ничего рендерить, если нет офферов', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <WidgetBanner250x250/>
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(tree).toBeEmptyRender();
});
