const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const CardDescription = require('./CardDescription');

it('не должен ничего отредерить, если нет описание и бейджей', () => {
    const offer = cloneOfferWithHelpers({})
        .withSection('used')
        .value();

    const wrapper = shallow(
        <CardDescription
            offer={ offer }
        />,
    );
    expect(wrapper.type()).toBeNull();
});

it('должен отредерить описание без бейджей, если есть только описание', () => {
    const offer = cloneOfferWithHelpers({})
        .withDescription('test')
        .withSection('used')
        .value();

    const wrapper = shallow(
        <CardDescription
            offer={ offer }
        />,
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отредерить описание с бейджами, если все есть', () => {
    const offer = cloneOfferWithHelpers({})
        .withBadges([ 'badge' ])
        .withDescription('test')
        .withSection('used')
        .value();

    const wrapper = shallow(
        <CardDescription
            offer={ offer }
        />,
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
