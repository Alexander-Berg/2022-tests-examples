import * as React from 'react';
import { mount } from 'enzyme';

import ControlTextInput from '../';

describe('ControlTextInput', () => {
    describe('with "text-numeric" type', () => {
        const onChange = jest.fn();
        const textNumericDefaultProps = {
            name: 'textNumericInput',
            controlType: 'text-numeric'
        };

        describe('when value is integer', () => {
            it('returns Number for correct value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '123' } });

                expect(onChange).toBeCalledWith(123);
            });

            it('returns Number for correct negative value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '-123' } });

                expect(onChange).toBeCalledWith(-123);
            });

            it('returns String for incorrect value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '-' } });

                expect(onChange).toBeCalledWith('-');
            });

            it("doesn't call onChange when value has incorrect symbols", () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: -123 } });
                wrapper.find('input').simulate('change', { target: { value: '-123f' } });

                expect(onChange).toBeCalledWith(-123);
                expect(onChange).toBeCalledTimes(1);
            });
        });

        describe('when value is float', () => {
            it('returns Number for correct value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '12.3' } });

                expect(onChange).toBeCalledWith(12.3);
            });

            it('returns Number for correct value with comma', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '12,3' } });

                expect(onChange).toBeCalledWith(12.3);
            });

            it('returns Number for correct negative value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '-12.3' } });

                expect(onChange).toBeCalledWith(-12.3);
            });

            it('returns Number for correct negative value with comma', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '-12,3' } });

                expect(onChange).toBeCalledWith(-12.3);
            });

            it('returns Number for partial value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '1.' } });

                expect(onChange).toBeCalledWith(1);
            });

            it('doesn\'t call handler for incorrect float value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '.0' } });

                expect(onChange).toBeCalledTimes(0);
            });

            it('returns String for incorrect value', () => {
                onChange.mockReset();

                const wrapper = mount(
                    <ControlTextInput
                        {...textNumericDefaultProps}
                        onChange={onChange}
                        isFloat
                    />
                );

                wrapper.find('input').simulate('change', { target: { value: '+' } });

                expect(onChange).toBeCalledWith('+');
            });
        });
    });
});
