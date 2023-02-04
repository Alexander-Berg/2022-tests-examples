const React = require('react');
const { shallow } = require('enzyme');

const CardSellerAlarms = require('./CardSellerAlarms');

it('должен отрендерить информацию про подменник для частника', () => {
    const offer = {
        category: 'cars',
        section: 'used',
        additional_info: {},
        seller: {
            name: 'Рудольф',
            redirect_phones: true,
        },
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <CardSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.CardSellerAlarms__item_virtualPhone')).toHaveLength(1);
});

it('должен отрендерить информацию про DND, если есть флаг not_disturb', () => {
    const offer = {
        category: 'cars',
        additional_info: {
            not_disturb: true,
        },
        seller: {},
        section: 'used',
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <CardSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.CardSellerAlarms__item_nodisturb')).toHaveLength(1);
});

it('должен отрендерить информацию про предоплату для частника', () => {
    const offer = {
        additional_info: {},
        seller: {},
        category: 'cars',
        section: 'used',
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <CardSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.CardSellerAlarms__item_fraud')).toHaveLength(1);
});
