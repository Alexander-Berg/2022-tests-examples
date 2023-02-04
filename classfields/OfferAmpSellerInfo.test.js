require('jest-enzyme');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const OfferAmpSellerInfo = require('./OfferAmpSellerInfo');

let store;
beforeEach(() => {
    store = mockStore({
        bunker: {},
        dealerCallback: { isVisible: false },
        cookie: {},
    });
});

describe('Компонент "такси" в AMP', () => {
    let offer;
    beforeEach(() => {
        offer = cloneOfferWithHelpers({
            seller: {
                location: {
                    coord: { latitude: 1, longitude: 1 },
                },
            },
        })
            .withStatus('ACTIVE');
    });

    it('должен отрендерить для невладельца, если есть координаты', async() => {
        const wrapper = shallow(
            <OfferAmpSellerInfo offer={ offer.value() }/>,
            { context: { ...contextMock, store } },
        );

        expect(wrapper.find('OfferAmpTaxiLink')).toExist();
    });
});
