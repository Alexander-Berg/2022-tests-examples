const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');

const OfferMicroData = require('./OfferMicroData');

const offer = _.cloneDeep(cardMock);

it('должен отрендерить микроразметку Offer', () => {
    const wrapper = shallow(
        <OfferMicroData offer={ offer }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
