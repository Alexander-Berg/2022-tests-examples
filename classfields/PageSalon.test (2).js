/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/dataDomain/listing/actions/fetch', () => {
    return jest.fn(() => ({ type: '__MOCK_listing_fetch' }));
});

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');
const React = require('react');

const PageSalon = require('./PageSalon');

const fetchListing = require('auto-core/react/dataDomain/listing/actions/fetch');

let initialState;
let defaultProps;
beforeEach(() => {
    initialState = {
        listing: {
            data: {
                offers: [],
                pagination: {},
                request_id: 'abc123',
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                    dealer_id: '111',
                    dealer_name: '111',
                },
            },
            searchID: '',
        },
        salonInfo: { data: {} },
        bunker: {
            'common/metrics': {},
        },
        geo: {
            gids: [],
        },
        router: {
            current: {
                name: 'dealer-page-official',
                params: { from: '' },
            },
        },
    };
});

describe('изменение параметров', () => {
    it('должен сделать push в историю и запросить листинг при изменении section', () => {
        const wrapper = shallowRenderPageListing();
        wrapper.find('Connect(ListingHead)').simulate('ChangeFilter', 'used', { name: 'section' });

        expect(fetchListing).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState).toHaveBeenCalledTimes(0);
        expect(contextMock.replaceState).toHaveBeenCalledWith('link/dealer-page-official/?category=cars&section=used&dealer_id=111&dealer_name=111');
    });

    it('должен сделать поменять урл и обновить листинг при смене ммм', () => {
        const wrapper = shallowRenderPageListing();
        const mmmFiletr = wrapper.find('Connect(ListingHead)').dive().dive().find('Connect(MMMMultiFilterCore)');

        mmmFiletr.simulate('change', { catalog_filter: [ { mark: 'AUDI', model: 'A3' } ] });

        expect(fetchListing).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledTimes(0);
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState).toHaveBeenCalledWith(
            'link/dealer-page-official/?category=cars&section=all&dealer_id=111&dealer_name=111&catalog_filter=mark%3DAUDI%2Cmodel%3DA3',
        );
    });
});

describe('ABT_VS_678_PESSIMIZATION_BEATEN', () => {
    const state = {
        listing: {
            data: {
                offers: [],
                pagination: {},
                request_id: 'abc123',
                search_parameters: {
                    category: 'cars',
                    section: 'all',
                    dealer_id: '111',
                    dealer_name: '111',
                    damage_group: 'ANY',
                    customs_state_group: 'DOESNT_MATTER',
                    price_to: 999999,
                },
            },
            searchID: '',
        },
        salonInfo: { data: {} },
        bunker: {
            'common/metrics': {},
        },
        geo: {
            gids: [],
        },
        router: {
            current: {
                name: 'dealer-page-official',
                params: { from: '' },
            },
        },
    };

    it('должен сделать push в историю со всеми параметрами вне экспа', () => {
        const wrapper = shallowRenderPageListing(defaultProps, state, contextMock);
        wrapper.find('Connect(ListingHead)').simulate('changeFilter', 'used', { name: 'section' });

        expect(contextMock.replaceState)
            // eslint-disable-next-line max-len
            .toHaveBeenCalledWith('link/dealer-page-official/?category=cars&section=used&dealer_id=111&dealer_name=111&damage_group=ANY&customs_state_group=DOESNT_MATTER&price_to=999999');
    });

    it('должен сделать push в историю без эксповых дефолтных параметров в экспе', () => {
        const context = {
            ...contextMock,
            hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN',
        };

        const wrapper = shallowRenderPageListing(defaultProps, state, context);
        wrapper.find('Connect(ListingHead)').simulate('changeFilter', 'used', { name: 'section' });

        expect(contextMock.replaceState)
            .toHaveBeenCalledWith('link/dealer-page-official/?category=cars&section=used&dealer_id=111&dealer_name=111&price_to=999999');
    });
});

function shallowRenderPageListing(props = defaultProps, state = initialState, context = contextMock) {
    const store = mockStore(state);

    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageSalon { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive().dive();
}
