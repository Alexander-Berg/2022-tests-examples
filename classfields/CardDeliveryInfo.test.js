jest.mock('auto-core/react/dataDomain/state/actions/sellerPopupOpen', () => {
    return jest.fn(() => ({ type: 'sellerPopupOpen_action' }));
});

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const mockStore = require('autoru-frontend/mocks/mockStore').default;

const CardDeliveryInfo = require('./CardDeliveryInfo');

const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

let store;
beforeEach(() => {
    store = mockStore({});
});

it('должен отрендерить виджет доставки', () => {
    const extendedOffer = _.cloneDeep(offer);

    extendedOffer.delivery_info = {
        delivery_regions: [
            {
                location: {
                    region_info: {
                        accusative: 'Москву',
                    },
                },
            },
            {
                location: {
                    region_info: {
                        accusative: 'Воронеж',
                    },
                },
            },
            {
                location: {
                    region_info: {
                        accusative: 'Самару',
                    },
                },
            },
        ],
    };

    const wrapper = shallow(<CardDeliveryInfo offer={ extendedOffer }/>, { context: { store } });
    expect(shallowToJson(wrapper.dive())).toMatchSnapshot();
});
