import React from 'react';
import { shallow } from 'enzyme';

import type { CustomsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import VinReportCustoms from './VinReportCustoms';

it('VinReportCustoms рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportCustoms customs={{
            header: {
                title: 'Было дело',
                timestamp_update: '1571028005586',
                is_updating: true,
            },
            customs_records: [],
            record_count: 0,
            status: Status.IN_PROGRESS,
        } as CustomsBlock}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
