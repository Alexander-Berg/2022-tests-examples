const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const React = require('react');
const { Provider } = require('react-redux');
const _ = require('lodash');

const PriceNewOffer = require('./PriceNewOffer');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const offerMock = require('autoru-frontend/mockData/state/newCard.mock');
const offerUsedMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const Context = createContextProvider(contextMock);

const baseState = {
    banks: {
        creditConfig: {},
    },
};

let offer;

beforeEach(() => {
    offer = _.cloneDeep(offerMock.card);
});

const TESTS = [
    {
        description: 'должна корректно отрендериться одна цена',
        priceInfo: {
            RUR: 2260700,
        },
    },
    {
        description: 'должен корректно отрендериться диапазон цен без списка скидок',
        priceInfo: {
            RUR: 2260700,
        },
        discountOptions: {
            max_discount: 715700,
        },
        showDiscounts: PriceNewOffer.SHOW_DISCOUNTS.NEVER,
    },
    {
        description: 'должен корректно отрендериться диапазон цен co списком скидок',
        priceInfo: {
            RUR: 2260700,
        },
        discountOptions: {
            max_discount: 715700,
        },
        showDiscounts: PriceNewOffer.SHOW_DISCOUNTS.ALWAYS,
    },
    {
        description: 'должна корректно отрендериться одна цена co списком скидок',
        priceInfo: {
            RUR: 2260700,
        },
        discountOptions: {
            tradein: 715700,
        },
        showDiscounts: PriceNewOffer.SHOW_DISCOUNTS.ALWAYS,
    },
];

TESTS.forEach(({ description, discountOptions, priceInfo, showDiscounts }) => {
    it(description, () => {
        offer.discount_options = discountOptions;
        offer.price_info = priceInfo;

        const tree = shallow(
            <Context>
                <PriceNewOffer
                    offer={ offer }
                    showDiscounts={ showDiscounts }
                />
            </Context>,
        ).dive();
        expect(shallowToJson(tree, { map: hideOfferMock })).toMatchSnapshot();
    });
});

it('должен отрендерить список скидок только в попапе', () => {
    offer.discount_options = { max_discount: 715700 };
    offer.price_info = { RUR: 715700 };

    const tree = shallow(
        <Context>
            <PriceNewOffer
                offer={ offer }
                showDiscounts={ PriceNewOffer.SHOW_DISCOUNTS.ONLY_IN_POPUP }
            />
        </Context>,
    ).dive();
    tree.setState({ showPopup: true });

    expect(tree.find('.PriceNewOffer__discountList')).toHaveLength(1);
    expect(tree.find('.PriceNewOffer__popup .PriceNewOffer__discountList')).toHaveLength(1);
});

describe('программа господдержки', () => {
    it('покажет скидку', () => {
        const Context = createContextProvider(contextMock);
        const store = mockStore(baseState);
        const offer = cloneOfferWithHelpers(offerUsedMock).withSection('new').value();

        const wrapper = shallow(
            <Context>
                <Provider store={ store }>
                    <PriceNewOffer
                        offer={ offer }
                        offerUrl="url"
                        onOfferLinkClick={ _.noop }
                        hasStateSupport={ true }
                        hasAnyDiscount={ false }
                    />
                </Provider>
            </Context>,
        ).dive().dive();

        const badge = wrapper.find('StateSupportBadge');
        const noDiscount = wrapper.find('.PriceNewOffer__noDiscount');

        expect(badge.isEmptyRender()).toBe(false);
        expect(noDiscount.isEmptyRender()).toBe(true);
    });
});

function hideOfferMock(json) {
    if (json.props && json.props.offer) {
        const originalMockHash = JSON.stringify(offer);
        const actualMockHash = JSON.stringify(json.props.offer);

        if (originalMockHash === actualMockHash) {
            return {
                ...json,
                props: {
                    ...json.props, offer: '[Offer mock]',
                },
            };

        } else {
            return json;
        }
    }

    return json;
}
