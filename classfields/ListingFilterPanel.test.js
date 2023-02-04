const React = require('react');
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingFilterPanel = require('./ListingFilterPanel');

const defaultState = {
    geo: {
        gids: [],
    },
};

const defaultProps = {
    category: 'cars',
    onFilterChange: () => {},
    section: 'used',
    sort: 'fresh_relevance_1-desc',
    sorts: [
        { text: 'По актуальности', val: 'fresh_relevance_1-desc', 'default': true },
        { text: 'По дате', val: 'cr_date-desc' },
        { val: 'price_profitability-desc', category: 'cars', text: 'По оценке стоимости' },
    ],
};

it('сортировка "По оценке стоимости"', () => {
    const tree = shallowRenderComponent(
        defaultProps,
        defaultState,
        contextMock,
    );

    expect(tree.find('SortOffersFilter').prop('items').map((item) => item.val)).toContain('price_profitability-desc');
});

it('вид листинга по умолчанию в б/у', () => {
    const props = {
        category: 'cars',
        onFilterChange: () => {},
        section: 'used',
        sorts: [],
    };

    const tree = shallowRenderComponent(
        props,
        defaultState,
        contextMock,
    );

    expect(tree.find('Connect(ListingOutputTypeSwitcher)').prop('defaultType')).toBe('list');
});

it('вид листинга по умолчанию в б/у в экспе', () => {
    const props = {
        category: 'cars',
        onFilterChange: () => {},
        section: 'used',
        sorts: [],
    };

    const context = {
        ...contextMock,
        hasExperiment: (exp) => exp === 'AUTORUFRONT-19853_carousel',
    };

    const tree = shallowRenderComponent(
        props,
        defaultState,
        context,
    );

    expect(tree.find('Connect(ListingOutputTypeSwitcher)').prop('defaultType')).toBe('carousel');
});

it('вид листинга по умолчанию в новых', () => {
    const props = {
        category: 'cars',
        onFilterChange: () => {},
        section: 'new',
        sorts: [],
    };

    const tree = shallowRenderComponent(
        props,
        defaultState,
        contextMock,
    );

    expect(tree.find('Connect(ListingOutputTypeSwitcher)').prop('defaultType')).toBe('models_list');
});

function shallowRenderComponent(props = defaultProps, state = defaultState, context = contextMock) {

    const ContextProvider = createContextProvider(context);
    const store = mockStore(state);
    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <ListingFilterPanel { ...props }/>
            </Provider>
        </ContextProvider>,
    );
    return wrapper.dive().dive();
}
