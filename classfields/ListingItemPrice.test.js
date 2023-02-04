const React = require('react');
const { shallow } = require('enzyme');

const ListingItemPrice = require('./ListingItemPrice');

it('renders correctly', () => {
    const tree = shallow(
        <ListingItemPrice
            className="test"
            price={{ currency: 'RUR', value: 100000 }}
        />,
    );

    expect(tree.find('.test').text()).toEqual('100 000 ₽');
});
