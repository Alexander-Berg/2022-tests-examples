const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const DateMock = require('autoru-frontend/mocks/components/DateMock');

const CardMicroData = require('./CardMicroData');

let offer;
beforeEach(() => {
    offer = _.cloneDeep(cardMock);
    offer.state.image_urls[0].sizes['320x240'] = '//avatars.mds.yandex.net/get-autoru-vos/2175772/52498ffdbf2816143ca3d5a6506a2ef5/320x240';
    offer.additional_info = { exchange: true };
    offer.description = 'Тест. Тест.';
    offer.seller = {};
    offer.service_prices = {};
    offer.services = [];
});

it('должен отрендерить микроразметку', () => {
    const wrapper = shallow(
        <CardMicroData offer={ offer }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('не должен отрендерить микроразметку, если нет оффера', () => {
    const wrapper = shallow(
        <CardMicroData offer={ undefined }/>,
    );

    expect(shallowToJson(wrapper)).toHaveLength(0);
});

it('должен сгенерировать описание, если нет description', () => {
    // владение 1 год и 10 месяцев
    // в cardMock дата покупки 2/10/2017
    // стало быть ставим дату - август 2019
    const date = '2019-08-20';
    offer.description = '';

    const wrapper = shallow(
        <DateMock date={ date }>
            <CardMicroData offer={ offer }/>
        </DateMock>,
    ).find('CardMicroData').dive();

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
