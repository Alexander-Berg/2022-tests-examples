/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import { mount } from 'enzyme';

const React = require('react');
const { shallow } = require('enzyme');
const numberRefiner = require('auto-core/fields/number-integer/number-integer-refiner');

const TextInput = require('./TextInput');

beforeEach(() => {
    jest.useFakeTimers();
});

afterEach(() => {
    jest.clearAllTimers();
});

describe('обработка изменения инпута', () => {
    let TextInputProps;
    let onChange;

    beforeEach(() => {
        onChange = jest.fn();

        TextInputProps = {
            onChange: onChange,
            radius: 'r8',
            size: TextInput.SIZE.L,
            type: 'text',
            valueProcessor: TextInput.defaultProps.valueProcessor,
        };
    });

    it('должен вызвать onChange на изменение', () => {
        const props = {
            ...TextInputProps,
        };

        const { textinput } = shallowRenderComponent({ props });
        textinput.find('input').simulate('change', { target: { value: '1234' } });

        expect(onChange).toHaveBeenCalledWith('1234', { ...props, value: '' });
    });

    it('не должен вызвать onChange на изменение, если disabled=true', () => {
        const props = {
            ...TextInputProps,
            disabled: true,
        };

        const { textinput } = shallowRenderComponent({ props });
        textinput.find('input').simulate('change', { target: { value: '1234' } });

        expect(onChange).not.toHaveBeenCalled();
    });

    it('должен вызвать onChange после обработки value через процессор', () => {
        const props = {
            ...TextInputProps,
            value: '1',
            valueProcessor: numberRefiner,
        };

        const { textinput } = shallowRenderComponent({ props });
        textinput.find('input').simulate('change', { target: { value: 'foo12bar' } });

        expect(onChange).toHaveBeenCalledWith(12, props);
    });

    it('не должен вызвать onChange, если после обработки value через процессор значение не изменилось', () => {
        const props = {
            ...TextInputProps,
            value: '1',
            valueProcessor: numberRefiner,
        };

        const { textinput } = shallowRenderComponent({ props });
        textinput.find('input').simulate('change', { target: { value: '1а' } });

        expect(onChange).not.toHaveBeenCalled();
    });
});

describe('если инпут маскированный, поставит каретку в правильное положение', () => {
    let props;
    const caretPos = 4;

    beforeEach(() => {
        props = {
            size: TextInput.SIZE.L,
            type: 'text',
            onChange: () => {},
            valueProcessor: (value) => {
                const clearedValue = value.replace(/\D/g, '');
                if (clearedValue) {
                    return Number(clearedValue);
                } else {
                    return '';
                }
            },
            value: '123 45',
        };
    });

    it('если пользователь удаляет разделитель', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.getControl().selectionStart = caretPos;
        textinput.find('input').simulate('change', { target: { value: '12345' } });
        jest.runAllTimers();

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos, caretPos);
    });

    it('если пользователь вводит некорректный символ', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.getControl().selectionStart = caretPos;
        textinput.find('input').simulate('change', { target: { value: '123 a45' } });
        jest.runAllTimers();

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos - 1, caretPos - 1);
    });

    it('если пользователь удаляет символ в середине строки без уменьшения разряда', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = caretPos;
        textinput.setProps({ ...props, value: '123 5' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos, caretPos);
    });

    it('если пользователь удаляет символ в середине строки с уменьшением разряда', () => {
        props.value = '1 234 567';
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = caretPos;
        textinput.setProps({ ...props, value: '124 567' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos - 1, caretPos - 1);
    });

    it('если пользователь добавляет символ в середине строки без увеличения разряда', () => {
        props.value = '1 234 567';
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = caretPos;
        textinput.setProps({ ...props, value: '12 334 567' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos, caretPos);
    });

    it('если пользователь добавляет символ в середине строки с увеличением разряда', () => {
        props.value = '123 456';
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = caretPos;
        textinput.setProps({ ...props, value: '1 234 456' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(1);
        expect(instance.getControl().setSelectionRange).toHaveBeenCalledWith(caretPos + 1, caretPos + 1);
    });

    it('не будет форсить изменение положения каретки если символ добавляется в конце', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = 7;
        textinput.setProps({ ...props, value: '123 456' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(0);
    });

    it('не будет форсить изменение положения каретки если символ добавляется в конце и увеличивается разряд', () => {
        props.value = '123 456';
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.caretPosition = 8;
        textinput.setProps({ ...props, value: '1 234 567' });

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(0);
    });
});

describe('если инпут не маскированный, не будет форсить изменение положения каретки', () => {
    let props;
    const caretPos = 4;

    beforeEach(() => {
        props = {
            size: TextInput.SIZE.L,
            type: 'text',
            onChange: () => { },
            value: '12345',
        };
    });

    it('если пользователь удаляет символ', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.getControl().selectionStart = caretPos;
        textinput.find('input').simulate('change', { target: { value: '1235' } });
        jest.runAllTimers();

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(0);
    });

    it('если пользователь добавляет символ', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.getControl().selectionStart = caretPos;
        textinput.find('input').simulate('change', { target: { value: '123495' } });
        jest.runAllTimers();

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(0);
    });

    it('если тригерится событие onChange но значение не изменилось (на всякий случай)', () => {
        const { instance, textinput } = shallowRenderComponent({ props });
        instance.getControl().selectionStart = caretPos;
        textinput.find('input').simulate('change', { target: { value: '12345' } });
        jest.runAllTimers();

        expect(instance.getControl().setSelectionRange).toHaveBeenCalledTimes(0);
    });
});

it('если значение не передано передаст в инпут пустую строку', () => {
    const props = {
        size: TextInput.SIZE.L,
        type: 'text',
        onChange: () => { },
        value: '12345',
    };

    const { textinput } = shallowRenderComponent({ props });
    textinput.setProps({ ...props, value: undefined });

    expect(textinput.find('input').prop('value')).toBe('');
});

describe('hasClear', () => {
    it('должен очистить input и оставить фокус внутри', () => {
        const onChange = jest.fn();
        const onClearClick = jest.fn();
        const onFocusChange = jest.fn();
        const valueProcessor = (value) => value;

        const props = { hasClear: true, onChange, onClearClick, onFocusChange, radius: 'r8', size: 'l', type: 'text', value: '123', valueProcessor };

        const wrapper = mount(
            <TextInput
                hasClear={ true }
                onFocusChange={ onFocusChange }
                onChange={ onChange }
                onClearClick={ onClearClick }
                value="123"
                valueProcessor={ valueProcessor }
            />,
        );

        const clear = wrapper.find('.TextInput__clear');
        clear.simulate('mouseDown');

        expect(onChange).toHaveBeenCalledTimes(1);
        expect(onChange).toHaveBeenCalledWith('', props, { source: 'clear' });

        expect(onFocusChange).toHaveBeenCalledTimes(1);
        expect(onFocusChange).toHaveBeenCalledWith('hard', props);
    });
});

function shallowRenderComponent({ props }) {
    const textinput = shallow(<TextInput { ...props }/>);
    const instance = textinput.instance();
    instance.control = {
        current: {
            selectionStart: 0,
            setSelectionRange: jest.fn(),
        },
    };

    return { textinput, instance };
}
