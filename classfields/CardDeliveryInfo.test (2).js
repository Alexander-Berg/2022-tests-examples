const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CardDeliveryInfo = require('./CardDeliveryInfo');

const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

it('должен отрендерить виджет доставки', () => {
    const extendedOffer = cloneOfferWithHelpers(offer).withDelivery().value();

    const tree = shallow(<CardDeliveryInfo offer={ extendedOffer }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить виджет доставки c ссылкой "и ещё 1 город"', () => {
    const extendedOffer = _.cloneDeep(offer);

    extendedOffer.delivery_info = {
        delivery_regions: [
            {
                location: { region_info: { accusative: 'Москву' } },
            },
            {
                location: { region_info: { accusative: 'Барнаул' } },
            },
            {
                location: { region_info: { accusative: 'Рим' } },
            },
            {
                location: { region_info: { accusative: 'Камчатку' } },
            },
        ],
    };

    const tree = shallow(<CardDeliveryInfo offer={ extendedOffer }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});
