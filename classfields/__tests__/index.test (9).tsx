import React from 'react';
import { mount } from 'enzyme';

import { EGRNPaidReportLocationFeaturesBlock } from '../';

const emptyProps = {
    pondList: [],
    parkList: [],
    metroList: [],
    heatmapList: [],
};

test('рендерит null, когда не передано ни одной фичи', () => {
    const wrapper = mount(<EGRNPaidReportLocationFeaturesBlock {...emptyProps} />);

    expect(wrapper).toMatchSnapshot();
});
