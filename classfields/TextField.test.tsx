import React from 'react';
import { shallow } from 'enzyme';

import type { TextFieldProps, OnChangeHandler } from './TextField';
import TextField from './TextField';

describe('параметры', () => {
    let onChange: jest.Mock<OnChangeHandler>;
    let DefaultTextFieldProps: TextFieldProps;

    beforeEach(() => {
        onChange = jest.fn();

        DefaultTextFieldProps = {
            onChange: onChange,
            name: 'test',
        };
    });

    it('дефолтные атрибуты и пропсы', () => {
        const textField = shallow(
            <TextField
                { ...DefaultTextFieldProps }
            />,
        );

        expect(textField).toMatchSnapshot();
    });

    it('атрибуты и пропсы для рут элемента и инпута', () => {
        const textField = shallow(
            <TextField
                { ...DefaultTextFieldProps }
                attributes={{
                    id: 'kek',
                    title: 'Мы избалованы потому, что не ценим те вещи, что у нас есть. (с) Офис',
                }}
                inputAttributes={{
                    type: 'password',
                    placeholder: 'Привет!',
                    className: 'InputTest',
                }}
                id="Mag"
            />,
        );

        expect(textField).toMatchSnapshot();
    });
});

describe('обработка изменения инпута', () => {
    let onChange: jest.Mock<OnChangeHandler>;
    let DefaultTextFieldProps: TextFieldProps;

    beforeEach(() => {
        onChange = jest.fn();

        DefaultTextFieldProps = {
            onChange: onChange,
            name: 'test',
        };
    });

    it('должен вызвать onChange на изменение', () => {
        const textField = shallow(
            <TextField
                { ...DefaultTextFieldProps }
            />,
        );
        textField.find('input').simulate('change', { currentTarget: { value: '1234' } });

        expect(onChange.mock.calls[0]).toMatchSnapshot();
    });

    it('не должен вызвать onChange на изменение, если disabled=true', () => {
        const textField = shallow(
            <TextField
                { ...DefaultTextFieldProps }
                disabled
            />,
        );
        textField.find('input').simulate('change', { currentTarget: { value: '1234' } });

        expect(onChange).not.toHaveBeenCalled();
    });
});
