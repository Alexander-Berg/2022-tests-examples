const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const MockDate = require('mockdate');

const OfferAmpDatesAndStats = require('./OfferAmpDatesAndStats');

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');
const offerMock = require('auto-core/react/dataDomain/card/mocks/card.cars.2.mock.ts');

beforeEach(() => {
    MockDate.set('2020-04-17');
});

it('должен отрендерить дату и количество происмотров, если есть данные', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withIsOwner(false)
        .withCounters()
        .withCreationDate(1586581933000)
        .value();

    const wrapper = shallow(<OfferAmpDatesAndStats offer={ offer }/>);

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить дату без количества происмотров, если нет данных', () => {
    const offer = cloneOfferWithHelpers(offerMock)
        .withIsOwner(false)
        .withCounters({ all: 0, daily: 0 })
        .withCreationDate(1586581933000)
        .value();

    const wrapper = shallow(<OfferAmpDatesAndStats offer={ offer }/>);

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
