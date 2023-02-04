const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const ReviewsServiceNavigation = require('./ReviewsServiceNavigation');

const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

const TEST_CASES = [
    {
        name: 'cars с маркой, моделью, поколением',
        params: {
            mark: 'bmw',
            model: '1er',
            parent_category: 'cars',
            sort: 'updateDate-desc',
            super_gen: '7707451',
        },
    },
    {
        name: 'cars с маркой и моделью',
        params: {
            mark: 'bmw',
            model: '1er',
            parent_category: 'cars',
        },
    },
    {
        name: 'cars с маркой',
        params: {
            mark: 'bmw',
            parent_category: 'cars',
        },
    },
    {
        name: 'cars',
        params: {
            parent_category: 'cars',
        },
    },
    {
        name: 'moto с маркой и моделью',
        params: {
            category: 'motorcycle',
            mark: 'bmw',
            model: 'f_650_gs',
            parent_category: 'moto',
            sort: 'updateDate-desc',
        },
    },
    {
        name: 'moto с маркой',
        params: { category: 'motorcycle',
            mark: 'bmw',
            parent_category: 'moto',
        },
    },
    {
        name: 'moto',
        params: {
            category: 'motorcycle',
            parent_category: 'moto',
        },
    },
    {
        name: 'moto без категории',
        params: {
            parent_category: 'moto',
        },
    },
    {
        name: 'trucks с маркой и моделью',
        params: {
            category: 'bus',
            mark: 'neoplan',
            model: 'spaceliner',
            parent_category: 'trucks',
            sort: 'updateDate-desc',
        },
    },
    {
        name: 'trucks с маркой',
        params: {
            category: 'bus',
            mark: 'neoplan',
            parent_category: 'trucks',
        },
    },
    {
        name: 'trucks',
        params: {
            category: 'bus',
            parent_category: 'trucks',
        },
    },
    {
        name: 'c кредитами вместо видео',
        exp: 'AUTORUFRONT-16575_credit_menu_exp',
        params: {
            category: 'cars',
        },
    },
];

TEST_CASES.forEach((testCase) => {
    it(`должен нормально отрендериться список табов в отзывах ${ testCase.name }`, () => {
        if (testCase.exp) {
            contextMock.hasExperiment.mockImplementation((exp) => exp === testCase.exp);
        }

        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <Provider store={ store }>
                    <ReviewsServiceNavigation
                        pageParams={ testCase.params }
                    />
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});
