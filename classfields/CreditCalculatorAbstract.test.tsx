import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import type { ShallowWrapper } from 'enzyme';

import CreditCalculatorAbstract from './CreditCalculatorAbstract';
import type { Props, State } from './CreditCalculatorAbstract';

it('правильно изменяет параметры при смене машины на более дешёвую', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                carPrice: 4299000,
                amount: {
                    min: 100000,
                    max: 3000000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            carPrice: 530000,
            amount: {
                min: 100000,
                max: 530000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 530000,
        carPrice: 530000,
        fee: 0,
        monthlyPayment: 10800,
        term: 60,
    });
});

it('правильно изменяет параметры при смене машины на более дорогую', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                carPrice: 530000,
                amount: {
                    min: 100000,
                    max: 530000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            carPrice: 4299000,
            amount: {
                min: 100000,
                max: 3000000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 3000000,
        carPrice: 4299000,
        fee: 1299000,
        monthlyPayment: 61100,
        term: 60,
    });
});

it('правильно изменяет параметры при появлении цены машины', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                amount: {
                    min: 100000,
                    max: 530000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            carPrice: 4299000,
            amount: {
                min: 100000,
                max: 3000000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 3000000,
        carPrice: 4299000,
        fee: 1299000,
        monthlyPayment: 61100,
        term: 60,
    });
});

it('правильно изменяет параметры при исчезновении цены машин', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                carPrice: 4299000,
                amount: {
                    min: 100000,
                    max: 5300000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            amount: {
                min: 100000,
                max: 5300000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 5300000,
        carPrice: undefined,
        fee: 0,
        monthlyPayment: 107950,
        term: 60,
    });
});

it('верно отдает минимальную сумму для машины стоимостью меньше минимальной суммы кредита', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                carPrice: 69000,
                amount: {
                    min: 100000,
                    max: 5300000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            carPrice: 68000,
            amount: {
                min: 100000,
                max: 5300000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 100000,
        fee: 0,
        monthlyPayment: 2050,
        term: 60,
    });
});

it('верно отдает сумму кредита при отсутствии машины, но с initialData', () => {
    const tree: ShallowWrapper<Props, State, CreditCalculatorAbstract<Props>> = shallow(
        <CreditCalculatorAbstract
            onChange={ _.noop }
            initialData={{
                amount: 1600000,
                term: 36,
            }}
            conditions={{
                term: [ 12, 24, 36, 48, 60 ],
                rate: 7.9,
                amount: {
                    min: 100000,
                    max: 5300000,
                    step: 25000,
                },
            }}
        />,
    );

    tree.setProps({
        conditions: {
            term: [ 12, 24, 36, 48, 60 ],
            rate: 7.9,
            amount: {
                min: 100000,
                max: 5300000,
                step: 25000,
            },
        },
    });

    expect(tree.state()).toMatchObject({
        amount: 1600000,
        fee: 0,
        monthlyPayment: 50400,
        term: 36,
    });
});
