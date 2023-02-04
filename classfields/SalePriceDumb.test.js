const React = require('react');
const { shallow } = require('enzyme');
const { Provider } = require('react-redux');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const SalePriceDumb = require('./SalePriceDumb').default;
const SalePricePopupEditContent = require('./SalePricePopupEditContent/SalePricePopupEditContent').default;

const OFFER_PRICE = 1000000;
const MARKET_PRICE = 1200000;
const offer = cloneOfferWithHelpers(cardMock)
    .withPrice(OFFER_PRICE)
    .withMarketPrice({ price: MARKET_PRICE })
    .value();

const defaultProps = {
    canWriteSaleResource: true,
    boostPrice: 200,
    applyService: () => {},
    showErrorNotification: () => {},
    offer,
};

it('покажет попап с ценой и скидками после клика на иконку редактирования', () => {
    const salePrice = shallowRenderComponent();
    salePrice.setState({ isEditing: true });

    expect(salePrice.find(SalePricePopupEditContent)).toExist();
});

it('покажет правильную подпись, если цена ниже рыночной', () => {
    const salePrice = shallowRenderComponent();

    const noteText = salePrice.find('.SalePrice__note').text();

    expect(noteText).toEqual(expect.stringContaining('<IconSvg />Ниже рынка'));
});

it('покажет правильную подпись, если цена выше рыночной', () => {
    const offerPrice = MARKET_PRICE + 100000;
    const salePrice = shallowRenderComponent({
        ...defaultProps,
        offer: cloneOfferWithHelpers(cardMock)
            .withPrice(offerPrice)
            .withMarketPrice({ price: MARKET_PRICE })
            .value(),
    });
    const noteText = salePrice.find('.SalePrice__note').text();

    expect(noteText).toEqual(expect.stringContaining('<IconSvg />Выше рынка'));
});

describe('не покажет сравнений с рыночной ценой, если', () => {
    it('нет рыночной цены', () => {
        const salePrice = shallowRenderComponent({
            ...defaultProps,
            offer: cloneOfferWithHelpers(cardMock)
                .withPrice(OFFER_PRICE)
                .withMarketPrice({})
                .value(),
        });

        expect(salePrice.find('.SalePrice__icon_comparision')).not.toExist();
        expect(salePrice.find('.SalePrice__note')).not.toExist();
    });

    it('цена равна рыночной', () => {
        const salePrice = shallowRenderComponent({
            ...defaultProps,
            offer: cloneOfferWithHelpers(cardMock)
                .withPrice(MARKET_PRICE)
                .withMarketPrice({ price: MARKET_PRICE })
                .value(),
        });

        expect(salePrice.find('.SalePrice__icon_comparision')).not.toExist();
        expect(salePrice.find('.SalePrice__note')).not.toExist();
    });

    it('у оффера есть бейдж хорошая-цена', () => {
        const salePrice = shallowRenderComponent({
            ...defaultProps,
            offer: cloneOfferWithHelpers(cardMock)
                .withTags([ 'good_price' ])
                .withPrice(OFFER_PRICE)
                .withMarketPrice({ price: MARKET_PRICE })
                .value(),
        });

        expect(salePrice.find('.SalePrice__icon_comparision')).not.toExist();
        expect(salePrice.find('.SalePrice__note')).not.toExist();
    });

    it('у оффера есть бейдж отличная-цена', () => {
        const salePrice = shallowRenderComponent({
            ...defaultProps,
            offer: cloneOfferWithHelpers(cardMock)
                .withTags([ 'excellent_price' ])
                .withPrice(OFFER_PRICE)
                .withMarketPrice({ price: MARKET_PRICE })
                .value(),
        });

        expect(salePrice.find('.SalePrice__icon_comparision')).not.toExist();
        expect(salePrice.find('.SalePrice__note')).not.toExist();
    });
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <Provider store={ mockStore({}) }>
            <SalePriceDumb { ...props } marketIsStable={ true }/>
        </Provider>,
        { context: contextMock },
    ).dive();
}
