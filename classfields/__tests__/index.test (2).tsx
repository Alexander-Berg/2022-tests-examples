import React from 'react';
import * as enzyme from 'enzyme';

import { NumberInput, INumberInputProps } from '../index';

const LogicComponent = (props: Omit<INumberInputProps, 'variant' | 'value' | 'onChange' | 'size' | 'label'>) => {
    const [value, setState] = React.useState<number | undefined>();

    return (
        <NumberInput variant="bordered" size="l" label="Адрес" value={value} onChange={(v) => setState(v)} {...props} />
    );
};

const getInputValue = (wrapper: enzyme.ReactWrapper) => wrapper.find('input').prop('value');
const getStateValue = (wrapper: enzyme.ReactWrapper) => wrapper.find(NumberInput).prop('value');

describe('NumberInput', () => {
    describe('Работа с числами с плавающей запятой', () => {
        it('Вводится целое значение', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100' } });

            expect(getInputValue(wrapper)).toBe('100');
            expect(getStateValue(wrapper)).toBe(100);
        });

        it('Вводится дробное значение', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100,10' } });

            expect(getInputValue(wrapper)).toBe('100,10');
            expect(getStateValue(wrapper)).toBe(100.1);
        });

        it('Опредленное количество символов после запятой (floatFractionalPartDigitCount)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat floatFractionalPartDigitCount={3} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100,123' } });

            expect(getInputValue(wrapper)).toBe('100,123');
            expect(getStateValue(wrapper)).toBe(100.123);
        });

        it('Использование точки в качестве разделителя', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat floatSeparator="." />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100.12' } });

            expect(getInputValue(wrapper)).toBe('100.12');
            expect(getStateValue(wrapper)).toBe(100.12);
        });

        it('Корректировка ввода, точку заменяем на запятую', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100.12' } });

            expect(getInputValue(wrapper)).toBe('100,12');
            expect(getStateValue(wrapper)).toBe(100.12);
        });

        it('Максимальное значение (max)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat max={1000} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '5000,12' } });

            expect(getInputValue(wrapper)).toBe('1000');
            expect(getStateValue(wrapper)).toBe(1000);
        });

        it('Минимальное значение (min)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat min={10} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '5,12' } });

            expect(getInputValue(wrapper)).toBe('10');
            expect(getStateValue(wrapper)).toBe(10);
        });

        it('Автодополнение до необходимого количества 0 цифрами дробной части (isPadFloat)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat isPadFloat />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '50,8' } });
            input.simulate('blur');

            expect(getInputValue(wrapper)).toBe('50,80');
            expect(getStateValue(wrapper)).toBe(50.8);
        });

        it('Убирает плавающуюю запятую, если нет дробной части', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat isPadFloat />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '50,' } });
            input.simulate('blur');

            expect(getInputValue(wrapper)).toBe('50');
            expect(getStateValue(wrapper)).toBe(50);
        });

        it('Разделяет целую часть на группы (digitGroupCount)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat digitGroupCount={3} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567,52' } });

            expect(getInputValue(wrapper)).toBe('123 456 7,52');
            expect(getStateValue(wrapper)).toBe(1234567.52);
        });

        it('Разделяет целую часть на группы в обратном порядке (digitGroupReversed)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat digitGroupCount={3} digitGroupReversed />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567,52' } });

            expect(getInputValue(wrapper)).toBe('1 234 567,52');
            expect(getStateValue(wrapper)).toBe(1234567.52);
        });

        it('Используется другой разделитель для групп (digitGroupSeparator)', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat digitGroupCount={3} digitGroupSeparator="-" />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567,52' } });

            expect(getInputValue(wrapper)).toBe('123-456-7,52');
            expect(getStateValue(wrapper)).toBe(1234567.52);
        });

        it('Нельзя ввести пустую целую часть и дробную часть', () => {
            const wrapper = enzyme.mount(<LogicComponent isFloat digitGroupCount={3} digitGroupSeparator="-" />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: ',4' } });

            expect(getInputValue(wrapper)).toBe('');
            expect(getStateValue(wrapper)).toBe(undefined);
        });
    });
    describe('Работа с целыми числами', () => {
        it('Вводится целое значение', () => {
            const wrapper = enzyme.mount(<LogicComponent />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100' } });

            expect(getInputValue(wrapper)).toBe('100');
            expect(getStateValue(wrapper)).toBe(100);
        });

        it('Не вводится разделитель дробной части', () => {
            const wrapper = enzyme.mount(<LogicComponent />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '100,' } });

            expect(getInputValue(wrapper)).toBe('100');
            expect(getStateValue(wrapper)).toBe(100);
        });

        it('Максимальное значение (max)', () => {
            const wrapper = enzyme.mount(<LogicComponent max={1000} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '5000' } });

            expect(getInputValue(wrapper)).toBe('1000');
            expect(getStateValue(wrapper)).toBe(1000);
        });

        it('Минимальное значение (min)', () => {
            const wrapper = enzyme.mount(<LogicComponent min={10} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '5' } });

            expect(getInputValue(wrapper)).toBe('10');
            expect(getStateValue(wrapper)).toBe(10);
        });

        it('Разделяет число на группы (digitGroupCount)', () => {
            const wrapper = enzyme.mount(<LogicComponent digitGroupCount={3} />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567' } });

            expect(getInputValue(wrapper)).toBe('123 456 7');
            expect(getStateValue(wrapper)).toBe(1234567);
        });

        it('Разделяет число на группы в обратном порядке (digitGroupReversed)', () => {
            const wrapper = enzyme.mount(<LogicComponent digitGroupCount={3} digitGroupReversed />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567' } });

            expect(getInputValue(wrapper)).toBe('1 234 567');
            expect(getStateValue(wrapper)).toBe(1234567);
        });

        it('Используется другой разделитель для групп (digitGroupSeparator)', () => {
            const wrapper = enzyme.mount(<LogicComponent digitGroupCount={3} digitGroupSeparator="-" />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '1234567' } });

            expect(getInputValue(wrapper)).toBe('123-456-7');
            expect(getStateValue(wrapper)).toBe(1234567);
        });

        it('Форматирует по маске число (mask)', () => {
            const wrapper = enzyme.mount(<LogicComponent mask="99=99()*9" />);
            const input = wrapper.find('input');

            input.simulate('change', { target: { value: '12345' } });

            expect(getInputValue(wrapper)).toBe('12=34()*5');
            expect(getStateValue(wrapper)).toBe(12345);
        });
    });
});
