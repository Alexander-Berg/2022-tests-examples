/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/lib/util/vas/logger', () => {
    return {
        'default': jest.fn(() => ({
            logVasEvent: jest.fn(),
        })),
    };
});
jest.mock('auto-core/react/dataDomain/listing/actions/fetch');
jest.mock('auto-core/react/dataDomain/equipmentFilters/actions/fetch');

const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const pageParamsMock = require('autoru-frontend/mockData/pageParams_cars.mock');

const fetchListing = require('auto-core/react/dataDomain/listing/actions/fetch');
fetchListing.mockImplementation(() => () => {});
const fetchEquipmentFilters = require('auto-core/react/dataDomain/equipmentFilters/actions/fetch');
fetchEquipmentFilters.mockImplementation(() => () => {});

const PageListingCars = require('./PageListingCars');

const Context = createContextProvider(contextMock);

let store;
let props;

beforeEach(() => {
    store = mockStore({
        config: {
            data: {},
        },
        autoguru: {
            answerValues: [],
        },
        matchApplication: {},
        bunker: {
            'common/metrics': {},
            'common/with_proven_owner': {
                isSortEnabled: false,
            },
        },
        listing: {
            data: {
                request_id: 'abc123',
            },
            searchID: '1',
            filteredOffersCount: 7,
        },
        geo: {
            gids: [],
        },
        user: { data: {} },
    });
    props = {
        params: _.cloneDeep(pageParamsMock),
        route: {},
    };
});

afterEach(() => {
    jest.resetModules();
});

it('отправляет повторный запрос, если нулевая выдача при текущих фильтрах', () => {
    const page = renderComponent();

    page.setProps({
        filteredOffersCount: 0,
    });

    setTimeout(() => {
        expect(fetchListing).toHaveBeenCalledTimes(1);
    }, 500);

});

it('отправляет повторный, если нулевая выдача и меняются фильтры', () => {
    const page = renderComponent();

    page.setProps({
        searchParams: { foo: 'bar' },
    });

    setTimeout(() => {
        expect(fetchListing).toHaveBeenCalledTimes(1);
    }, 500);

});

it('должен отправить запрос за новыми equpmentFilters при смене в пустом листинге', () => {
    const page = renderComponent();

    const listingFilterPanel = page.find('ListingFilterPanel');
    listingFilterPanel.props().onFilterChange(123, { name: 'panorama' });

    expect(fetchEquipmentFilters).toHaveBeenCalledTimes(1);
});

it('не должен дернуть обновление листинга, если листинг пустой и нажали на добавления фильтра ММП', () => {
    const page = renderComponent();

    page.setProps({
        searchParams: { foo: 'bar' },
    });

    const carsFiltersComponent = page.find('ListingCarsFilters');
    carsFiltersComponent.simulate('change', { catalog_filter: [
        { mark: 'SUBARU' }, {},
    ] }, { name: 'mmm-filter' });

    setTimeout(() => {
        expect(fetchListing).not.toHaveBeenCalled();
    }, 500);
});

function renderComponent() {
    const tree = shallow(
        <Context>
            <PageListingCars
                { ...props }
                store={ store }
            />
        </Context>,
    ).dive().dive();
    return tree;
}
