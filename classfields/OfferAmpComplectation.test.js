require('jest-enzyme');
const React = require('react');
const { shallow } = require('enzyme');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const yandexAutoCars = require('autoru-frontend/mockData/bunker/common/yandex_auto_cars.json');

const OfferAmpComplectation = require('./OfferAmpComplectation');

let offer;
let store;

beforeEach(() => {
    offer = cloneOfferWithHelpers({})
        .withMarkInfo({ code: 'RENAULT', name: 'RENAULT' })
        .withMilage(100)
        .withModelInfo({ code: 'DUSTER', name: 'DUSTER' })
        .withSellerGeoParentsIds([ '1', '10174' ])
        .withYear(2018)
        .value();
    offer.vehicle_info.equipmentGroups = [
        {
            name: 'Мультимедиа',
            values: [ 'Система «старт-стоп»' ],
        },
    ];

    store = mockStore({
        bunker: {
            'common/yandex_auto_cars': yandexAutoCars,
        },
    });
});

it('должен отрисовать плашку Яндес.Авто в AMP, если объвление подходит по условие', () => {
    const wrapper = shallow(
        <OfferAmpComplectation
            offer={ offer }
        />,
        { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('OfferAmpComplectationTitleBanner')).toExist();
});

it('не должен отрисовать плашку Яндес.Авто в AMP, если объвление не подходит по условие', () => {
    offer = cloneOfferWithHelpers(offer)
        .withModelInfo({ code: 'DUSTER1', name: 'DUSTER1' })
        .value();

    const wrapper = shallow(
        <OfferAmpComplectation
            offer={ offer }
        />,
        { context: { ...contextMock, store } },
    ).dive();

    expect(wrapper.find('OfferAmpComplectationTitleBanner')).not.toExist();
});
