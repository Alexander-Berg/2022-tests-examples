const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardGroupListingItemPhoneButton = require('./CardGroupListingItemPhoneButton');

it('должен корректно отрендерится без показа номера телефона', () => {
    const tree = shallow(
        <CardGroupListingItemPhoneButton
            phone=""
            onPhonePopup={ jest.fn }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен корректно отрендерится с показом номера телефона', () => {
    const tree = shallow(
        <CardGroupListingItemPhoneButton
            phone="+7 499 938-56-03"
            onPhonePopup={ jest.fn }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
