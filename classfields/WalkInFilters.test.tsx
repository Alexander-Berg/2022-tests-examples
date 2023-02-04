import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import MockDate from 'mockdate';

import DateRange from 'auto-core/react/components/desktop/DateRange/DateRange';

import WalkInFilters from './WalkInFilters';

beforeEach(() => {
    MockDate.set('2019-03-02');
});

afterEach(() => {
    MockDate.reset();
});

it('должен формировать корректные пропсы даты для дата-пикера', () => {
    const tree = shallow(
        <WalkInFilters
            dateLimits={{ from: '2019-02-02', to: '2019-03-01' }}
            onChange={ _.noop }
            isVisibleDescriptionButton={ false }
        />,
    );

    const datePicker = tree.find(DateRange);

    const props = _.pick(datePicker.props(), [ 'initialDate', 'maxDate', 'minDate' ]);

    expect(props).toMatchSnapshot();
});
