const React = require('react');
const ListingItemPriceInfo = require('./ListingItemPriceInfo');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const store = mockStore({
    bunker: {},
    user: { data: {} },
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

const emptyGroupCard = require('autoru-frontend/mockData/state/emptyGroupCard.mock.js');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const offerFromHelper = cloneOfferWithHelpers(offerMock)
    .withIsOwner(false)
    .withCreditPrecondition();

it('должен отрендерить цену в рублях для обычного объвления', () => {
    const page = shallow(
        <ListingItemPriceInfo
            currency="RUR"
            offer={ offer }
            sort="fresh_relevance_1-desc"
        />,
        { context: { ...contextMock, store } },
    );

    const price = page.find('.ListingItemPriceInfo__price');
    expect(price.text()).toBe('100 000 ₽');
});

it('должен отрендерить цену в долларах для обычного объвления', () => {
    const page = shallow(
        <ListingItemPriceInfo
            currency="USD"
            offer={ offer }
            sort="fresh_relevance_1-desc"
        />,
        { context: { ...contextMock, store } },
    );

    const price = page.find('.ListingItemPriceInfo__price');
    expect(price.text()).toBe('1 000 $');
});

it('должен отрендерить цену c кредитным предложением', () => {
    const page = shallow(
        <ListingItemPriceInfo
            offer={ offerFromHelper }
            sort="fresh_relevance_1-desc"
        />,
        { context: { ...contextMock, store } },
    );

    const price = page.find('.ListingItemPriceInfo__price');
    expect(price.text()).toBe('855 000 ₽');
    const creditText = page.find('CreditPrice');
    expect(creditText).not.toBeEmptyRender();
});

it('должен отрендерить цену для пустой модели', () => {
    const page = shallow(
        <ListingItemPriceInfo
            currency="RUR"
            offer={ emptyGroupCard }
            sort="fresh_relevance_1-desc"
        />,
        { context: { ...contextMock, store } },
    );

    const price = page.find('.ListingItemPriceInfo__price');
    expect(price.text()).toBe('от 2 001 000 ₽');
});

it('не должен отрендериться для проданной тачки', () => {
    const page = shallow(
        <ListingItemPriceInfo
            currency="RUR"
            offer={{
                price_info: {},
            }}
            sort="fresh_relevance_1-desc"
        />,
        { context: { ...contextMock, store } },
    );

    expect(page.html()).toBeNull();
});
