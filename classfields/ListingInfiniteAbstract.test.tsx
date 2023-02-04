jest.mock('auto-core/react/dataDomain/infiniteListing/actions/fetch');

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import listingState from 'autoru-frontend/mockData/state/listingState.mock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import cardMock from 'auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock';
import type { RegionWithLinguistics } from 'auto-core/react/dataDomain/geo/StateGeo';
import fetchInfiniteListing from 'auto-core/react/dataDomain/infiniteListing/actions/fetch';
import { TInfiniteListingStatus } from 'auto-core/react/dataDomain/infiniteListing/TStateInfiniteListing';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import type { Props } from './ListingInfiniteAbstract';
import ListingInfiniteAbstract from './ListingInfiniteAbstract';

const fetchInfiniteListingMock = fetchInfiniteListing as jest.MockedFunction<typeof fetchInfiniteListing>;
fetchInfiniteListingMock.mockImplementation(() => () => Promise.resolve());

const gidsInfoMock = {
    id: 2,
    linguistics: {
        preposition: 'в',
        prepositional: 'Санкт-Петербурге',
    },
    name: 'Санкт-Петербург',
    supports_geo_radius: true,
    type: 6,
} as RegionWithLinguistics;
const initStore = {
    ...listingState,
    banks: { creditConfig: {} },
    geo: {
        radius: 200,
        gidsInfo: [ gidsInfoMock ],
    },
    infiniteListing: {
        offers: [
            cloneOfferWithHelpers(cardMock).withSaleId('111-222').value(),
            cloneOfferWithHelpers(cardMock).withSaleId('333-444').value(),
        ],
        pagination: {
            current: 1,
            from: 1,
            to: 37,
            page: 1,
            page_size: 37,
            total_page_count: 1,
            total_offers_count: 37,
        },
        status: TInfiniteListingStatus.SUCCESS,
    },
    listingGeoRadiusCounters: {
        data: [
            { radius: 300, count: 1000 },
            { radius: 400, count: 100600 },
        ],
        pending: false,
    },
};

interface TestProps extends Props {
    searchID?: string;
}

class ListingInfiniteTest extends ListingInfiniteAbstract<TestProps> {
    className = 'ListingInfiniteTest';

    renderPageItems = (items: Array<Offer>, pageIndex: number): Array<JSX.Element> | JSX.Element => {
        const renderItems = items.map(this.renderListingItem, this);

        return (
            <div className="ListingInfiniteDesktop__page" key={ pageIndex }>
                { renderItems }
            </div>
        );
    };

    renderListingItem(item: Offer, index: number): JSX.Element {
        const { metrikaSnippetClick, metrikaSnippetShow } = this.state;
        return (
            <div
                key={ item.id + index }
                data-click={ metrikaSnippetClick }
                data-show={ metrikaSnippetShow }
            />
        );
    }
}

const ConnectedListingInfiniteTest = ListingInfiniteAbstract.connector(ListingInfiniteTest);

describe('не должен отрисовать бесконечный листинг', () => {
    it('если выбрано более одного гео', async() => {
        const store = mockStore({
            ...initStore,
            geo: {
                ...initStore.geo,
                gidsInfo: [ gidsInfoMock, gidsInfoMock ],
            },
        });

        const wrapper = renderListing(store);

        expect(wrapper).toBeEmptyRender();
    });

    it('если выбранное гео не поддерживает расширение радиусами (область, регион и т.п.)', async() => {
        const store = mockStore({
            ...initStore,
            geo: {
                ...initStore.geo,
                gidsInfo: [ { ...gidsInfoMock, supports_geo_radius: false, type: 3 } ],
            },
        });

        const wrapper = renderListing(store);

        expect(wrapper).toBeEmptyRender();
    });
});

it('должен отрисовать бесконечный листинг и отправить метрики на показ', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: [],
            pagination: {
                total_offers_count: 0,
            },
        } },
    });
    const instance = renderListing(store).instance();

    instance.setState({
        rings: [ { count: 100, radius: 400 } ],
    });

    expect(contextMock.metrika.sendPageEvent).toHaveBeenNthCalledWith(1, [ 'infinite_listing', 'all', 'show_listing' ]);
});

it('должен отрисовать бесконечный листинг и прокинуть правильные метрики в сниппет', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: Array(3).fill({} as Offer),
            pagination: {
                total_offers_count: 0,
            },
        } },
    });
    const wrapper = renderListing(store);
    const instance = wrapper.instance();

    instance.setState({
        rings: [ { count: 100, radius: 400 } ],
    });

    expect(wrapper.find('div[data-click="infinite_listing,all,snippet_click"]').first()).toExist();
    expect(wrapper.find('div[data-show="infinite_listing,all,snippet_show"]').first()).toExist();
});

it('должен отрисовать бесконечный листинг и прокинуть метрики на клик по сниппету при пустом основном листинге', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: [],
            pagination: {
                total_offers_count: 0,
            },
        } },
    });
    const wrapper = renderListing(store);
    const instance = wrapper.instance();

    instance.setState({
        rings: [ { count: 100, radius: 400 } ],
    });

    expect(wrapper.find('div[data-click="infinite_listing,all,snippet_click,from_empty_listing"]').first()).toExist();
});

it('должен отрисовать бесконечный листинг и не запрашивать данные на клиенте при пустом основном листинге', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: [],
            pagination: {
                total_offers_count: 0,
            },
        } },
    });
    const wrapper = renderListing(store);
    const instance = wrapper.instance();

    instance.setState({
        rings: [ { count: 100, radius: 400 } ],
    });

    expect(fetchInfiniteListingMock).toHaveBeenCalledTimes(0);
    expect(wrapper.find('div[data-click="infinite_listing,all,snippet_click,from_empty_listing"]').first()).toExist();
});

it('прокидывает правильные параметры при запросе при пустом основном листинге при скролле страницы', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: [],
            pagination: {
                total_offers_count: 0,
            },
        } },
        listingGeoRadiusCounters: {
            data: [
                { count: 1, radius: 0 },
                { count: 10, radius: 100 },
                { count: 20, radius: 200 },
                { count: 100, radius: 1100 },
            ],
        },
        infiniteListing: {},
    });

    const wrapper = renderListing(store, contextMock);
    const instance = wrapper.instance();

    instance.setState({
        rings: [
            { count: 10, radius: 100 },
        ],
    });

    const container = wrapper.find('InView.ListingInfiniteTest__container');
    container.simulate('change', true);

    expect(fetchInfiniteListingMock).toHaveBeenCalledTimes(1);
    expect(fetchInfiniteListingMock).toHaveBeenCalledWith({
        category: 'cars',
        exclude_geo_radius: 200,
        exclude_rid: 2,
        geo_radius: 200,
        infinite_listing: true,
        rid: [ 2 ],
        section: 'all',
        sort: 'fresh_relevance_1-desc',
    });
});

it('прокидывает правильные параметры при запросе листинга для всей России', async() => {
    const store = mockStore({
        ...initStore,
        listing: { data: {
            search_parameters: {},
            offers: Array(3).fill({} as Offer),
            pagination: {
                total_offers_count: 20,
            },
        } },
        listingGeoRadiusCounters: {
            data: [
                { count: 1, radius: 0 },
                { count: 10, radius: 100 },
                { count: 20, radius: 200 },
                { count: 100, radius: 1100 },
            ],
        },
        infiniteListing: {},
    });

    const wrapper = renderListing(store, contextMock);
    const instance = wrapper.instance();

    instance.setState({
        rings: [
            { count: 100, radius: 1100 },
        ],
    });

    const container = wrapper.find('InView.ListingInfiniteTest__container');
    container.simulate('change', true);

    expect(fetchInfiniteListingMock).toHaveBeenCalledTimes(1);
    expect(fetchInfiniteListingMock).toHaveBeenCalledWith({
        category: 'cars',
        exclude_geo_radius: 200,
        exclude_rid: 166,
        geo_radius: undefined,
        infinite_listing: true,
        rid: [ 2 ],
        section: 'all',
        sort: 'fresh_relevance_1-desc',
    });
});

function renderListing(store: any, context?: typeof contextMock) {
    const params = { sort: 'fresh_relevance_1-desc', section: 'all' as Offer['section'], category: 'cars' as Offer['category'] };
    const Context = createContextProvider(context || contextMock);

    return shallow(
        <Context>
            <Provider store={ store }>
                <ConnectedListingInfiniteTest searchParameters={ params }/>
            </Provider>
        </Context>,
    ).dive().dive().dive();
}
