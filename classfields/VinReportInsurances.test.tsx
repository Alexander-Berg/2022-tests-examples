import React from 'react';
import { shallow } from 'enzyme';

import type { InsurancesBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

import VinReportInsurances from './VinReportInsurances';

it('VinReportCustoms рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportInsurances insurancesData={{
            header: {
                title: 'Было дело',
                timestamp_update: '1571028005586',
                is_updating: true,
            },
            insurances: [],
            record_count: 0,
            comments_count: 0,
            status: Status.UNKNOWN,
        } as InsurancesBlock}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
