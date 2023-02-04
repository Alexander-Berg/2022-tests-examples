import React from 'react';
import { shallow } from 'enzyme';

import VinReportSellTime from './VinReportSellTime';

it('не должен отрисоваться, если нет данных', () => {
    const wrapper = shallow(
        <VinReportSellTime/>,
    );

    expect(wrapper.type()).toBeNull();
});
