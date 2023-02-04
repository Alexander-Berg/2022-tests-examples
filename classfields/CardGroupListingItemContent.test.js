const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const CardGroupListingItemContent = require('./CardGroupListingItemContent');

const OFFER = {
    category: 'cars',
    documents: {
        year: 2019,
    },
    color_hex: '97948F',
    vehicle_info: {
        availability: 'IN_STOCK',
        price_info: {
            RUR: 2260700,
        },
        color_hex: 200204,
        complectation: {
            name: 'Invite',
            available_options: [
                'cruise-control', 'alarm',
            ],
        },
        equipment: {
            'airbag-rear-side': true,
            alcantara: true,
            'adaptive-light': true,
            'automatic-lighting-control': true,
            'rear-camera': true,
        },
        tech_param: {
            displacement: 1596,
            engine_type: 'GASOLINE',
            gear_type: 'FORWARD_CONTROL',
            transmission: 'ROBOT',
            power: 122,
        },
    },
    seller: {
        name: 'Автомир MITSUBISHI Крылатское',
        location: {
            region_info: {
                name: 'Москва',
            },
        },
    },
};

it('должен корректно отрендерить содержимое сниппета', () => {
    const tree = shallow(
        <CardGroupListingItemContent
            offer={ OFFER }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
