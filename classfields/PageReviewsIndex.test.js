/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

const React = require('react');
const { shallow } = require('enzyme');
const { Provider } = require('react-redux');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const PageReviewsIndex = require('./PageReviewsIndex');

it(`на главной отзывов по умолчанию должен быть выбран таб "актуальные"`, () => {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore({
        reviews: {
            params: {
                category: 'CARS',
            },
        },
    });

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageReviewsIndex
                    location={{}}
                    route={{}}
                />
            </Provider>
        </ContextProvider>,
    ).dive().dive().dive();
    const activeTab = wrapper.find('.PageReviewsIndex__tab_active');
    expect(activeTab.text()).toEqual('Актуальные');
    const link = wrapper.find('Link');
    expect(link).toHaveProp('url', 'link/reviews-listing/?parent_category=cars&sort=relevance-exp1-desc');
});
