const React = require('react');

const SaleServiceTurbo = require('./SaleServiceTurbo');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const { noop } = require('lodash');

it('render тест: должен вернуть корректный элемент', () => {
    expect(shallowToJson(shallow(<SaleServiceTurbo
        serviceTurbo={{ active: true }}
        applyService={ noop }
        offerID="1089867200-124f21d9"
    />))).toMatchSnapshot();
});

it('onActivate тест: должен вызвать confirm с корректными параметрами', () => {
    const confirm = jest.fn();
    const saleTurbo = shallow(<SaleServiceTurbo
        confirm={ confirm }
    />);
    saleTurbo.instance().onActivate();

    expect(confirm).toHaveBeenCalledWith({
        visible: true,
        onOk: saleTurbo.instance().onActivateOk,
        text: 'Подключить услугу "Турбо-продажа"?',
    });
});

it('onActivateOk тест: должен вызвать confirm и applyService с корректными параметрами', () => {
    const confirm = jest.fn();
    const applyService = jest.fn();
    const saleTurbo = shallow(<SaleServiceTurbo
        offerID="111-222"
        confirm={ confirm }
        applyService={ applyService }
        category="cars"
    />);
    saleTurbo.instance().onActivateOk();

    expect(confirm).toHaveBeenCalledWith({
        visible: false,
    });

    expect(applyService).toHaveBeenCalledWith({
        service: 'turbo',
        offerID: '111-222',
        isActivate: true,
    });
});
