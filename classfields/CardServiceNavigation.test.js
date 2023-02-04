const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CardServiceNavigation = require('./CardServiceNavigation');

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

const CARD_USED = {
    category: 'cars',
    vehicle_info: {
        mark_info: { code: 'AUDI' },
        model_info: { code: 'Q5' },
        super_gen: { id: '8351293' },
        configuration: { id: '8351305' },
        tech_param: { id: '8351307' },
    },
    id: '1082479594',
    hash: '63ee1b5b',
    section: 'used',
};

const CARD_NEW = {
    category: 'cars',
    vehicle_info: {
        mark_info: { code: 'VOLKSWAGEN' },
        model_info: { code: 'POLO' },
        super_gen: { id: '20113124' },
        configuration: { id: '20554752' },
        tech_param: { id: '20726508' },
    },
    id: '1081240978',
    hash: '9de38c5f',
    section: 'new',
};

const CARD_MOTO = {
    category: 'moto',
    sub_category: 'atv',
    vehicle_info: {
        mark_info: { code: 'BMW' },
        model_info: { code: '155' },
    },
    id: '1082479594',
    hash: '63ee1b5b',
    section: 'used',
};

const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

it('должен нормально отрендериться список табов на карточке used', () => {
    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'used',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    card={ CARD_USED }
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке new', () => {
    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'new',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    card={ CARD_NEW }
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке группы', () => {
    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'new',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    cardGroupInfo={{
                        mark: { code: 'AUDI' },
                        model: { code: 'A4' },
                        configuration: { id: '111' },
                        generation: { id: '222' },
                    }}
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке used moto', () => {
    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'used',
    };

    const tree = shallow(
        <ContextProvider>
            <CardServiceNavigation
                card={ CARD_MOTO }
                pageParams={ pageParams }
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке used (с кредитами)', () => {
    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-16575_credit_menu_exp');

    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'used',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    card={ CARD_USED }
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке new (с кредитами)', () => {
    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-16575_credit_menu_exp');

    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'new',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    card={ CARD_NEW }
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке группы (с кредитами)', () => {
    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-16575_credit_menu_exp');

    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'new',
    };

    const tree = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <CardServiceNavigation
                    cardGroupInfo={{
                        mark: { code: 'AUDI' },
                        model: { code: 'A4' },
                        configuration: { id: '111' },
                        generation: { id: '222' },
                    }}
                    pageParams={ pageParams }
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен нормально отрендериться список табов на карточке used moto (с кредитами)', () => {
    contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-16575_credit_menu_exp');

    const ContextProvider = createContextProvider(contextMock);
    const pageParams = {
        category: 'cars',
        section: 'used',
    };

    const tree = shallow(
        <ContextProvider>
            <CardServiceNavigation
                card={ CARD_MOTO }
                pageParams={ pageParams }
            />
        </ContextProvider>,
    ).dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});
