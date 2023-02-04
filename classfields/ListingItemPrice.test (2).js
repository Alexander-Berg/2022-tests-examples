const React = require('react');
const { render } = require('@testing-library/react');
const { Provider } = require('react-redux');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const { getBunkerMock } = require('autoru-frontend/mockData/state/bunker.mock');

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const card = require('auto-core/react/dataDomain/listing/mocks/listingOffer.cars.mock').default;

const ListingItemPrice = require('./ListingItemPrice');

const store = mockStore({
    bunker: getBunkerMock([ 'common/vas' ]),
});

const Context = createContextProvider(contextMock);

const renderComponent = (offer, props = {}) => {
    return render(
        <Context>
            <Provider store={ store }>
                <ListingItemPrice
                    offer={ offer }
                    highlighted={ props.highlighted }
                />
            </Provider>
        </Context>,
    );
};

describe('старое поведение (не эксп AUTORUFRONT-22049_show-exchange-popup)', () => {
    beforeAll(() => {
        contextMock.hasExperiment.mockImplementation(() => false);
    });

    describe('отрендерит попап цены', () => {
        it('если есть скидки', () => {
            const offerMock = cloneOfferWithHelpers(card).withDiscountOptions({ tradein: 1000, credit: 2000, max_discount: 5000 }).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeTruthy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });
    });

    describe('не отрендерит попап цены', () => {
        it('если нет скидки, но есть опция обмена и не хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).withExchange(true).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeFalsy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });

        it('есть опция обмена, но хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).withExchange(true).value();
            renderComponent(offerMock, { highlighted: true });

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeFalsy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeTruthy();
        });

        it('нет опции обмена и не хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeFalsy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });
    });
});

describe('эксп AUTORUFRONT-22049_show-exchange-popup', () => {
    beforeAll(() => {
        contextMock.hasExperiment.mockImplementation((exp) => exp === 'AUTORUFRONT-22049_show-exchange-popup');
    });

    describe('отрендерит попап цены', () => {
        it('если есть скидки', () => {
            const offerMock = cloneOfferWithHelpers(card).withDiscountOptions({ tradein: 1000, credit: 2000, max_discount: 5000 }).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeTruthy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });

        it('если нет скидки, но есть опция обмена и не хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).withExchange(true).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeTruthy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });
    });
    describe('не отрендерит попап цены', () => {
        it('есть опция обмена, но хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).withExchange(true).value();
            renderComponent(offerMock, { highlighted: true });

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeFalsy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeTruthy();
        });
        it('нет опции обмена и не хайлайтед', () => {
            const offerMock = cloneOfferWithHelpers(card).value();
            renderComponent(offerMock);

            expect(document.querySelector('.ListingItemPrice__pricePopup')).toBeFalsy();
            expect(document.querySelector('.ListingItemPrice__vasPopup')).toBeFalsy();
        });
    });
});
