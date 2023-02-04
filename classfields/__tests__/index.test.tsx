import React from 'react';
import * as enzyme from 'enzyme';

import { MaskedInput, IMaskedInputProps } from '../index';

const Component = (props: Omit<IMaskedInputProps, 'variant' | 'value' | 'onChange' | 'size' | 'label' | 'type'>) => {
    const [value, setState] = React.useState<string | undefined>('');

    return (
        <MaskedInput type="text" variant="bordered" size="l" value={value} onChange={(v) => setState(v)} {...props} />
    );
};

const getInputValue = (wrapper: enzyme.ReactWrapper) => wrapper.find('input').prop('value');
const getStateValue = (wrapper: enzyme.ReactWrapper) => wrapper.find(MaskedInput).prop('value');

describe('MaskedInput', () => {
    it('Полный ввод. Один разделитель', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[4]} />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: '0000012345' } });

        expect(getInputValue(wrapper)).toBe('0000 - 012345');
        expect(getStateValue(wrapper)).toBe('0000012345');
    });

    it('Полный ввод. Несколько разделителей', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[4, 8]} />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: '0000012345' } });

        expect(getInputValue(wrapper)).toBe('0000 - 0123 - 45');
        expect(getStateValue(wrapper)).toBe('0000012345');
    });

    it('Только числа', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[2]} />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: 'dick1boobs2 34' } });

        expect(getInputValue(wrapper)).toBe('12 - 34');
        expect(getStateValue(wrapper)).toBe('1234');
    });

    it('Только буквы', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[2]} allowed="[a-z]+" />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: 'a1b2c3d4' } });

        expect(getInputValue(wrapper)).toBe('ab - cd');
        expect(getStateValue(wrapper)).toBe('abcd');
    });

    it('Ввод до разделителя', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[2]} />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: '11' } });

        expect(getInputValue(wrapper)).toBe('11 - ');
        expect(getStateValue(wrapper)).toBe('11');
    });

    it('Ввод после разделителя', () => {
        const wrapper = enzyme.mount(<Component separator=" - " separatorPositions={[2]} />);
        const input = wrapper.find('input');

        input.simulate('change', { target: { value: '111' } });

        expect(getInputValue(wrapper)).toBe('11 - 1');
        expect(getStateValue(wrapper)).toBe('111');
    });
});
