jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetch', () => {
    return {
        'default': jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFiltersAndMerge', () => {
    return {
        'default': jest.fn(),
    };
});
const loadBreadcrumbs = require('./loadBreadcrumbs');
const mockStore = require('autoru-frontend/mocks/mockStore').default;

it('должен вызвать fetch, если категория - мото или коммТС', () => {
    const store = mockStore({
        formFields: {
            data: {
                category: {
                    value: 'moto',
                },
            },
        },
    });
    const fetchWithFilters = require('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetch').default;
    fetchWithFilters.mockImplementation(() => ({ type: 'FETCH' }));
    store.dispatch(loadBreadcrumbs({ parent_category: 'moto' }));

    expect(fetchWithFilters).toHaveBeenCalledWith({
        category: 'moto',
        catalog_filter: [
            { mark: undefined, model: undefined },
        ],
        moto_category: 'moto',
    });
});

it('должен вызвать fetchWithFiltersAndMerge, если категория - легковые автомобили', () => {
    const store = mockStore({
        formFields: {
            data: {
                mark: {
                    value: 'LAND_ROVER',
                },
            },
        },
    });
    const fetchWithFiltersAndMerge = require('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFiltersAndMerge').default;
    fetchWithFiltersAndMerge.mockImplementation(() => ({ type: 'FETCH_WITH_FILTERS_AND_MERGE' }));
    store.dispatch(loadBreadcrumbs({ parent_category: 'cars' }));

    expect(fetchWithFiltersAndMerge).toHaveBeenCalledWith({
        category: 'cars',
        catalog_filter: [
            { mark: 'LAND_ROVER', model: undefined },
        ],
    });
});
