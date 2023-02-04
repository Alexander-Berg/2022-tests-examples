import React from 'react';
import { mount } from 'enzyme';

import { EGRNPaidReportPriceLineChartBlock } from '../';

const emptyPriceDynamics = {
    building: [],
    district: [],
    fifteenMin: [],
};

test('не рендерит ничего, когда записей меньше двух', () => {
    const wrapper = mount(<EGRNPaidReportPriceLineChartBlock priceDynamics={emptyPriceDynamics} />);

    expect(wrapper).toMatchSnapshot();
});
