import React from 'react';
import { shallow } from 'enzyme';

import type { EstimateBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import VinReportEstimates from './VinReportEstimates';

const DATA = {
    header: {
        title: 'Экспертная оценка',
        timestamp_update: '1619695841879',
        is_updating: true,
    },
    estimate_records: [],
    record_count: 0,
    status: Status.OK,
} as EstimateBlock;

const Context = createContextProvider(contextMock);

it('VinReportEstimates должен рендерить Loading', () => {
    const wrapper = shallow(
        <Context>
            <VinReportEstimates data={ DATA } renderRecordGallery={ jest.fn() }/>
        </Context>,
    ).dive();

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});
