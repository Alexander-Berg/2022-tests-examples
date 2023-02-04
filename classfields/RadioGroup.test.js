const React = require('react');
const { shallow } = require('enzyme');

const Radio = require('../Radio');
const RadioGroup = require('./RadioGroup');

it('dsf', () => {
    const fn = jest.fn();
    const component = (
        <RadioGroup
            size={ RadioGroup.SIZE.M }
            type={ RadioGroup.TYPE.BUTTON }
            value="value1"
            onChange={ fn }
        >
            <Radio value="value1">Марки</Radio>
            <Radio value="value2">Помощник</Radio>
        </RadioGroup>
    );

    const wrapper = shallow(component);
    wrapper.find({ value: 'value2' }).simulate('check', null, { value: 'value2' });

    expect(fn.mock.calls).toHaveLength(1);
    expect(fn.mock.calls[0][0]).toEqual('value2');
});

it('RadioGroup с mode=RADIO_CHECK должен уметь снимать выделение', () => {
    const fn = jest.fn();
    const component = (
        <RadioGroup
            size={ RadioGroup.SIZE.M }
            type={ RadioGroup.TYPE.BUTTON }
            mode={ RadioGroup.MODE.RADIO_CHECK }
            value="value1"
            onChange={ fn }
        >
            <Radio value="value1">Марки</Radio>
            <Radio value="value2">Помощник</Radio>
        </RadioGroup>
    );

    const wrapper = shallow(component);
    wrapper.find({ value: 'value1' }).simulate('check', null, { value: 'value1' });

    expect(fn.mock.calls).toHaveLength(1);
    expect(fn.mock.calls[0][0]).toEqual('');
});

it('RadioGroup с mode=RADIO не должен сбрасывать значение при повторном клике', () => {
    const fn = jest.fn();
    const component = (
        <RadioGroup
            size={ RadioGroup.SIZE.M }
            type={ RadioGroup.TYPE.BUTTON }
            value="value1"
            onChange={ fn }
        >
            <Radio value="value1">Марки</Radio>
            <Radio value="value2">Помощник</Radio>
        </RadioGroup>
    );

    const wrapper = shallow(component);
    wrapper.find({ value: 'value1' }).simulate('check', null, { value: 'value1' });

    expect(fn.mock.calls).toHaveLength(0);
});
