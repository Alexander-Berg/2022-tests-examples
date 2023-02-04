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

it(`на главной отзывов по умолчанию должен быть пресет "актуальные"`, () => {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore({
        breadcrumbs: {},
        breadcrumbsParams: { data: {} },
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
    const firstPreset = wrapper.find('SliderListing').first();
    expect(firstPreset.props().title).toEqual('Актуальные');
    expect(firstPreset.props().titleUrl).toEqual('link/reviews-listing-all/?parent_category=&sort=relevance-exp1-desc');
});
