import React from 'react';
import { shallow } from 'enzyme';

import VinReportTax from './VinReportTax';

it('не должен отрисоваться, если нет данных', () => {
    const wrapper = shallow(
        <VinReportTax/>,
    );

    expect(wrapper.type()).toBeNull();
});
