jest.mock('auto-core/react/lib/gateApi', () => ({
    getResource: jest.fn(),
    getResourcePublicApi: jest.fn(),
}));

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const gateApi = require('auto-core/react/lib/gateApi');
const stateMock = require('../mocks/state.mock');
const changeFilters = require('./changeFilters');

const gateApiMock = jest.fn(() => Promise.resolve());

const FILTER_VALUES = {
    tech_param_id: [ '346' ],
    complectation_name: 'Test',
    transmission: [ 'MECHANICAL' ],
    gear_type: [],
    color: [],
};

let store;
beforeEach(() => {
    store = mockStore(stateMock);
    gateApi.getResource.mockImplementation(gateApiMock);
    gateApi.getResourcePublicApi.mockImplementation(gateApiMock);
});

describe('изменение поисковых параметров и урла', () => {

    let actions;
    let result;
    beforeEach(() => {
        result = store.dispatch(changeFilters(FILTER_VALUES, { complectationFilter: true }));
        actions = store.getActions();
    });

    it('должен вызвать изменение поисковых параметров в стейте в соответствии со значениями фильтра', () => {
        expect(actions).toContainEqual({
            type: 'LISTING_CHANGE_PARAMS',
            payload: {
                category: 'cars',
                catalog_filter: [
                    {
                        configuration: '666',
                        generation: '666',
                        mark: 'A',
                        model: 'B',
                        tech_param: '346',
                    },
                ],
                color: [],
                gear_type: [],
                search_tag: [],
                transmission: [ 'MECHANICAL' ],
                section: 'new',
            },
        });
    });

    it('должен вызвать изменение url в соответствии со значениями фильтра', () => {
        expect(result).toEqual({
            catalog_filter: [
                {
                    mark: 'A',
                    model: 'B',
                    configuration: '666',
                    generation: '666',
                    tech_param: '346',
                },
            ],
            category: 'cars',
            transmission: [ 'MECHANICAL' ],
            gear_type: [],
            search_tag: [],
            color: [],
            section: 'new',
            year_to: undefined,
        });
    });
});

const NEW_SEARCH_PARAMETERS = {
    catalog_filter: [
        {
            complectation_name: 'Test',
            configuration: '666',
            generation: '666',
            mark: 'A',
            model: 'B',
            tech_param: '346',
        },
    ],
    category: 'cars',
    transmission: [ 'MECHANICAL' ],
    color: [],
    gear_type: [],
    search_tag: [],
    section: 'new',
    year_to: undefined,
};

describe('получение данных', () => {
    it('должен запросить получение количества офферов c правильными параметрами и ,если передать fetchOnlyCounts === true', () => {
        store.dispatch(changeFilters(FILTER_VALUES, { fetchOnlyCounts: true }));
        expect(gateApi.getResource.mock.calls).toEqual([
            [ 'listingCount', NEW_SEARCH_PARAMETERS ],
            [ 'equipmentFilters', NEW_SEARCH_PARAMETERS ],
        ]);
    });

    it('должен запросить получение списка офферов c правильными параметрами, если не передать fetchOnlyCounts', () => {
        store.dispatch(changeFilters(FILTER_VALUES));
        expect(gateApi.getResourcePublicApi).toHaveBeenCalledWith('cardGroupOffers', NEW_SEARCH_PARAMETERS);
    });

    it('должен запросить получение списка актуальных опций c правильными параметрами', () => {
        store.dispatch(changeFilters(FILTER_VALUES));
        expect(gateApi.getResource).toHaveBeenCalledWith('equipmentFilters', NEW_SEARCH_PARAMETERS);
    });
});
