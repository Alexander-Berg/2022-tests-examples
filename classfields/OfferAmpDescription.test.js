const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const OfferAmpDescription = require('./OfferAmpDescription');

it('не должен ничего отредерить, если нет описание и бейджей', () => {
    const offer = cloneOfferWithHelpers({})
        .withSection('used')
        .value();

    const wrapper = shallow(
        <OfferAmpDescription
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
        <OfferAmpDescription
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
        <OfferAmpDescription
            offer={ offer }
        />,
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отредерить описание с кнопкой "Показать еще", если много текста', () => {
    const offer = cloneOfferWithHelpers({})
        // eslint-disable-next-line max-len
        .withDescription('Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.')
        .withSection('used')
        .value();

    const wrapper = shallow(
        <OfferAmpDescription
            offer={ offer }
        />,
    );
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
