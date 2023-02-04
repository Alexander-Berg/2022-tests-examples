const React = require('react');
const { Provider } = require('react-redux');
const { shallow } = require('enzyme');

const groupCard = require('autoru-frontend/mockData/state/groupCard.mock');
const emptyGroupCard = require('autoru-frontend/mockData/state/emptyGroupCard.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const configMock = require('auto-core/react/dataDomain/config/mock').default;

const geoMock = require('auto-core/react/dataDomain/geo/mocks/geo.mock');

const ListingItemBig = require('./ListingItemBig');

const store = mockStore({
    geo: geoMock,
    config: configMock,
});

it('должен корректно сформировать ссылку на группу для группы с офферами', () => {
    const tree = shallow(
        <Provider store={ store }>
            <ListingItemBig
                offer={ groupCard }
                groupSize={ 12 }
                isGroupItem
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    const link = tree.find('Button');
    expect(link).toHaveProp('url', 'link/card-group/?section=new&category=cars&mark=BMW&model=3ER&configuration_id=21398651&super_gen=21398591');
});

it('должен корректно сформировать ссылку на группу для пустой группы', () => {
    const tree = shallow(
        <Provider store={ store }>
            <ListingItemBig
                offer={ emptyGroupCard }
                groupSize={ 0 }
                isGroupItem
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    const link = tree.find('Button');
    expect(link).toHaveProp('url', 'link/card-group/?section=new&category=cars&mark=BMW&model=X1&configuration_id=20583371&super_gen=20583308');
});

it('должен корректно сформировать ссылку на оффер, если в группе только один оффер', () => {
    const offer = cloneOfferWithHelpers(groupCard)
        .withGroupingInfo({
            ...groupCard.groupping_info,
            size: 1,
        })
        .value();

    const tree = shallow(
        <Provider store={ store }>
            <ListingItemBig
                offer={ offer }
                isGroupItem
                groupSize={ 1 }
            />
        </Provider>,
        { context: contextMock },
    ).dive().dive();

    const link = tree.find('Button');
    // eslint-disable-next-line max-len
    expect(link).toHaveProp('url', 'link/card/?category=cars&section=new&mark=BMW&model=3ER&sale_id=1091506272&sale_hash=62e5b1b8&tech_param_id=21605511&complectation_id=21606157');
});
