import React from 'react';
import { shallow } from 'enzyme';

import type { InsurancePaymentBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

import VinReportInsurancePayments from './VinReportInsurancePayments';

it('VinReportInsurancePayments рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportInsurancePayments insurancePayments={{
            header: {
                title: 'Было дело',
                timestamp_update: '1571028005586',
                is_updating: true,
            },
            payments: [],
            record_count: 0,
            comments_count: 0,
            status: Status.UNKNOWN,
        } as InsurancePaymentBlock}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
