import React from 'react';
import { shallow } from 'enzyme';

import type { RecallsBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types/auto/api/vin/vin_resolution_enums';

import VinReportRecalls from './VinReportRecalls';

it('VinReportRecalls должен отрендерить VinReportLoading, если is_updating и нет данных', () => {
    const recallsBlock = {
        header: {
            title: 'Были отзывные кампании',
            timestamp_update: '1571028005586',
            is_updating: true,
        },
        recall_records: [],
        record_count: 0,
        comments_count: 0,
        status: Status.UNKNOWN,
    } as RecallsBlock;

    const wrapper = shallow(
        <VinReportRecalls data={ recallsBlock }/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
