const React = require('react');
const ListingAmpItemPriceInfo = require('./ListingAmpItemPriceInfo');
const renderer = require('react-test-renderer');
const { Provider } = require('react-redux');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const ContextProvider = createContextProvider(contextMock);
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const store = mockStore({
    bunker: {},
});
const offer = {
    hash: 'abc',
    id: 123,
    status: 'ACTIVE',
    section: 'used',
    category: 'cars',
    additional_info: { is_owner: false },
    price_info: { RUR: 100000, USD: 1000 },
};
const offerMock = require('auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock').default;
const offerFromHelper = cloneOfferWithHelpers(offerMock)
    .withIsOwner(false)
    .withCreditPrecondition();

it('должен отрендерить цену в рублях для обычного объвления', () => {
    const tree = renderer.create(
        <ContextProvider>
            <Provider store={ store }>
                <ListingAmpItemPriceInfo
                    currency="RUR"
                    offer={ offer }
                    sort="fresh_relevance_1-desc"
                />
            </Provider>
        </ContextProvider>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен отрендерить цену в долларах для обычного объвления', () => {
    const tree = renderer.create(
        <ContextProvider>
            <Provider store={ store }>
                <ListingAmpItemPriceInfo
                    currency="USD"
                    offer={ offer }
                    sort="fresh_relevance_1-desc"
                />
            </Provider>
        </ContextProvider>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('должен отрендерить цену c кредтиным предложением', () => {
    const tree = renderer.create(
        <ContextProvider>
            <Provider store={ store }>
                <ListingAmpItemPriceInfo
                    offer={ offerFromHelper }
                    sort="fresh_relevance_1-desc"
                />
            </Provider>
        </ContextProvider>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});

it('не должен отрендериться для проданной тачки', () => {
    const tree = renderer.create(
        <ContextProvider>
            <Provider store={ store }>
                <ListingAmpItemPriceInfo
                    currency="RUR"
                    offer={{
                        price_info: {},
                    }}
                    sort="fresh_relevance_1-desc"
                />
            </Provider>
        </ContextProvider>,
    ).toJSON();
    expect(tree).toMatchSnapshot();
});
