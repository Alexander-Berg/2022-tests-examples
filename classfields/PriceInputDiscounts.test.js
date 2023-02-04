const React = require('react');
const { shallow } = require('enzyme');

const PriceInputDiscounts = require('./PriceInputDiscounts');
const TextInput = require('auto-core/react/components/islands/TextInput');

const onChange = () => {};

it('рендерит пять полей скидок для LCV', () => {
    const tree = shallow(
        <PriceInputDiscounts
            category="LCV"
            currencySign="₽"
            discountOptions={{}}
            onChange={ onChange }
        />,
    );

    const placeholders = tree
        .find(TextInput)
        .map((node) => node.prop('placeholder'));

    expect(placeholders).toMatchSnapshot();
});

it('рендерит два поля скидок для не LCV', () => {
    const tree = shallow(
        <PriceInputDiscounts
            category="BUS"
            currencySign="₽"
            discountOptions={{}}
            onChange={ onChange }
        />,
    );

    const placeholders = tree
        .find(TextInput)
        .map((node) => node.prop('placeholder'));

    expect(placeholders).toMatchSnapshot();
});
