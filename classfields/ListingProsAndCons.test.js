const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingProsAndCons = require('./ListingProsAndCons');
const { Provider } = require('react-redux');

const Context = createContextProvider(contextMock);

const searchParams = {
    catalog_filter: [
        {
            mark: 'BMW',
            model: 'M5',
        },
    ],
    category: 'cars',
    section: 'all',
};

const features = {
    negative: [
        {
            minus_count: 4,
            name: 'Расход топлива',
            plus_count: 1,
            total_count: 5,
            type: 'FUEL',
        },
    ],
    positive: [
        {
            minus_count: 2,
            name: 'Комфорт',
            plus_count: 6,
            total_count: 8,
            type: 'COMFORT',
        },
    ],
    controversy: [
        {
            minus_count: 3,
            name: 'Подвеска',
            plus_count: 2,
            total_count: 5,
            type: 'SUSPENSION',
        },
    ],
};

it('отрисует блок при наличии отзывов', () => {
    const store = mockStore({
        reviewsFeatures: {
            data: {
                features,
            },
        },
        listing: {
            data: {
                search_parameters: searchParams,
            },
        },
    });

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <ListingProsAndCons searchParams={ searchParams }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(tree.isEmptyRender()).toBe(false);
});

it('не отрисует блок, если есть только один вид отзывов', () => {
    const store = mockStore({
        reviewsFeatures: {
            data: {
                features: { negative: features.negative },
            },
        },
        listing: {
            data: {
                search_parameters: searchParams,
            },
        },
    });

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <ListingProsAndCons searchParams={ searchParams }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();

    expect(tree.isEmptyRender()).toBe(true);
});
