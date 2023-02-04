const React = require('react');
const { shallow } = require('enzyme');

const BreadcrumbsPanel = require('./BreadcrumbsPanel');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

it('правильно формирует разметку для страницы легковых с 1+ поколениями', () => {
    const initialState = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                super_gen: [ '20104320', '21126901' ],
                category: 'CARS',
                catalog_filter: [
                    { mark: 'FORD', model: 'ECOSPORT', generation: '20104320' },
                    { mark: 'FORD', model: 'ECOSPORT', generation: '21126901' },
                ],
            },
        },
    };
    const page = shallowRenderComponent({ initialState });

    expect(page).toMatchSnapshot();
});

it('правильно формирует разметку для страницы легковых с 1 поколением', () => {
    const initialState = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.value(),
        reviews: {
            params: {
                mark: 'FORD',
                model: 'ECOSPORT',
                super_gen: [ '20104320' ],
                category: 'CARS',
                catalog_filter: [
                    { mark: 'FORD', model: 'ECOSPORT', generation: '20104320' },
                ],
            },
        },
    };
    const page = shallowRenderComponent({ initialState });

    expect(page).toMatchSnapshot();
});

it('правильно формирует разметку для страницы мото', () => {
    const initialState = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.withCategoryBreadcrumbs({
            category: 'moto',
            sub_category: 'atv',
        }).value(),
        reviews: {
            params: {
                mark: 'BRP',
                model: 'COMMANDER_1000',
                category: 'MOTO',
                sub_category: 'ATV',
                catalog_filter: [
                    { mark: 'BRP', model: 'COMMANDER_1000' },
                ],
            },
        },
    };
    const page = shallowRenderComponent({ initialState });

    expect(page).toMatchSnapshot();
});

it('правильно формирует разметку для страницы комтранса', () => {
    const initialState = {
        breadcrumbsPublicApi: breadcrumbsPublicApiMock.withCategoryBreadcrumbs({
            category: 'trucks',
            sub_category: 'bus',
        }).value(),
        reviews: {
            params: {
                mark: 'FUSO',
                model: 'ROSA',
                category: 'TRUCKS',
                sub_category: 'BUS',
                catalog_filter: [
                    { mark: 'FUSO', model: 'ROSA' },
                ],
            },
        },
    };
    const page = shallowRenderComponent({ initialState });

    expect(page).toMatchSnapshot();
});

function shallowRenderComponent({ initialState }) {
    const ContextProvider = createContextProvider(contextMock);
    const store = mockStore(initialState);

    return shallow(
        <ContextProvider>
            <BreadcrumbsPanel store={ store }/>
        </ContextProvider>,
    ).dive().dive();
}
