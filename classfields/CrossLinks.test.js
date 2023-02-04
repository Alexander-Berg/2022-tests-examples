const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { Provider } = require('react-redux');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const Context = createContextProvider(contextMock);
const store = require('./CrossLinks.store.mock');

const CrossLinks = require('./CrossLinks');

const ratingProp = {
    ratings: [
        {
            name: 'total',
            value: 4.7,
        },
    ],
};

const listingSectionsCount = { 'new': 4, used: 1 };

it('Рендерит полный блок CrossLinks - all', async() => {
    const mockedStore = mockStore(store);
    const tree = shallow(
        <Context>
            <Provider store={ mockedStore }>
                <CrossLinks
                    averageRating={ ratingProp }
                    mark="FORD"
                    markName="Ford"
                    model="C_MAX"
                    modelName="C-MAX"
                    params={{ section: 'all' }}
                    listingSectionsCount={ listingSectionsCount }
                />
            </Provider>
        </Context >,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('Рендерит полный блок CrossLinks - new', () => {
    const state = _.cloneDeep(store);
    state.listing.data.search_parameters.section = 'new';
    const mockedState = mockStore(state);
    const tree = shallow(
        <Context>
            <Provider store={ mockedState }>
                <CrossLinks
                    averageRating={ ratingProp }
                    mark="FORD"
                    markName="Ford"
                    model="C_MAX"
                    modelName="C-MAX"
                    listingSectionsCount={ listingSectionsCount }
                />
            </Provider>
        </Context >,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('Рендерит полный блок CrossLinks - used', async() => {
    const state = _.cloneDeep(store);
    state.listing.data.search_parameters.section = 'used';
    const mockedState = mockStore(state);
    const tree = shallow(
        <Context>
            <Provider store={ mockedState }>
                <CrossLinks
                    averageRating={ ratingProp }
                    mark="FORD"
                    markName="Ford"
                    model="C_MAX"
                    modelName="C-MAX"
                    searchParameters={{
                        catalog_filter: [
                            {
                                mark: 'FORD',
                                model: 'C_MAX',
                            },
                        ],
                        section: 'used',
                        category: 'cars',
                    }}
                    listingSectionsCount={ listingSectionsCount }
                />
            </Provider>
        </Context >,
    ).dive().dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

const LINKS = _.values(CrossLinks.PAGES);

LINKS.forEach((link) => {
    it('Рендерит блок CrossLinks со ссылкой ' + link, async() => {
        const mockedStore = mockStore(store);
        const tree = shallow(
            <Context>
                <Provider store={ mockedStore }>
                    <CrossLinks
                        averageRating={ ratingProp }
                        mark="BMW"
                        markName="BMW"
                        model="M4"
                        modelName="M4"
                        params={{ section: 'all' }}
                        listingSectionsCount={ listingSectionsCount }
                    />
                </Provider>
            </Context >,
        ).dive().dive().dive();
        const linkArr = tree.find('.CrossLinks__linkWrapper');
        expect(JSON.stringify(shallowToJson(linkArr))).toContain(link);
    });
});

LINKS.forEach((link) => {
    it('Рендерит блок CrossLinks без ссылки на текущую страницу ' + link, async() => {
        const mockedStore = mockStore(store);
        const tree = shallow(
            <Context>
                <Provider store={ mockedStore }>
                    <CrossLinks
                        averageRating={ ratingProp }
                        currentPage={ link }
                        mark="BMW"
                        markName="BMW"
                        model="M4"
                        modelName="M4"
                        params={{ section: 'all' }}
                        listingSectionsCount={ listingSectionsCount }
                    />
                </Provider>
            </Context >,
        ).dive().dive().dive();
        const linkArr = tree.find('.CrossLinks__linkWrapper');
        expect(JSON.stringify(shallowToJson(linkArr))).not.toContain(link);
    });
});

it('Для шильдиков с no_model=true отрисовало только шильдик', async() => {
    const state = _.cloneDeep(store);
    state.listing.data.crossLinks = {
        nameplates: [
            {
                nameplate: '9264570',
                key: 'FORD_C_MAX_grand',
                value: '9264570',
            },
        ],
    };
    const mockedState = mockStore(state);
    const tree = shallow(
        <Context>
            <Provider store={ mockedState }>
                <CrossLinks
                    averageRating={ ratingProp }
                    mark="FORD"
                    markName="Ford"
                    model="C_MAX"
                    modelName="C-MAX"
                    listingSectionsCount={ listingSectionsCount }
                />
            </Provider>
        </Context >,
    ).dive().dive().dive();
    expect(JSON.stringify(shallowToJson(tree)).toString()).not.toContain('C-MAX Grand C-MAX');
    expect(JSON.stringify(shallowToJson(tree)).toString()).toContain('Grand C-MAX');
    expect(shallowToJson(tree.find('.CrossLinks__wrapper'))).toMatchSnapshot();
});
