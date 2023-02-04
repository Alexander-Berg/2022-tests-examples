jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetch', () => {
    return {
        'default': jest.fn(() => ({ type: 'MOCK_ACTION' })),
    };
});

const React = require('react');
const { Provider } = require('react-redux');
const renderer = require('react-test-renderer');
const { shallow } = require('enzyme');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const fetchBreadcrumbs = require('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetch').default;

let Context;
let store;
beforeEach(() => {
    Context = createContextProvider(contextMock);
    store = mockStore({
        breadcrumbsPublicApi: breadcrumbsPublicApiMock,
        card: cardMock,
    });
});

const Breadcrumbs = require('./Breadcrumbs');

it('Должен отрендерить хлебные крошки', () => {
    const tree = renderer.create(
        <Context>
            <Provider store={ store }>
                <Breadcrumbs/>
            </Provider>
        </Context>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('Должен запросить хлебные крошки при наведении', () => {
    store = mockStore({
        breadcrumbsPublicApi: {},
        card: cardMock,
    });

    const tree = shallow(
        <Breadcrumbs/>,
        { context: { ...contextMock, store } },
    ).dive();

    tree.find('.CardBreadcrumbs__item').at(3).simulate('mouseenter');

    expect(fetchBreadcrumbs).toHaveBeenCalledWith({
        bc_lookup: 'FORD#ECOSPORT#20104320#20104322',
        category: 'cars',
        state: 'USED',
    });
});

it('не должен запросить хлебные крошки, если он уже есть', () => {
    store = mockStore({
        breadcrumbsPublicApi: {
            status: 'SUCCESS',
        },
        card: cardMock,
    });

    const tree = shallow(
        <Breadcrumbs/>,
        { context: { ...contextMock, store } },
    ).dive();

    tree.find('.CardBreadcrumbs__item').at(1).simulate('mouseenter');

    expect(fetchBreadcrumbs).not.toHaveBeenCalled();
});
