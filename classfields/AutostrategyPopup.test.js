const React = require('react');
const { shallow } = require('enzyme');
const AutostrategyPopup = require('./AutostrategyPopup').default;

const item = {
    context: { mark_code: 'CHERY',
        mark_ru: 'Чери',
        mark_name: 'Chery',
        model_code: 'BONUS',
        model_ru: 'Бонус',
        model_name: 'Bonus (A13)',
        region_id: '1',
    },
    base_price: 2000,
    one_step: 100,
    min_bid: 2100,
    auto_strategy: {
        auto_strategy_settings:
        { max_bid: '300', max_position_for_price: {},
        },
    },
};

it('Должен округлить сумму, когда пользователь кликает на кнопку плюс', async() => {
    const autostrategyPopup = shallow(<AutostrategyPopup
        item={ item }
        calculatorBunkerDictAutostrategyPopup={{}}
    />);

    autostrategyPopup.find('PriceToFilter').simulate('change', 5440, { name: 'price_to' });
    autostrategyPopup.find('Button').at(1).simulate('click');
    expect(autostrategyPopup.find('PriceToFilter').props().value).toBe(5500);
});

it('Должен округлить сумму, когда пользователь кликает на кнопку минус', async() => {
    const autostrategyPopup = shallow(<AutostrategyPopup
        item={ item }
        calculatorBunkerDictAutostrategyPopup={{}}
    />);

    autostrategyPopup.find('PriceToFilter').simulate('change', 5440, { name: 'price_to' });
    autostrategyPopup.find('Button').at(0).simulate('click');
    expect(autostrategyPopup.find('PriceToFilter').props().value).toBe(5400);
});

it('Должен установить цену, равную минимальной ставке + один шаг, ' +
    'если пользователь ввел вручную маленькую сумму и нажал кнопку плюс', async() => {
    const autostrategyPopup = shallow(<AutostrategyPopup
        item={ item }
        calculatorBunkerDictAutostrategyPopup={{}}
    />);

    autostrategyPopup.find('PriceToFilter').simulate('change', 200, { name: 'price_to' });
    autostrategyPopup.find('Button').at(1).simulate('click');
    expect(autostrategyPopup.find('PriceToFilter').props().value).toBe(2100);
});
