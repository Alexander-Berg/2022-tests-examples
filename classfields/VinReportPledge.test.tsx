import React from 'react';
import { shallow } from 'enzyme';

import VinReportPledge from './VinReportPledge';

const HEADER = {
    title: 'Сведения о нахождении в залоге',
    timestamp_update: '1608727054369',
    is_updating: false,
};

it('не должен отрисовать блок залогов, если нет блока', () => {
    const wrapper = shallow(<VinReportPledge/>);
    expect(wrapper.type()).toBeNull();
});

it('VinReportPledge должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const header = { ...HEADER, is_updating: true };
    const wrapper = shallow(<VinReportPledge pledge={{ header }}/>);
    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
