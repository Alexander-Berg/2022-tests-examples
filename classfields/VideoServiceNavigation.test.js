const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const VideoServiceNavigation = require('./VideoServiceNavigation');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

afterEach(() => {
    contextMock.hasExperiment.mockReset();
});

const TEST_CASES = [
    {
        name: 'без марки',
        params: {
            category: 'cars',
        },
    },
    {
        name: 'с маркой',
        params: {
            category: 'cars',
            mark: 'vaz',
        },
    },
    {
        name: 'с маркой и моделью',
        params: {
            category: 'cars',
            mark: 'vaz',
            model: 'vesta',
        },
    },
    {
        name: 'с маркой, моделью и поколением',
        params: {
            category: 'cars',
            mark: 'vaz',
            model: 'vesta',
            super_gen: '20417749',
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

const DEFAULT_STATE = {
    cookies: {},
};

const store = mockStore(DEFAULT_STATE);

TEST_CASES.forEach((testCase) => {
    it(`должен нормально отрендериться список табов в разделе видео ${ testCase.name }`, () => {
        if (testCase.exp) {
            contextMock.hasExperiment.mockImplementation((exp) => exp === testCase.exp);
        }

        const ContextProvider = createContextProvider(contextMock);
        const tree = shallow(
            <ContextProvider>
                <Provider store={ store }>
                    <VideoServiceNavigation
                        pageParams={ testCase.params }
                    />
                </Provider>
            </ContextProvider>,
        ).dive().dive().dive();
        expect(shallowToJson(tree)).toMatchSnapshot();
    });
});
