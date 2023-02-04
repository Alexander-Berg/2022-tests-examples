const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ListingServiceNavigation = require('./ListingServiceNavigation');

const TEST_CASES = [
    {
        name: 'all без параметров',
        params: {
            category: 'cars',
            section: 'all',
        },
    },
    {
        name: 'new с маркой',
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [ { mark: 'FORD' } ],
        },
    },
    {
        name: 'new с маркой, моделью, комплектацией',
        params: {
            category: 'cars',
            section: 'new',
            catalog_filter: [ { mark: 'FORD', model: 'FOCUS' } ],
            complectation_name: 'Trend',
        },
    },
    {
        name: 'used с маркой, моделью, поколением',
        params: {
            category: 'cars',
            catalog_filter: [ { mark: 'FORD', model: 'FOCUS', generation: '3483450' } ],
            section: 'used',
        },
    },
    {
        name: 'used с кучей параметров',
        params: {
            body_type_group: 'SEDAN',
            category: 'cars',
            configuration_id: '4760019',
            currency: 'USD',
            displacement_from: '2000',
            displacement_to: '2000',
            engine_group: 'GASOLINE',
            gear_type: 'FORWARD_CONTROL',
            has_history: 'true',
            section: 'used',
            sort: 'fresh_relevance_1-desc',
            tech_param_id: '6283532',
            top_days: '7',
            transmission: 'AUTOMATIC',
        },
    },
];

const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

TEST_CASES.forEach((testCase) => {
    it(`должен нормально отрендериться список табов в листинге ${ testCase.name }`, () => {
        const ContextProvider = createContextProvider(contextMock);
        const tree = shallow(
            <ContextProvider>
                <Provider store={ store }>
                    <ListingServiceNavigation
                        pageParams={ testCase.params }
                    />
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});
