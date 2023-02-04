const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const OfferAmpDeliveryInfo = require('./OfferAmpDeliveryInfo');

const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

it('должен отрендерить виджет доставки в AMP', () => {
    const extendedOffer = cloneOfferWithHelpers(offer).withDelivery().value();

    const tree = shallow(<OfferAmpDeliveryInfo offer={ extendedOffer }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен отрендерить виджет доставки в AMP c текстом "и ещё 1 город"', () => {
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

    const tree = shallow(<OfferAmpDeliveryInfo offer={ extendedOffer }/>);
    expect(shallowToJson(tree)).toMatchSnapshot();
});
