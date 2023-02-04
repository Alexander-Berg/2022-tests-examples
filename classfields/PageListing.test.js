/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/cookie', () => {
    return {
        get: jest.fn(cookieName => cookieName === 'offer_is_banned'),
        remove: jest.fn(),
    };
});
jest.mock('auto-core/react/dataDomain/notifier/actions/notifier');
jest.mock('auto-core/react/dataDomain/listing/actions/fetch', () => {
    return jest.fn(() => ({ type: '__MOCK_listing_fetch' }));
});
jest.mock('auto-core/react/dataDomain/listingPremium/actions/fetchPremiumOffers', () => ({
    fetchPremiumOffersAction: jest.fn(() => () => { }),
}));
jest.mock('auto-core/react/dataDomain/listing/actions/setSearchParameters', () => {
    const mock = jest.fn(() => ({ type: '__MOCK_listing_setSearchParameters' }));
    mock['default'] = mock;
    return mock;
});
jest.mock('auto-core/react/dataDomain/breadcrumbsPublicApi/actions/fetchWithFilters', () => {
    return {
        'default': jest.fn(() => ({ type: '__MOCK_breadcrumbsPublicApi_fetchWithFilters' })),
    };
});
jest.mock('auto-core/react/dataDomain/config/actions/updateData', () => {
    return jest.fn(() => ({ type: '__MOCK_config_updateData' }));
});

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const { shallow } = require('enzyme');

const { Provider } = require('react-redux');
const React = require('react');

const PageListing = require('./PageListing');

const cookie = require('auto-core/react/lib/cookie');
const updateData = require('auto-core/react/dataDomain/config/actions/updateData');
const fetchListing = require('auto-core/react/dataDomain/listing/actions/fetch');
const setSearchParameters = require('auto-core/react/dataDomain/listing/actions/setSearchParameters').default;
const { showAutoclosableMessage } = require('auto-core/react/dataDomain/notifier/actions/notifier');

const COOKIE_OFFER_IS_BANNED = 'offer_is_banned';
const POPUP_MESSAGE = 'Объявление удалено модератором';

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
                },
            },
            searchID: '',
        },
        dealerCounter: {
            data: {},
        },
        state: {
            headerHeight: 52,
        },
        bunker: {
            'common/metrics': {},
            'common/with_proven_owner': {
                isSortEnabled: false,
            },
        },
        config: configStateMock.value(),
        redirectParams: {
            data: {},
        },
        geo: {
            gids: [],
        },
        matchApplication: {},
        breadcrumbsPublicApi: {
            data: [],
        },
    };
    defaultProps = {
        location: {
            query: {},
            pathname: '',
        },
        route: {
            name: '',
        },
        params: {
            from: '',
        },
        updateConfigData: updateData,
    };
});

it(`проверит куку "${ COOKIE_OFFER_IS_BANNED }". если она есть, покажет попап и удалит эту куку`, () => {
    shallowRenderPageListing();

    expect(cookie.get).toHaveBeenCalledWith(COOKIE_OFFER_IS_BANNED);

    expect(showAutoclosableMessage).toHaveBeenCalledTimes(1);
    expect(showAutoclosableMessage).toHaveBeenCalledWith({ message: POPUP_MESSAGE });

    expect(cookie.remove).toHaveBeenCalledWith(COOKIE_OFFER_IS_BANNED);
});

describe('изменение параметров', () => {
    it('должен сделать push в историю, запросить листинг при изменении section и поменять урл для перехода в десктоп', () => {
        const wrapper = shallowRenderPageListing();
        wrapper.find('Connect(ListingHead)').simulate('ChangeFilter', 'used', { name: 'section' });

        expect(fetchListing).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledTimes(0);
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=cars&section=used', { loadData: false });
        expect(updateData).toHaveBeenCalledWith({ desktopUrl: 'linkDesktop/listing/?category=cars&section=used&nomobile=true' });
    });

    it('должен сделать push в историю, запросить листинг при сбросе пресета и поменять урл для перехода в десктоп', () => {
        initialState.listing.data.search_parameters.price_to = 1000000;
        initialState.listing.data.search_parameters.special = true;
        const wrapper = shallowRenderPageListing();
        const listingHead = wrapper.find('Connect(ListingHead)').dive().dive();

        // Пресеты всегда идут вторыми
        listingHead.find('Tag').at(1).simulate('click');

        expect(setSearchParameters).toHaveBeenCalledTimes(1);
        expect(setSearchParameters).toHaveBeenCalledWith({
            category: 'cars',
            section: 'all',
            price_to: 1000000,
        });
        expect(fetchListing).toHaveBeenCalledTimes(1);
        expect(contextMock.replaceState).toHaveBeenCalledTimes(0);
        expect(contextMock.pushState).toHaveBeenCalledTimes(1);
        expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=cars&section=all&price_to=1000000',
            { loadData: false });
        expect(updateData).toHaveBeenCalledWith({ desktopUrl: 'linkDesktop/listing/?category=cars&section=all&price_to=1000000&nomobile=true' });
    });

    describe('ABT_VS_678_PESSIMIZATION_BEATEN', () => {
        it('должен сделать push в историю со всеми параметрами вне экспа', () => {
            const state = {
                ...initialState,
                listing: { ...initialState.listing, data: {
                    ...initialState.listing.data,
                    search_parameters: {
                        ...initialState.listing.data.search_parameters,
                        damage_group: 'ANY',
                        customs_state_group: 'DOESNT_MATTER',
                        price_to: 999999,
                    },
                } },
            };

            const wrapper = shallowRenderPageListing(defaultProps, state, contextMock);
            wrapper.find('Connect(ListingHead)').simulate('changeFilter', 'used', { name: 'section' });

            expect(contextMock.pushState)
                .toHaveBeenCalledWith('link/listing/?category=cars&section=used&damage_group=ANY&customs_state_group=DOESNT_MATTER&price_to=999999',
                    { loadData: false });
        });

        it('должен сделать push в историю без эксповых дефолтных параметров в экспе', () => {
            const context = {
                ...contextMock,
                hasExperiment: exp => exp === 'ABT_VS_678_PESSIMIZATION_BEATEN',
            };
            const state = {
                ...initialState,
                listing: { ...initialState.listing, data: {
                    ...initialState.listing.data,
                    search_parameters: {
                        ...initialState.listing.data.search_parameters,
                        damage_group: 'ANY',
                        customs_state_group: 'DOESNT_MATTER',
                        price_to: 999999,
                    },
                } },
            };

            const wrapper = shallowRenderPageListing(defaultProps, state, context);
            wrapper.find('Connect(ListingHead)').simulate('changeFilter', 'used', { name: 'section' });

            expect(contextMock.pushState).toHaveBeenCalledWith('link/listing/?category=cars&section=used&price_to=999999',
                { loadData: false });
        });
    });
});

function shallowRenderPageListing(props = defaultProps, state = initialState, context = contextMock) {
    const store = mockStore(state);

    const ContextProvider = createContextProvider(context);

    const wrapper = shallow(
        <ContextProvider>
            <Provider store={ store }>
                <PageListing { ...props }/>
            </Provider>
        </ContextProvider>,
    );

    return wrapper.dive().dive().find('Connect(PageListingDumb)').dive().dive();
}
