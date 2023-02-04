import React from 'react';
import { shallow } from 'enzyme';

import type { FinesBlock } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import sleep from 'auto-core/lib/sleep';

import FINES_BLOCK from 'auto-core/react/dataDomain/vinReport/mocks/fines';

import VinReportFines from './VinReportFines';

it('VinReportCustoms рендерит VinReportLoading, если данные еще не пришли', async() => {
    const wrapper = shallow(
        <VinReportFines fines={{
            header: {
                title: 'Было дело',
                timestamp_update: '1571028005586',
                is_updating: true,
            },
            records: [],
            is_sts_known: false,
            record_count: 0,
            status: Status.IN_PROGRESS,
        } as FinesBlock}/>,
    );

    expect(wrapper.find('VinReportLoading').exists()).toBe(true);
});

it('VinReportFines при клике на количество штрафов показываем экран с неоплаченными штрафами', async() => {
    const wrapper = shallow(
        <VinReportFines fines={ FINES_BLOCK }/>,
    );

    const vinReportFinesHeaderBlock = wrapper.find('VinReportFinesHeader');
    const finesCountBlock = vinReportFinesHeaderBlock.dive().find('.VinReportFinesHeader__amount');

    finesCountBlock.simulate('click');

    //сначала мы показываем лоадер и только через секунду экран со штрафами
    await sleep(1000);

    const vinRepostFinesRecords = wrapper.find('VinReportFineRecord');

    const button = wrapper.find('.VinReportFines__button');

    // должен вернуть только неоплаченные штрафы
    expect(vinRepostFinesRecords.everyWhere((item) => {
        const { status } = item.prop('record');
        return status === 'NEED_PAYMENT';
    })).toEqual(true);

    expect(button.dive().text()).toEqual('Показать все штрафы');
});

it('VinReportFines при клике на кнопку Показать все штрафы показываем экран со всеми штрафами', async() => {
    const wrapper = shallow(
        <VinReportFines fines={ FINES_BLOCK }/>,
    );

    const button = wrapper.find('.VinReportFines__button');
    button.simulate('click');

    const vinRepostFinesRecords = wrapper.find('VinReportFineRecord');

    expect(vinRepostFinesRecords).toHaveLength(FINES_BLOCK.records.length);
});
