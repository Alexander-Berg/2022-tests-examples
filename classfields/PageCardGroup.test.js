const React = require('react');
const { shallow } = require('enzyme');

const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cardGroupComplectations = require('autoru-frontend/mockData/state/cardGroupComplectations.mock');
const configStateMock = require('auto-core/react/dataDomain/config/mock').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

let pageParams;
let store;
beforeEach(() => {
    pageParams = {
        category: 'cars',
        section: 'new',
        catalog_filter: [ { mark: 'RENAULT', model: 'KAPTUR', generation: '20780680', configuration: '20780824' } ],
    };
    store = {
        config: configStateMock.withPageParams(pageParams).value(),
        bunker: getBunkerMock([ 'common/metrics' ]),
        ...cardGroupComplectations,
        listing: {
            data: {
                price_range: {
                    min: {
                        price: 1827000,
                        currency: 'RUR',
                    },
                },
                pagination: {
                    total_offers_count: 1,
                },
            },
            searchID: 'searchID',
        },
        searchID: {
            searchID: 'searchID',
            parentSearchId: undefined,
        },
    };
});

const PageCardGroup = require('./PageCardGroup').default;

it('рендерит компонент CardGroupViewEvents с правильными пропами', () => {
    store.listing.data.pagination.total_offers_count = 10;
    const wrapper = shallow(
        <PageCardGroup
            store={ mockStore(store) }
        />,
        { context: contextMock },
    ).dive();
    const cardGroupViewEvents = wrapper.find('CardGroupViewEvents');

    expect(cardGroupViewEvents).toHaveProp({
        category: 'cars',
        groupingId: 'mark=RENAULT,model=KAPTUR,generation=20780680,configuration=20780824',
        groupSize: 10,
        offer: undefined,
        searchID: 'searchID',
        section: 'new',
    });
});
