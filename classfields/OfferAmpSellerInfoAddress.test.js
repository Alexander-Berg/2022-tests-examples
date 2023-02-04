const React = require('react');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const OfferAmpSellerInfoAddress = require('./OfferAmpSellerInfoAddress');

it('должен отрендерить адрес', () => {
    const offer = {
        seller: {
            location: {
                address: 'Льва Толстого, 16',
            },
        },
    };

    const wrapper = shallow(
        <OfferAmpSellerInfoAddress offer={ offer }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрендерить адрес и регион', () => {
    const offer = {
        seller: {
            location: {
                address: 'Льва Толстого, 16',
                region_info: {
                    id: '213',
                    name: 'Москва',
                    latitude: 55.753215,
                    longitude: 37.622504,
                },
            },
        },
    };

    const wrapper = shallow(
        <OfferAmpSellerInfoAddress offer={ offer } renderRegionName={ true }/>,
    );

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
