import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import type { ShallowWrapper } from 'enzyme';

import type { CreditConfiguration } from 'www-cabinet/react/dataDomain/applicationCredit/types';

import CreditsConfigurator from './CreditsConfigurator';

const onChangeMock = jest.fn((configuration: CreditConfiguration) => new Promise(() => configuration));
const defaultProps = {
    configuration: {
        id: '16453:CARS:NEW',
        credit_term_values: [ 9, 10, 11, 12, 13, 14 ],
        credit_default_term: 14,
        credit_amount_slider_step: 25000,
        credit_min_amount: 50000,
        credit_max_amount: 5000000,
        credit_min_rate: 0.08,
        credit_step: 50,
        credit_offer_initial_payment_rate: 0.15,
        dealer_id: 16453,
        category: 'CARS',
        section: 'NEW',
    },
    onChange: onChangeMock,
};

describe('правильно замапит пропсы в стейт', () => {
    it('если есть все настройки', () => {
        const tree = shallowRenderComponent();
        expect(tree.state()).toEqual({
            credit_offer_initial_payment_rate: 15,
            credit_min_rate: 8,
            credit_period: { from: 9, to: 14 },
            isPending: false,
            isDownpaymentFromPreset: true,
            hasDownPaymentError: false,
            hasInterestError: false,
        });
    });

    it('если не указаны варианты периода', () => {
        const props = _.cloneDeep(defaultProps);
        props.configuration.credit_term_values = [];
        const tree = shallowRenderComponent(props);

        expect(tree.state()).toEqual({
            credit_offer_initial_payment_rate: 15,
            credit_min_rate: 8,
            credit_period: { from: 1, to: 15 },
            isPending: false,
            isDownpaymentFromPreset: true,
            hasDownPaymentError: false,
            hasInterestError: false,
        });
    });
});

it('при сохранения вызовет колбэк с правильными пропсами', () => {
    const tree = shallowRenderComponent();
    tree.findWhere(node => node.prop('name') === 'interestRate').simulate('change', '25');
    tree.find('.CreditsConfigurator__saveBlock Button').simulate('click');

    expect(onChangeMock).toHaveBeenCalledWith({
        ...defaultProps.configuration,
        credit_min_rate: 0.25,
    });
});

it('не вызовет колбэк, если состояние isPending: true', () => {
    const tree = shallowRenderComponent();
    tree.findWhere(node => node.prop('name') === 'interestRate').simulate('change', '25');
    tree.setState({ isPending: true });
    tree.find('.CreditsConfigurator__saveBlock Button').simulate('click');

    expect(onChangeMock).not.toHaveBeenCalled();
});

it('проставит активный Tag, если значение первичного взноса из пресетов', () => {
    const tree = shallowRenderComponent();

    expect(tree.find('Tags').prop('value')).toEqual(15);
});

describe('задизейблит кнопку сохранения', () => {
    const checkButtonDisabledProp = (tree: ShallowWrapper<any>) => {
        return tree.find('.CreditsConfigurator__saveBlock Button').prop('disabled');
    };

    const TEST_CASES = {
        downpaymentCustom: [ '91', '' ],
        interestRate: [ '51', '0.001', '' ],
    };

    Object.keys(TEST_CASES).forEach(inputName => {
        TEST_CASES[inputName as keyof typeof TEST_CASES].forEach((inputValue: string) => {
            it(`если ${ inputName } имеет значение ${ inputValue }`, () => {
                const tree = shallowRenderComponent();
                tree.findWhere(node => node.prop('name') === inputName).simulate('change', inputValue);

                expect(checkButtonDisabledProp(tree)).toEqual(true);
            });
        });
    });
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <CreditsConfigurator { ...props }/>,
    );
}
