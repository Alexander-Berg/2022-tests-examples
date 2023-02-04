import 'jest-enzyme';
import React, { useCallback } from 'react';
import type { ReactWrapper } from 'enzyme';
import { mount } from 'enzyme';

import { AmountInput } from './AmountInput';

const onAmountChange = jest.fn();

const minAmount = 30;
const maxAmount = 100;

const Input = ({ onChange, ...props }: {onChange: (v: string) => void}) => {
    const handleChange = useCallback(
        (e) => {
            onChange(e.target.value);
        },
        [ onChange ])
    ;
    return (<input { ...props } onChange={ handleChange }/>);
};
jest.mock('auto-core/react/components/common/MaskedTextInput/MaskedTextInput',
    () => Input);

const setFieldValue = (field: ReactWrapper<any, any>, value: string) => {
    // field.simulate('change', { value: value });
    field.simulate('change', { target: { value } });

    field.simulate('blur');
};

const containerRef = React.createRef<HTMLDivElement>();

describe('AmountInput', () => {
    it('должен при удалении, вызывать onChange c минимумом', async() => {
        const amountInput = mount(
            <AmountInput
                titleClassName="CreditCalculator2__fieldTitle"
                valueClassName="CreditCalculator2__fieldValue"
                max={ maxAmount }
                min={ minAmount }
                value={ 73 }
                onChange={ onAmountChange }
                containerRef={ containerRef }
            />,
        );

        setFieldValue(
            amountInput.find('input'),
            '',
        );

        expect(onAmountChange).toHaveBeenCalledWith(minAmount);
    });

    it('должен при вводе суммы меньше минимума, вызывать onChange c минимумом', async() => {
        const amountInput = mount(
            <AmountInput
                titleClassName="CreditCalculator2__fieldTitle"
                valueClassName="CreditCalculator2__fieldValue"
                max={ maxAmount }
                min={ minAmount }
                value={ 73 }
                onChange={ onAmountChange }
                containerRef={ containerRef }
            />,
        );

        setFieldValue(
            amountInput.find('input'),
            (minAmount - minAmount / 2).toString(),
        );

        expect(onAmountChange).toHaveBeenCalledWith(minAmount);
    });

    it('должен при вводе суммы больше максимума, вызывать onChange c максимумом', async() => {
        const amountInput = mount(
            <AmountInput
                titleClassName="CreditCalculator2__fieldTitle"
                valueClassName="CreditCalculator2__fieldValue"
                max={ maxAmount }
                min={ minAmount }
                value={ 73 }
                onChange={ onAmountChange }
                containerRef={ containerRef }
            />,
        );

        setFieldValue(
            amountInput.find('input'),
            (maxAmount + maxAmount / 2).toString(),
        );

        expect(onAmountChange).toHaveBeenCalledWith(maxAmount);
    });
});
