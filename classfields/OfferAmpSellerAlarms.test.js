const React = require('react');
const { shallow } = require('enzyme');

const OfferAmpSellerAlarms = require('./OfferAmpSellerAlarms');

it('должен отрендерить информацию про подменник для частника', () => {
    const offer = {
        additional_info: {},
        section: 'used',
        seller: {
            name: 'Рудольф',
            redirect_phones: true,
        },
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <OfferAmpSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.OfferAmpSellerAlarms__item_virtualPhone')).toHaveLength(1);
});

it('должен отрендерить информацию про DND, если есть флаг not_disturb', () => {
    const offer = {
        additional_info: {
            not_disturb: true,
        },
        seller: {},
        section: 'used',
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <OfferAmpSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.OfferAmpSellerAlarms__item_nodisturb')).toHaveLength(1);
});

it('должен отрендерить информацию про предоплату для частника', () => {
    const offer = {
        additional_info: {},
        section: 'used',
        seller: {},
        seller_type: 'PRIVATE',
    };

    const wrapper = shallow(
        <OfferAmpSellerAlarms
            offer={ offer }
        />,
    );

    expect(wrapper.find('.OfferAmpSellerAlarms__item_fraud')).toHaveLength(1);
});
