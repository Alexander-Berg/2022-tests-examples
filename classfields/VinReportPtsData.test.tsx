import React from 'react';
import { shallow } from 'enzyme';

import type { PtsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import VinReportPtsData from './VinReportPtsData';

it('VinReportPtsData должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const header = {
        title: 'Есть ПТС данные',
        timestamp_update: '1571028005586',
        is_updating: true,
    };

    const wrapper = shallow(
        <VinReportPtsData ptsData={{ header } as PtsBlock}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});

it('VinReportPtsData не должен отрендериться, если status NOT_VISIBLE', () => {
    const wrapper = shallow(
        <VinReportPtsData ptsData={{ status: 'NOT_VISIBLE' } as PtsBlock}/>,
    );

    expect(wrapper).toBeEmptyRender();
});
