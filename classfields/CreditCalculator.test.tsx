import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';

import CreditCalculator from './CreditCalculator';

it('при передаче исходных данных, находит ближайшие к ним значения слайдеров', () => {
    const tree = shallow(
        <CreditCalculator
            onChange={ _.noop }
            initialData={{
                amount: 140000,
                term: 24,
            }}
            conditions={{
                term: [ 12, 18, 24, 36, 48, 60 ],
                rate: 0.068,
                carPrice: 600000,
                amount: {
                    min: 100000,
                    max: 540000,
                    step: 25000,
                },
            }}
        />,
        { context: contextMock },
    ).dive();

    expect(tree.state()).toMatchObject({ amount: 140000, term: 24 });
});

it('при отсутствии исходных данных, выбирает дефолтные (максимальные) значения', () => {
    const tree = shallow(
        <CreditCalculator
            onChange={ _.noop }
            initialData={{}}
            conditions={{
                term: [ 12, 18, 24, 36, 48, 60 ],
                rate: 0.068,
                carPrice: 600000,
                amount: {
                    min: 100000,
                    max: 540000,
                    step: 25000,
                },
            }}
        />,
        { context: contextMock },
    ).dive();

    expect(tree.state()).toMatchObject({ amount: 540000, term: 60 });
});
