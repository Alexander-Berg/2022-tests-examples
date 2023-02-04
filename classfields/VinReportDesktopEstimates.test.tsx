import React from 'react';
import { shallow } from 'enzyme';

import type { EstimateBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import DateMock from 'autoru-frontend/mocks/components/DateMock';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import DATA, { ESTIMATE_RECORD as FIRST_RECORD } from 'auto-core/react/dataDomain/vinReport/mocks/estimates';

import VinReportDesktopEstimates from './VinReportDesktopEstimates';

// Просто чтобы отличать две записи
const SECOND_RECORD = {
    ...FIRST_RECORD,
    images: FIRST_RECORD.images.map((item) => ({
        ...item,
        sizes: {
            ...item.sizes,
            '320x240': 'CHECKING_IMAGE_URL_MOCK',
            '1200x900': 'CHECKING_IMAGE_URL_MOCK',
        },
    })),
};

const DOUBLE_DATA = {
    ...DATA,
    estimate_records: [ FIRST_RECORD, SECOND_RECORD ],
};

const Context = createContextProvider(contextMock);

async function renderComponent(data: EstimateBlock) {
    const wrapper = await shallow(
        <DateMock date="2020-04-20">
            <Context>
                <VinReportDesktopEstimates data={ data }/>
            </Context>
        </DateMock>,
    ).dive().dive();

    return wrapper;
}

it('десктоп: при клике на превью откроет fullscreen-галерею', async() => {
    const LAST_INDEX = DOUBLE_DATA.estimate_records.length - 1;
    const wrapper = await renderComponent(DOUBLE_DATA);

    wrapper.find('VinReportEstimates').dive()
        .find('VinReportEstimatesRecord').last().dive()
        .find('.VinReportDesktopEstimates__images')
        .simulate('click', { preventDefault: () => {}, currentTarget: { dataset: { index: LAST_INDEX } } });

    expect(wrapper.find('ImageGalleryFullscreenVertical')).toExist();
});

it('десктоп: прокинет в fullscreen-галерею правильные фото', async() => {
    const LAST_INDEX = DOUBLE_DATA.estimate_records.length - 1;
    const wrapper = await renderComponent(DOUBLE_DATA);

    const records = wrapper.find('VinReportEstimates').dive().find('VinReportEstimatesRecord');

    expect((records.last().props() as { indexRecord: number }).indexRecord).toEqual(LAST_INDEX);

    wrapper.find('VinReportEstimates').dive()
        .find('VinReportEstimatesRecord').last().dive()
        .find('.VinReportDesktopEstimates__images')
        .simulate('click', { preventDefault: () => {}, currentTarget: { dataset: { index: LAST_INDEX } } });

    expect(wrapper.find('ImageGalleryFullscreenVertical').props().items[0].widescreen).toEqual('CHECKING_IMAGE_URL_MOCK');
});
