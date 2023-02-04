const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { Provider } = require('react-redux');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');

const DealerBreadcrumbs = require('auto-core/react/components/common/DealerBreadcrumbs/DealerBreadcrumbs');

const storeMock = {
    dealersListing: {
        isDealerMapShown: false,
        isDealerListShown: true,
        searchParams: { category: 'cars', mark: 'bmw', section: 'new', dealer_net_semantic_url: 'major' },
        dealerNameSuggest: '',
        suggestions: [],
        regions: [],
        mapBalloonDealer: [],
        isFetching: false,
        dealers: [ {
            dealerId: '20176484',
            dealerName: 'БМВ Авто-Авангард',
            dealerCode: 'avto_avangard_moskva_bmw',
            dealerLogo: '//avatars.mds.yandex.net/get-verba/937147/2a00000160940b33991adefb5f269208ccbe/dealer_logo',
            dealerLink: '/diler-oficialniy/cars/new/avto_avangard_moskva_bmw/bmw/?from=dealer-listing-list&dealer_net_semantic_url=major',
            isLoyalty: false,
            markId: 'bmw',
            markName: 'BMW',
            markLogo: '//avatars.mds.yandex.net/get-verba/997355/2a00000170a99124c3a521a92b8cd4846660/logo',
            netId: '20156383',
            netAlias: 'major',
            netName: 'Major',
            orgType: '1',
            address: 'Москва, Новорижское ш. 8 км от МКАД',
            metro: [],
            phones: { list: [], isFetching: false },
            latitude: 55.791693877645955,
            longitude: 37.258665854696225,
        } ],
        h1: 'Дилеры по продаже автомобилей',
        title: 'Все дилеры - автосалоны по продаже новых и бу автомобилей',
        description: 'На auto.ru представлен полный список дилерских центров. ✔️Адреса, телефоны и самые свежие объявления от автосалонов',
        seoText: 'Продажа автомобилей у дилеров. Все свежие объявления по продаже и условия по техническому обслуживанию.',
        selectedMark: {
            id: 'BMW',
            name: 'BMW',
            count: 297,
            popular: true,
            'autoru-alias': 'bmw',
            'no-listing': false,
            reviews_count: 2165,
            logo: '//avatars.mds.yandex.net/get-verba/997355/2a00000170a99124c3a521a92b8cd4846660/logo',
            'big-logo': '//avatars.mds.yandex.net/get-verba/997355/2a00000170a99124c3a521a92b8cd4846660/dealer_logo',
            'cyrillic-name': 'БМВ',
            'autoru-id': '30',
            'vendor-ids': [ 4, 3, 15, 2, 0 ],
        },
        totalResultsCount: 1,
        searchRegion: {
            id: 1,
            latitude: 55.815792,
            linguistics: {
                ablative: '',
                accusative: 'Москву и Московскую область',
                dative: 'Москве и Московской области',
                directional: '',
                genitive: 'Москвы и Московской области',
                instrumental: 'Москвой и Московской областью',
                locative: '',
                nominative: 'Москва и Московская область',
                preposition: 'в',
                prepositional: 'Москве и Московской области',
            },
            longitude: 37.380031,
            name: 'Москва и Московская область',
            parent_id: 3,
            type: 5,
            geoAlias: 'moskovskaya_oblast',
        },
        mapBounds: [ [ 55.78969387764595, 37.25666585469622 ], [ 55.79369387764596, 37.26066585469623 ] ],
        filtersCount: 2,
        dealerNetName: 'Major',
        geo_radius: null,
        ads: {
            code: 'dealer-listing',
            settings: {
                c2: {
                    sources: [ {
                        type: 'adfox',
                        async: true,
                        internal: false,
                        placeId: '243420',
                        params: { pp: 'h', ps: 'cefr', p2: 'fhwz' },
                    } ], id: 'eehpfs2pw9',
                },
                c3: {
                    sources: [ {
                        type: 'direct',
                        sections: [ 'premium', 'direct' ],
                        method: 'getStaticContextDirectJSON',
                        params: { pageId: '151547' },
                        groupKey: 'direct',
                    } ], id: 'u9qh9c0txm',
                },
            },
            direct: { direct: { pageId: 151547, trafficFrom: 'direct' } },
            statId: '100',
        },
    },
    geo: {
        gidsInfo: [],
    },
    breadcrumbs: breadcrumbsPublicApiMock,
};

it('должен корректно отрендериться', () => {
    const Context = createContextProvider(contextMock);
    const store = mockStore(storeMock);

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <DealerBreadcrumbs/>
            </Provider>
        </Context>,
    ).dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен корректно отрендериться для мобильного', () => {
    const Context = createContextProvider(contextMock);
    const store = mockStore(storeMock);

    const tree = shallow(
        <Context>
            <Provider store={ store }>
                <DealerBreadcrumbs
                    isMobile={ true }
                />
            </Provider>
        </Context>,
    ).dive().dive();
    expect(shallowToJson(tree)).toMatchSnapshot();
});
