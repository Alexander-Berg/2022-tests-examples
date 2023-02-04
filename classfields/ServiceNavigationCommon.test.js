const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const IndexServiceNavigation = require('./ServiceNavigationCommon');

const PAGE_PARAMS = {
    category: 'cars',
    section: 'all',
};

const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

it('должен нормально отрендериться список табов на морде', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <IndexServiceNavigation
                    pageParams={ PAGE_PARAMS }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на морде (для экспа с кредитами)', () => {
    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-16575_credit_menu_exp');

    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <IndexServiceNavigation
                    pageParams={ PAGE_PARAMS }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});
